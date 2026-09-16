package com.pet.service;

import com.pet.common.PetConstants;
import com.pet.common.exception.BusinessException;
import com.pet.entity.Pet;
import com.pet.entity.PomodoroRecord;
import com.pet.mapper.PetMapper;
import com.pet.mapper.PomodoroRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 番茄钟业务单测:等级计算边界、完成结算(币/经验/等级)、暂停次数限制、归属校验。
 * 用 Mockito 打桩 Mapper,不依赖真实数据库。
 */
@ExtendWith(MockitoExtension.class)
class PomodoroServiceTest {

    @Mock
    private PomodoroRecordMapper recordMapper;

    @Mock
    private PetMapper petMapper;

    @Mock
    private PetService petService;

    @InjectMocks
    private PomodoroService service;

    @Test
    void calcLevel_boundaries() {
        // 等级 = 1 + 累计分钟 / 1200
        assertThat(PomodoroService.calcLevel(0)).isEqualTo(1);
        assertThat(PomodoroService.calcLevel(1199)).isEqualTo(1);
        assertThat(PomodoroService.calcLevel(1200)).isEqualTo(2);
        assertThat(PomodoroService.calcLevel(2400)).isEqualTo(3);
    }

    @Test
    void calcLevel_cappedAtMax() {
        int max = PetConstants.LEVEL_MAX;
        // 远超上限应被截到 LEVEL_MAX
        assertThat(PomodoroService.calcLevel(max * 10_000)).isEqualTo(max);
    }

    @Test
    void complete_grantsCoinsAndExpProportionalToFocusedMinutes() {
        Long userId = 1L;
        Long recordId = 10L;
        Pet pet = new Pet();
        pet.setId(2L);
        pet.setCoins(100);
        pet.setExp(0);
        pet.setLevel(1);

        PomodoroRecord r = new PomodoroRecord();
        r.setId(recordId);
        r.setUserId(userId);
        r.setStartedAt(LocalDateTime.now().minusMinutes(60)); // 60 分钟前开始
        r.setPausedCount(0);
        r.setTotalPausedMinutes(0);
        r.setCompleted(0);
        r.setPausedAt(null);

        when(recordMapper.selectById(recordId)).thenReturn(r);
        when(petService.getMyPet(userId)).thenReturn(pet);

        Map<String, Object> data = service.complete(userId, recordId);

        Integer coins = (Integer) data.get("coins");
        // 60 分钟前开始 → 专注约 60 分钟(留 ±2 容差防慢机);1 币/分钟
        assertThat(coins).isBetween(58, 62);
        // 宠物金币、经验按专注分钟同步增长;等级按新经验重算
        assertThat(pet.getCoins()).isEqualTo(100 + coins);
        assertThat(pet.getExp()).isEqualTo(0 + coins);
        assertThat(pet.getLevel()).isEqualTo(PomodoroService.calcLevel(coins));
        verify(recordMapper).updateById(r);
        verify(petMapper).updateById(pet);
    }

    @Test
    void pause_overMaxPauses_throws() {
        Long userId = 1L;
        Long recordId = 10L;
        PomodoroRecord r = new PomodoroRecord();
        r.setId(recordId);
        r.setUserId(userId);
        r.setPausedCount(PetConstants.POMODORO_MAX_PAUSES); // 已达上限
        r.setCompleted(0);
        r.setPausedAt(null);

        when(recordMapper.selectById(recordId)).thenReturn(r);

        assertThatThrownBy(() -> service.pause(userId, recordId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void complete_notOwner_throws() {
        PomodoroRecord r = new PomodoroRecord();
        r.setId(10L);
        r.setUserId(999L); // 别人的记录
        when(recordMapper.selectById(10L)).thenReturn(r);

        assertThatThrownBy(() -> service.complete(1L, 10L))
                .isInstanceOf(BusinessException.class);
    }
}
