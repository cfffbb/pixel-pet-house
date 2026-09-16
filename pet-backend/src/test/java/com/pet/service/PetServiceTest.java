package com.pet.service;

import com.pet.common.PetConstants;
import com.pet.common.exception.BusinessException;
import com.pet.entity.Pet;
import com.pet.entity.PetGravestone;
import com.pet.entity.PetType;
import com.pet.mapper.PetGravestoneMapper;
import com.pet.mapper.PetInventoryMapper;
import com.pet.mapper.PetMapper;
import com.pet.mapper.PetSubtypeMapper;
import com.pet.mapper.PetTypeMapper;
import com.pet.mapper.PlayModeMapper;
import com.pet.mapper.ShopItemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PetService 单测:状态推进 / 抽蛋 / 成长阶段 / 上架校验
 * <p>
 * 用 Mockito 模拟 7 个 Mapper,不依赖真实数据库。
 * 时间相关用 LocalDateTime.now().minusHours(N) 控制相对时间差。
 */
@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @Mock private PetMapper petMapper;
    @Mock private PetTypeMapper petTypeMapper;
    @Mock private PetSubtypeMapper petSubtypeMapper;
    @Mock private PetGravestoneMapper gravestoneMapper;
    @Mock private ShopItemMapper shopItemMapper;
    @Mock private PetInventoryMapper inventoryMapper;
    @Mock private PlayModeMapper playModeMapper;

    @InjectMocks
    private PetService petService;

    // ==================== refreshState(状态推进) ====================

    @Test
    void refreshState_nullPet_doesNothing() {
        petService.refreshState(null);
        verifyNoInteractions(petMapper, petTypeMapper);
    }

    @Test
    void refreshState_deadPet_doesNothing() {
        Pet pet = new Pet();
        pet.setStatus(PetConstants.PET_STARVED);

        petService.refreshState(pet);

        verifyNoInteractions(petTypeMapper, petMapper);
    }

    @Test
    void refreshState_hungerDropsNormally() {
        PetType type = new PetType();
        type.setBaseDays(3); // starveHours = 3×24×1.0 = 72h

        Pet pet = alivePet();
        pet.setHunger(100);
        pet.setHungerCalcAt(LocalDateTime.now().minusHours(24)); // 24h 前

        when(petTypeMapper.selectById(any())).thenReturn(type);

        petService.refreshState(pet);

        // rate=100/72≈1.389/h; 24h→drop=floor(1.389×1440/60)=33; hunger=100-33=67
        assertThat(pet.getHunger()).isEqualTo(67);
        assertThat(pet.getStatus()).isEqualTo(PetConstants.PET_ALIVE);
        verify(petMapper).updateById(pet);
    }

    @Test
    void refreshState_hungerToZero_entersDanger() {
        PetType type = new PetType();
        type.setBaseDays(3);

        Pet pet = alivePet();
        pet.setHunger(5);
        pet.setHungerCalcAt(LocalDateTime.now().minusHours(48)); // 48h 前

        when(petTypeMapper.selectById(any())).thenReturn(type);

        petService.refreshState(pet);

        // drop=floor(1.389×2880/60)=66; hunger=max(0,5-66)=0 → 进病危
        assertThat(pet.getHunger()).isEqualTo(0);
        assertThat(pet.getStatus()).isEqualTo(PetConstants.PET_DANGER);
        assertThat(pet.getDangerSince()).isNotNull();
        verify(petMapper).updateById(pet);
    }

    @Test
    void refreshState_dangerOver24h_diesAndCreatesGravestone() {
        PetType type = new PetType();
        type.setBaseDays(3);
        type.setTypeName("测试猫");

        Pet pet = alivePet();
        pet.setHunger(0);
        pet.setHungerCalcAt(LocalDateTime.now().minusHours(48));
        pet.setDangerSince(LocalDateTime.now().minusHours(25)); // 病危已 25h
        pet.setHatchedAt(LocalDateTime.now().minusDays(5));
        pet.setStatus(PetConstants.PET_DANGER);

        when(petTypeMapper.selectById(any())).thenReturn(type);

        petService.refreshState(pet);

        assertThat(pet.getStatus()).isEqualTo(PetConstants.PET_STARVED);
        verify(gravestoneMapper).insert(any(PetGravestone.class));
    }

    @Test
    void refreshState_typeNotFound_doesNothing() {
        Pet pet = alivePet();
        pet.setHunger(100);
        pet.setHungerCalcAt(LocalDateTime.now().minusHours(24));

        when(petTypeMapper.selectById(any())).thenReturn(null);

        petService.refreshState(pet);

        // type=null 直接 return,hunger 不变,不写库
        assertThat(pet.getHunger()).isEqualTo(100);
        verify(petMapper, never()).updateById(any(Pet.class));
    }

    // ==================== draw(抽蛋) ====================

    @Test
    void draw_emptyName_throws() {
        assertThatThrownBy(() -> petService.draw(1L, "USER", null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("必须给宠物起名");
    }

    @Test
    void draw_nameTooLong_throws() {
        assertThatThrownBy(() -> petService.draw(1L, "USER", "一二三四五六七八九十一二三", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("最多 12 个字");
    }

    @Test
    void draw_alreadyHasPet_throws() {
        when(petMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> petService.draw(1L, "USER", "小猫", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已有一只宠物");
    }

    @Test
    void draw_hasUnansweredGravestone_throws() {
        when(petMapper.selectCount(any())).thenReturn(0L);
        when(gravestoneMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> petService.draw(1L, "USER", "小猫", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("反思答题");
    }

    @Test
    void draw_adminWithCode_createsSpecifiedType() {
        PetType type = new PetType();
        type.setId(1L);
        type.setTypeCode("DRAGON");
        type.setTypeName("龙");
        type.setRarity("LEGEND");

        when(petMapper.selectCount(any())).thenReturn(0L);
        when(gravestoneMapper.selectCount(any())).thenReturn(0L);
        when(petTypeMapper.selectOne(any())).thenReturn(type);
        when(petSubtypeMapper.selectList(any())).thenReturn(List.of());
        when(shopItemMapper.selectOne(any())).thenReturn(null);

        var data = petService.draw(1L, "ADMIN", "小龙", "DRAGON");

        assertThat(data.get("typeName")).isEqualTo("龙");
        assertThat(data.get("rarity")).isEqualTo("LEGEND");
        verify(petMapper).insert(any(Pet.class));
    }

    // ==================== 成长阶段 / 年龄 / 成熟 ====================

    @Test
    void stageOf_boundaries() {
        Pet baby = new Pet();
        baby.setHatchedAt(LocalDateTime.now()); // 0 天
        assertThat(petService.stageOf(baby)).isEqualTo("BABY");

        Pet young = new Pet();
        young.setHatchedAt(LocalDateTime.now().minusDays(2)); // 2 天
        assertThat(petService.stageOf(young)).isEqualTo("YOUNG");

        Pet adult = new Pet();
        adult.setHatchedAt(LocalDateTime.now().minusDays(10)); // 10 天
        assertThat(petService.stageOf(adult)).isEqualTo("ADULT");

        Pet elder = new Pet();
        elder.setHatchedAt(LocalDateTime.now().minusDays(61)); // 61 天
        assertThat(petService.stageOf(elder)).isEqualTo("ELDER");
    }

    @Test
    void ageDays_nullHatchedAt_returnsZero() {
        Pet pet = new Pet();
        assertThat(petService.ageDays(pet)).isEqualTo(0);
    }

    @Test
    void isMature_boundary() {
        Pet young = new Pet();
        young.setHatchedAt(LocalDateTime.now().minusDays(2)); // < 3 天
        assertThat(petService.isMature(young)).isFalse();

        Pet mature = new Pet();
        mature.setHatchedAt(LocalDateTime.now().minusDays(3)); // = 3 天
        assertThat(petService.isMature(mature)).isTrue();
    }

    // ==================== setListed(上架配种市场) ====================

    @Test
    void setListed_female_throws() {
        Pet pet = alivePet();
        pet.setGender("FEMALE");
        when(petMapper.selectOne(any())).thenReturn(pet);

        assertThatThrownBy(() -> petService.setListed(1L, "USER", true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("只有雄性能上架");
    }

    @Test
    void setListed_deadPet_throws() {
        Pet pet = alivePet();
        pet.setGender("MALE");
        pet.setStatus(PetConstants.PET_STARVED);
        when(petMapper.selectOne(any())).thenReturn(pet);

        assertThatThrownBy(() -> petService.setListed(1L, "USER", true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已死亡");
    }

    @Test
    void setListed_notMature_throws() {
        Pet pet = alivePet();
        pet.setGender("MALE");
        pet.setHatchedAt(LocalDateTime.now().minusDays(1)); // 1 天 < 3 天
        when(petMapper.selectOne(any())).thenReturn(pet);

        assertThatThrownBy(() -> petService.setListed(1L, "USER", true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("还未成熟");
    }

    @Test
    void setListed_inCoolDown_throws() {
        Pet pet = alivePet();
        pet.setGender("MALE");
        pet.setHatchedAt(LocalDateTime.now().minusDays(10)); // 已成熟
        pet.setBreedingCoolUntil(LocalDateTime.now().plusDays(2)); // 还在冷却
        when(petMapper.selectOne(any())).thenReturn(pet);

        assertThatThrownBy(() -> petService.setListed(1L, "USER", true))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("冷却中");
    }

    @Test
    void setListed_unlist_succeeds() {
        Pet pet = alivePet();
        pet.setGender("FEMALE");
        pet.setListed(1);
        when(petMapper.selectOne(any())).thenReturn(pet);

        var data = petService.setListed(1L, "USER", false);

        assertThat(data.get("listed")).isEqualTo(0);
        verify(petMapper).updateById(pet);
    }

    // ==================== rename ====================

    @Test
    void rename_alwaysThrows() {
        assertThatThrownBy(() -> petService.rename(1L, "新名字"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不能更改");
    }

    // ==================== 辅助方法 ====================

    /** 构造一只标准活宠,各字段已设好避免 NPE */
    private Pet alivePet() {
        Pet pet = new Pet();
        pet.setId(1L);
        pet.setUserId(10L);
        pet.setPetTypeId(1L);
        pet.setPetName("小测");
        pet.setGender("MALE");
        pet.setPersonality("活泼"); // coeff = 1.0
        pet.setHunger(100);
        pet.setMood(100);
        pet.setCoins(0);
        pet.setExp(0);
        pet.setLevel(1);
        pet.setStatus(PetConstants.PET_ALIVE);
        pet.setHatchedAt(LocalDateTime.now());
        pet.setHungerCalcAt(LocalDateTime.now());
        pet.setListed(0);
        return pet;
    }
}
