// 宠物形象常量:emoji 占位(M4 拍板:开源素材管线 + emoji 回退)
// 素材目录约定:src/assets/pets/{typeCode}/{gender}/{action}/{n}.png
// action: idle / play / happy / danger / dead
export const PET_EMOJI = {
  RAT: '🐭', OX: '🐮', TIGER: '🐯', RABBIT: '🐰', DRAGON: '🐲',
  SNAKE: '🐍', HORSE: '🐴', GOAT: '🐐', MONKEY: '🐵', ROOSTER: '🐔',
  DOG: '🐶', PIG: '🐷', CAT: '🐱',
};

export function petEmoji(typeCode, gender) {
  const base = PET_EMOJI[typeCode] || '🐾';
  return gender === 'MALE' ? base + '♂' : base + '♀';
}

export function rarityText(r) {
  return { NORMAL: '普通', RARE: '稀有', LEGEND: '传说' }[r] || r || '';
}

export const PLAY_BUBBLES = [
  '嘿嘿,再玩一会儿!',
  '最喜欢你啦!',
  '今天也要加油学习哦!',
  '摸摸头~',
];
