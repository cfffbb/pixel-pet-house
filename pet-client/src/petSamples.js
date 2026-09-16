/*
 * 宠物样例图映射 —— 将 species+gender 映射到 public/pets/samples/ 下的图片
 * 文件名格式: 01_小公猴_transparent.png (序号_中文名_transparent.png)
 */

// species → 中文名
const SPECIES_CN = {
  RAT: '鼠', OX: '牛', TIGER: '虎', RABBIT: '兔', DRAGON: '龙',
  SNAKE: '蛇', HORSE: '马', GOAT: '羊', MONKEY: '猴', ROOSTER: '鸡',
  DOG: '狗', PIG: '猪', CAT: '猫',
  // 兼容别名
  SHEEP: '羊', CHICKEN: '鸡', BUNNY: '兔',
};

// gender → 中文前缀
const GENDER_CN = {
  MALE: '公',
  FEMALE: '母',
};

// 文件名前缀映射表(species+gender → 样例图文件序号)
// 根据 public/pets/samples/ 目录文件名整理
const SAMPLE_MAP = {
  'MONKEY_MALE':   '01_小公猴',
  'HORSE_MALE':    '02_小公马',
  'DOG_MALE':      '03_小公狗',
  'GOAT_FEMALE':   '04_小母羊',
  'DOG_FEMALE':    '05_小母狗',
  'OX_FEMALE':     '06_小母牛',
  'PIG_MALE':      '07_小公猪',
  'PIG_FEMALE':    '08_小母猪',
  'TIGER_FEMALE':  '09_小母虎',
  'TIGER_MALE':    '10_小公虎',
  'ROOSTER_FEMALE':'11_小母鸡',
  'ROOSTER_MALE':  '12_小公鸡',
  'MONKEY_MALE_2': '13_小公猴',  // 重复,作为备用
  'MONKEY_FEMALE': '14_小母猴',
  'GOAT_MALE':     '15_小公羊',
  'GOAT_FEMALE_2': '16_小母羊',  // 重复,作为备用
  'HORSE_FEMALE':  '17_小母马',
  'SNAKE_FEMALE':  '18_小母蛇',
  'SNAKE_MALE':    '19_小公蛇',
  'DRAGON_MALE':   '20_小公龙',
  'DRAGON_FEMALE': '21_小母龙',
  'RABBIT_FEMALE': '22_小母兔',
  'RABBIT_MALE':   '23_小公兔',
  'OX_MALE':       '24_小公牛',
  'RAT_MALE':      '25_小公鼠',
  'RAT_FEMALE':    '26_小母鼠',
  // CAT 没有样例图,用虎(猫科)替代,视觉上更接近
  'CAT_MALE':      '10_小公虎',
  'CAT_FEMALE':    '09_小母虎',
};

/**
 * 获取宠物样例图URL
 * @param {string} species - 物种代码 (CAT/DOG/RAT...)
 * @param {string} gender - 性别 (MALE/FEMALE)
 * @returns {string} 图片路径,如 ./pets/samples/03_小公狗_transparent.png
 */
export function petSampleUrl(species, gender) {
  const key = `${species}_${gender}`;
  const fileName = SAMPLE_MAP[key];
  if (fileName) {
    return `./pets/samples/${fileName}_transparent.png`;
  }
  // 兜底:尝试用中文名构造文件名
  const cnSpecies = SPECIES_CN[species] || '狗';
  const cnGender = GENDER_CN[gender] || '公';
  return `./pets/samples/小${cnGender}${cnSpecies}_transparent.png`;
}

/**
 * 从宠物对象统一提取图片URL(解决三处字段名不一致问题)
 * 兼容 typeCode/code/species + gender/sex
 */
export function petImgFrom(pet) {
  if (!pet) return '';
  const code = pet.typeCode || pet.code || pet.species || 'DOG';
  const gender = pet.gender || pet.sex || 'MALE';
  return petSampleUrl(code, gender);
}

export { SPECIES_CN, GENDER_CN, SAMPLE_MAP };
