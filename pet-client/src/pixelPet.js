/*
 * 像素宠物引擎 —— 精确迁移自 pixel-pet-app.html
 * 包含: SPRITES(13种像素宠物) / SPRITE_PAL / gridToCanvas / drawScene / GLYPHS / glyphData
 */

/* ═══════════════ 调色板(从 CSS 变量读取,与 style.css 同步) ═══════════════ */
const cssVar = (n) => getComputedStyle(document.documentElement).getPropertyValue(n).trim();
let C = {};
function resolveColors() {
  const keys = ['line','ink','cream','pink','heart','fur-cat','fur-cat-d','fur-bun','fur-bun-d','fur-dog','fur-dog-d',
    'wall','wall-shade','wain','wain-d','wood','wood-d','wood-l','sky','sky-d','sun','rug','rug-d','rug-c',
    'pot','leaf','leaf-d','book1','book2','book3','bowl','fish','zzz','spark','bubble'];
  C = {};
  keys.forEach(k => { C[k] = cssVar('--p-' + k); C[k.replace(/-/g, '_')] = cssVar('--p-' + k); });
}

/* ═══════════════ 随机数(可种子) ═══════════════ */
function mulberry32(a) {
  return function () {
    a |= 0; a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}
let rnd = mulberry32(20260701);
function resetRnd(seed) { rnd = mulberry32(seed || 20260701); }

/* ═══════════════ 像素网格 → Canvas ═══════════════ */
function gridToCanvas(grid, pal, blink) {
  const w = Math.max(...grid.map((r) => r.length));
  const h = grid.length;
  const cv = document.createElement('canvas');
  cv.width = w; cv.height = h;
  const cx = cv.getContext('2d');
  for (let y = 0; y < h; y++) {
    const row = grid[y];
    for (let x = 0; x < row.length; x++) {
      const ch = row[x];
      if (ch === '.') continue;
      if (ch === 'I' && blink) { cx.fillStyle = pal.O; cx.fillRect(x, y, 2, 1); continue; }
      cx.fillStyle = pal[ch] || pal.O;
      cx.fillRect(x, y, 1, 1);
    }
  }
  // 裁剪透明边距
  const data = cx.getImageData(0, 0, w, h).data;
  let minX = w, minY = h, maxX = -1, maxY = -1;
  for (let y = 0; y < h; y++)
    for (let x = 0; x < w; x++) {
      if (data[(y * w + x) * 4 + 3] > 0) {
        if (x < minX) minX = x;
        if (x > maxX) maxX = x;
        if (y < minY) minY = y;
        if (y > maxY) maxY = y;
      }
    }
  if (maxX < 0) return cv;
  const tw = maxX - minX + 1, th = maxY - minY + 1;
  const out = document.createElement('canvas');
  out.width = tw; out.height = th;
  out.getContext('2d').drawImage(cv, minX, minY, tw, th, 0, 0, tw, th);
  return out;
}

/* ═══════════════ 13 种像素宠物(侧面全身 · 精确像素数据) ═══════════════ */
const SPECIES = {
  RAT: { n: '鼠' }, OX: { n: '牛' }, TIGER: { n: '虎' }, RABBIT: { n: '兔' },
  DRAGON: { n: '龙' }, SNAKE: { n: '蛇' }, HORSE: { n: '马' }, SHEEP: { n: '羊' },
  MONKEY: { n: '猴' }, ROOSTER: { n: '鸡' }, DOG: { n: '狗' }, PIG: { n: '猪' }, CAT: { n: '猫' },
};

const SPRITES = {
  CAT: [
    '...O.....O...............',
    '..OEO...OEO..............',
    '..OEO...OEO..............',
    '..OFF...OFF..............',
    '.OFFFOOFFFO..............',
    '.OFFFFFFFFFFFFFFFOTTT...',
    '.OFFFFIIFFFFFFFFFOTTT...',
    '.OFFFFFCFFFFFFFFFOOFO...',
    '.OLLFNFFFFFFFFFFOOFO....',
    '.OLLFFFFFFFFFFFFOOFO....',
    '.OLLLLFFFFFFFFFOOFO.....',
    '.OLLLLFFFFFFFFFOOFO.....',
    '.OLLLFFFFFFFFFFFFOOFO...',
    '.OFFFFFFFFFFFFFFOOFO....',
    '.OFFFFFFFFFFFFFFOOFO....',
    '...OFO......OFO...OFO...',
    '....L........L..........',
  ],
  TIGER: [
    '...D.....D...............',
    '..OEO...OEO..............',
    '..OEO...OEO..............',
    '..OFF...OFF..............',
    '.OFFFOOFFFO..............',
    '.OFFFFFFFFFFFFFFFODDD...',
    '.OFFFFIIFFFFFFFFFODDD...',
    '.OFFFFFCFFFFFFFFFOODO...',
    '.OLLFNDDFFFFFFFFDOOFO...',
    '.OLLFDDFFFFFFFFFDOOFO...',
    '.OLLLDDFFFFFFFFDOOFO....',
    '.OLLLDDFFFFFFFFDOOFO....',
    '.OLLLDDDFFFFFFFFDOOFO...',
    '.ODFFFFFFFFFFFFFFOOFO....',
    '.ODFFFFFFFFFFFFFFOOFO....',
    '...OFO......OFO...OFO...',
    '....L........L..........',
  ],
  DOG: [
    'OO..............OO',
    'OFO............OFO',
    'OFO............OFO',
    'OFF.............FFO',
    '.OFFFOOFFFO........',
    '.OFFFFFFFFFFFFFO...',
    '.OFFFFIIFFFFFFFFO..',
    '.OFFFFFCFFFFFFFFO..',
    '.OLLFNFFFFFFFFFFO..',
    '.OLLFFFFFFFFFFFFO..',
    '.OLLLLFFFFFFFFFO...',
    '.OLLLLFFFFFFFFFO...',
    '.OLLLFFFFFFFFFFFFO.',
    '.OFFFFFFFFFFFFFFO.O.',
    '.OFFFFFFFFFFFFFFO..O',
    '...OFO......OFO.....',
    '....L........L......',
  ],
  RABBIT: [
    '..O......O.................',
    '.OEO....OEO................',
    '.OEO....OEO................',
    '.OEO....OEO................',
    '.OEO....OEO................',
    '.OFFO...OFFO...............',
    '.OFFFOOFFFO..............',
    '.OFFFFFFFFFFFFFFFO.......',
    '.OFFFFIIFFFFFFFFO........',
    '.OFNLLFFFFFFFFFO.........',
    '.OFCCLLFFFFFFFFOTT......',
    '.OFLLLLFFFFFFFOTT.......',
    'OFLLFFFFFFFFFFFFO.......',
    'OFLLFFFFFFFFFFFFO.......',
    '.OFFLLLLLFFFFO...........',
    '..OFFFFFFFFFO............',
    '...OFO......OFO..........',
    '....L........L............',
  ],
  RAT: [
    '..OOO......OOO..',
    '.OEEEO....OEEEO.',
    '.OEEEO....OEEEO.',
    '..OFFO....OFFO..',
    '..OFFFOOOFFFO...',
    '..OFFFFFFFFO....',
    '.OFFFFIIFFFO....',
    '.OFNLLFFFFFO....',
    '.OFLLLLFFFFO....',
    '..OFFFFFFFO.....',
    '...OFO..OFO.....',
    '....L....L......',
    '..........OO....',
    '.........O..O...',
  ],
  OX: [
    'O..............O',
    '.O............O.',
    '..OOO......OOO..',
    '..OFO......OFO..',
    '..OFFFOOOFFFO..',
    '.OFFLLFOFLLLFO..',
    '.OFIILLFOLLLFFO.',
    '.OFNNLLFOLLLFFO.',
    '.OFLLLLFOLLLFFO.',
    '.OFFLLLLLFFFO.O.',
    '.OFFFFFFFFFO..O.',
    '..OFO....OFO.OO.',
    '..O.O....O.O....',
  ],
  DRAGON: [
    'O..............O',
    '.O............O.',
    '..OOO......OOO..',
    '..OFO......OFO..',
    '..OFFFOOOFFFO..',
    '.OFFLLFOFLLLFO..',
    '.OFIILLFOLLLFFOW',
    '.OFNLLLFOLLLFFOW',
    '.OFLLLLFOLLLFFO.',
    '.OFFLLLLLFFFO...',
    '..OFFFFFFFO..OOO',
    '...OFO.OFO...O.O',
    '....L...L...OO..',
  ],
  SNAKE: [
    'OOO.......',
    'OEEO......',
    'OFIIO.....',
    'OFNNO.....',
    'OOOO......',
    '.O.O......',
    '..OO....OO',
    '..OO....OO',
    '..OO....OO',
    '..OOOOOOOO',
    '...OOOOOO.',
  ],
  HORSE: [
    'O..........O',
    '.OEO......OEO',
    '.OEO......OEO',
    '..OFFOOOOFFO',
    '.OFFLLOOFFLFO',
    '.OFIILLOOLLFO',
    '.OFNNLLOOLLFO',
    '.OFLLLLOOLLFO',
    'D.OFFFFOOLLO.',
    'D.OFFFFOOLLO.',
    '.DOFFFGOOLLO.',
    '..OFO..OFO...',
    '..O.O..O.O...',
    '......OO.....',
    '.....O.......',
  ],
  SHEEP: [
    '...O....O...',
    '..OEO..OEO..',
    '..OEO..OEO..',
    '..OFFOOFFO..',
    '.OFNFFOFFFFO',
    '.OFFWWWWWFFO',
    '.OFFWWWWWFFO',
    '.OFFWWWWWFFO',
    '.OFFWWWWWFFO',
    '..OFFWWFFO..',
    '...OFO.OFO..',
    '...O...O....',
  ],
  MONKEY: [
    '..OO....OO..',
    '.OEO....OEO.',
    '.OEO....OEO.',
    '..OFFO..OFFO',
    '.OFFFLOOOFFO',
    '.OFIILLOFFFFO',
    '.OFNNLLOFFFFO',
    '.OFLLLLOFFFFO',
    '.OFFFFFFFO...',
    '..OFFFFFFO...',
    '...OFO.OFO...',
    '....O...O....',
    '.......OO....',
    '......O..O...',
  ],
  ROOSTER: [
    '...O..O..O...',
    '..OOO.OOO.OO.',
    '..OEO...OEO..',
    '..OFFO..OFFO.',
    '.OFFFOOOFFFO.',
    '.OFFFFIIFFFO.',
    '.OFNBBFFFFO..',
    '.OFKFFFFFLO..',
    '.OFFFFFFFO...',
    '..OFFFFFFO...',
    '..OFO..OFO...',
    '..O....O.....',
    '..O.....OO...',
    '........O....',
    '.......O.....',
    '......O......',
  ],
  PIG: [
    '.OOO......OOO.',
    '.OEOO....OOEO.',
    '.OEOO....OOEO.',
    '..OFFO..OFFO..',
    '.OFFFOOOFFFO..',
    '.OFPPPPPFFFO..',
    '.OFPPPPPFFFO..',
    '.OFIIFFFFFO...',
    '.OFNFFFFFO....',
    '.OFFFFFFFO....',
    '..OFO..OFO....',
    '...O....O.....',
    '........OO....',
    '.......O..O...',
  ],
};

const SPRITE_PAL = {
  CAT:    { O: '#6b4f3a', F: '#f0a054', D: '#d98a3c', L: '#fdf1dc', E: '#f2a2b5', I: '#4a3a2a', N: '#4a3a2a', T: '#fdf1dc', C: '#f2a2b5' },
  TIGER:  { O: '#6b4f3a', F: '#f0a054', D: '#c77f2e', L: '#fdf1dc', E: '#f2a2b5', I: '#4a3a2a', N: '#4a3a2a', T: '#c77f2e', C: '#f2a2b5' },
  DOG:    { O: '#6b4f3a', F: '#c9a06c', D: '#a87f4c', L: '#fdf1dc', E: '#f2a2b5', I: '#4a3a2a', N: '#4a3a2a', C: '#f2a2b5' },
  RABBIT: { O: '#6b4f3a', F: '#f2ebe0', D: '#ddd0bd', L: '#ffffff', E: '#f2a2b5', I: '#4a3a2a', N: '#4a3a2a', T: '#ffffff', C: '#f2a2b5' },
  RAT:    { O: '#6b4f3a', F: '#b8a494', D: '#9a8774', L: '#efe6da', E: '#f2a2b5', I: '#4a3a2a', N: '#4a3a2a', T: '#f2a2b5' },
  OX:     { O: '#6b4f3a', F: '#c9a06c', D: '#a87f4c', L: '#efe0c8', E: '#f2a2b5', I: '#4a3a2a', N: '#4a3a2a', T: '#6b4f3a' },
  DRAGON: { O: '#3f6b55', F: '#7fbf9e', D: '#5f9e80', L: '#d9f0e2', E: '#f2a2b5', I: '#2f5c48', N: '#2f5c48', T: '#5f9e80', W: '#5f9e80' },
  SNAKE:  { O: '#3f6b55', F: '#93c47d', D: '#6fa85c', L: '#d9f0d0', E: '#f2a2b5', I: '#2f5c48', N: '#2f5c48' },
  HORSE:  { O: '#6b4f3a', F: '#b98a5e', D: '#8a5f38', L: '#efe0c8', E: '#f2a2b5', I: '#4a3a2a', N: '#4a3a2a', G: '#8a5f38', T: '#8a5f38' },
  SHEEP:  { O: '#6b4f3a', F: '#c9b49a', D: '#a08a70', L: '#ffffff', E: '#f2a2b5', I: '#4a3a2a', N: '#4a3a2a', W: '#ffffff' },
  MONKEY: { O: '#6b4f3a', F: '#c98d5e', D: '#a06c40', L: '#f5dcc0', E: '#f2a2b5', I: '#4a3a2a', N: '#4a3a2a', T: '#a06c40' },
  ROOSTER:{ O: '#6b4f3a', F: '#e98a5e', D: '#c96a3a', L: '#f5bd9e', E: '#f2a2b5', I: '#4a3a2a', N: '#4a3a2a', K: '#d1453b', B: '#f5a03c' },
  PIG:    { O: '#6b4f3a', F: '#f2a2b5', D: '#d9829a', L: '#fad6de', E: '#e88aa0', I: '#4a3a2a', N: '#4a3a2a', P: '#e88aa0', T: '#d9829a' },
};

function addBow(grid) {
  return ['...PP..PP.....'.split(''), '..PPPPPPPP....'.split(''), '....PP..PP....'.split('')].concat(grid);
}

function petCodeName(code) {
  return (SPECIES[code] || { n: code }).n;
}

const PET_CACHE = {};
function petFrames(code, sex) {
  const key = code + '_' + sex;
  if (PET_CACHE[key]) return PET_CACHE[key];
  const pal = SPRITE_PAL[code] || SPRITE_PAL.CAT;
  let grid = (SPRITES[code] || SPRITES.CAT).map((r) => r.split(''));
  if (sex === 'FEMALE') grid = addBow(grid);
  PET_CACHE[key] = {
    normal: gridToCanvas(grid, pal, false),
    blink: gridToCanvas(grid, pal, true),
    w: Math.max(...grid.map((r) => r.length)),
    h: grid.length,
  };
  return PET_CACHE[key];
}

/* ═══════════════ 场景绘制(小屋/书房/商店/花园/阁楼/设置) ═══════════════ */
function drawScene(canvas, type, petInfo) {
  if (!C.line) resolveColors();
  resetRnd(20260701);
  const w = canvas.clientWidth, h = canvas.clientHeight;
  if (!w || !h) return;
  const dpr = Math.min(window.devicePixelRatio || 1, 2);
  canvas.width = w * dpr;
  canvas.height = h * dpr;
  const ctx = canvas.getContext('2d');
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  ctx.imageSmoothingEnabled = false;
  const rect = (x, y, ww, hh, c) => { ctx.fillStyle = c; ctx.fillRect(Math.round(x), Math.round(y), Math.round(ww), Math.round(hh)); };
  const garden = type === 'garden';
  const wallH = garden ? h * 0.5 : h * 0.52;
  const floorY = garden ? h * 0.72 : h * 0.68;
  const floorH = h - floorY;

  if (garden) {
    const g = ctx.createLinearGradient(0, 0, 0, h);
    g.addColorStop(0, '#bfe3f2'); g.addColorStop(0.7, '#dff0f7'); g.addColorStop(1, '#cfe9c4');
    ctx.fillStyle = g; ctx.fillRect(0, 0, w, h);
    rect(0, floorY, w, floorH, '#a8d69a');
    for (let x = 0; x < w; x += 14) if (rnd() < 0.3) rect(x, floorY + rnd() * floorH, 2, 2, '#8fc47f');
    const cloud = (cx, cy) => { rect(cx, cy, 26, 8, '#fff'); rect(cx + 6, cy - 6, 14, 6, '#fff'); rect(cx - 8, cy + 4, 42, 6, '#fff'); };
    cloud(w * 0.12, h * 0.14); cloud(w * 0.62, h * 0.09); cloud(w * 0.85, h * 0.2);
    rect(w * 0.3, h * 0.08, 14, 14, '#ffd76e');
    for (let x = 0; x < w; x += 40) rect(x, floorY - 16, 6, 16, '#c8a16b');
    rect(0, floorY - 6, w, 4, '#b98a5e');
    const tree = (tx) => { rect(tx, floorY - 26, 12, 26, '#9c6b3f'); rect(tx - 12, floorY - 52, 36, 28, '#6fae63'); rect(tx - 6, floorY - 60, 24, 12, '#6fae63'); };
    tree(w * 0.08); tree(w * 0.94);
    for (let i = 0; i < 10; i++) { const fx = w * (0.12 + i * 0.08); rect(fx, floorY + 6, 4, 8, '#8fc47f'); rect(fx, floorY, 4, 4, ['#f2a2b5', '#ffd76e', '#f0a054', '#fff'][i % 4]); }
  } else if (type === 'home' || type === 'auth') {
    rect(0, 0, w, wallH, C.wall);
    for (let x = 0; x < w; x += 26) if (rnd() < 0.25) rect(x, rnd() * wallH, 1, 1, C.wall_shade);
    rect(0, wallH, w, floorY - wallH, C.wain);
    for (let x = 14; x < w; x += 92) rect(x, wallH + 3, 2, floorY - wallH - 6, C.wain_d);
    rect(0, floorY - 4, w, 4, C.wain_d);
    const plankH = Math.max(14, floorH / 5);
    const tones = [C.wood, C.wood_l, C.wood, C.wood_d, C.wood];
    for (let i = 0; i < 5; i++) {
      const y = floorY + i * plankH;
      rect(0, y, w, plankH, tones[i]);
      for (let x = 0; x < w; x += 9) if (rnd() < 0.2) rect(x, y + 3, 3, 1, i % 2 ? C.wood_l : C.wood_d);
      rect(0, y, w, 1, C.wood_d);
    }
    // 圆窗 + 阳光光柱
    const winCX = w * 0.16, winCY = wallH * 0.4, winR = Math.min(h, w) * 0.105;
    rect(winCX - winR * 0.8, winCY + winR * 0.6, winR * 1.6, 7, C.wood_d);
    const arch = () => { ctx.beginPath(); ctx.arc(winCX, winCY - winR * 0.6, winR * 0.6, Math.PI, 0); ctx.rect(winCX - winR * 0.6, winCY - winR * 0.6, winR * 1.2, winR * 0.8); ctx.closePath(); };
    arch(); ctx.fillStyle = C.sky_d; ctx.fill();
    ctx.fillStyle = C.sky; ctx.fillRect(winCX - winR * 0.42, winCY - winR * 0.42, winR * 0.84, winR * 0.66);
    ctx.fillStyle = C.sun; ctx.fillRect(winCX - winR * 0.28, winCY - winR * 0.34, winR * 0.16, winR * 0.16);
    ctx.strokeStyle = C.wood; ctx.lineWidth = Math.max(3, winR * 0.14); ctx.lineJoin = 'round';
    arch(); ctx.stroke();
    ctx.beginPath();
    ctx.moveTo(winCX, winCY - winR * 0.6); ctx.lineTo(winCX, winCY + winR * 0.6);
    ctx.moveTo(winCX - winR * 0.6, winCY); ctx.lineTo(winCX + winR * 0.6, winCY);
    ctx.stroke();
    ctx.fillStyle = 'rgba(255,214,160,.16)';
    ctx.beginPath();
    ctx.moveTo(winCX - winR * 0.5, winCY - winR * 0.2);
    ctx.lineTo(winCX - winR * 1.8, h);
    ctx.lineTo(winCX + winR * 1.6, h);
    ctx.lineTo(winCX + winR * 0.5, winCY - winR * 0.2);
    ctx.closePath(); ctx.fill();
    // 挂画
    const fx = w * 0.66, fy = wallH * 0.16, fs = Math.min(w, h) * 0.055;
    rect(fx, fy, fs * 1.5, fs * 1.2, C.wood);
    rect(fx + 3, fy + 3, fs * 1.5 - 6, fs * 1.2 - 6, C.rug_c);
    rect(fx + fs * 0.45, fy + 3, fs * 0.6, fs * 0.18, C.wood_l);
    rect(fx + fs * 0.62, fy + 3 + fs * 0.18, fs * 0.28, fs * 0.34, C.pot);
    rect(fx + fs * 0.55, fy + 3 + fs * 0.1, fs * 0.14, fs * 0.2, C.leaf_d);
    rect(fx + fs * 0.72, fy + 3 + fs * 0.05, fs * 0.14, fs * 0.24, C.leaf);
    rect(fx + fs * 0.9, fy + 3 + fs * 0.12, fs * 0.14, fs * 0.18, C.leaf_d);
    // 吊灯
    rect(w * 0.5 - 1.5, 0, 3, h * 0.09, C.wood_d);
    rect(w * 0.5 - 7, h * 0.09, 14, 9, C.sun);
    rect(w * 0.5 - 5, h * 0.09 + 9, 10, 4, C.wood_d);
    ctx.fillStyle = 'rgba(255,214,160,.13)';
    ctx.beginPath(); ctx.ellipse(w * 0.5, h * 0.2, w * 0.08, h * 0.04, 0, 0, Math.PI * 2); ctx.fill();
    // 置物架
    const sx = w * 0.86, sy = wallH * 0.2, sw = w * 0.1;
    rect(sx, sy + 14, sw, 5, C.wood_d);
    rect(sx, sy + 12, sw, 2, C.wood);
    rect(sx + 4, sy + 2, 8, 10, C.book1);
    rect(sx + 13, sy + 3, 7, 9, C.book2);
    rect(sx + 21, sy + 4, 6, 8, C.book3);
    rect(sx + 30, sy + 8, 6, 4, C.book2);
    rect(sx + 40, sy + 7, 8, 5, C.pot);
    rect(sx + 52, sy + 9, 7, 3, C.heart);
    // 绿植(左下)
    const px0 = w * 0.06, py0 = floorY + floorH * 0.6;
    rect(px0 - 12, py0 - 8, 26, 8, C.pot);
    rect(px0 - 16, py0, 34, 6, C.pot);
    rect(px0 - 14, py0 + 6, 30, 4, C.wood_d);
    rect(px0 - 4, py0 - 14, 10, 6, C.leaf_d);
    rect(px0 - 8, py0 - 26, 7, 12, C.leaf);
    rect(px0 + 3, py0 - 30, 7, 16, C.leaf_d);
    rect(px0 + 10, py0 - 20, 6, 10, C.leaf);
    rect(px0 - 12, py0 - 20, 6, 10, C.leaf);
    rect(px0 - 1, py0 - 34, 3, 6, C.leaf);
    // 宠物垫(右下)
    const bx = w * 0.93, by = floorY + floorH * 0.6;
    rect(bx - 16, by - 6, 44, 14, C.rug_d);
    rect(bx - 12, by - 3, 36, 10, C.rug);
    rect(bx - 10, by, 5, 3, C.rug_c); rect(bx + 5, by, 5, 3, C.rug_c); rect(bx + 20, by, 5, 3, C.rug_c);
    rect(bx - 6, by - 6, 5, 3, C.rug_c); rect(bx + 10, by - 6, 5, 3, C.rug_c);
  } else {
    // 变体房间(书房/商店/阁楼/设置):简化布局
    rect(0, 0, w, wallH, C.wall);
    for (let x = 0; x < w; x += 26) if (rnd() < 0.25) rect(x, rnd() * wallH, 1, 1, C.wall_shade);
    rect(0, wallH, w, floorY - wallH, C.wain);
    for (let x = 14; x < w; x += 92) rect(x, wallH + 3, 2, floorY - wallH - 6, C.wain_d);
    rect(0, floorY - 4, w, 4, C.wain_d);
    const plankH2 = Math.max(14, floorH / 5);
    const tones2 = [C.wood, C.wood_l, C.wood, C.wood_d, C.wood];
    for (let i = 0; i < 5; i++) {
      const y = floorY + i * plankH2;
      rect(0, y, w, plankH2, tones2[i]);
      for (let x = 0; x < w; x += 9) if (rnd() < 0.2) rect(x, y + 3, 3, 1, i % 2 ? C.wood_l : C.wood_d);
      rect(0, y, w, 1, C.wood_d);
    }
    const winCX = w * 0.14, winCY = wallH * 0.4, winR = Math.min(h, w) * 0.1;
    rect(winCX - winR * 0.8, winCY + winR * 0.6, winR * 1.6, 6, C.wood_d);
    ctx.fillStyle = C.sky_d; ctx.beginPath(); ctx.arc(winCX, winCY - winR * 0.6, winR * 0.6, Math.PI, 0); ctx.rect(winCX - winR * 0.6, winCY - winR * 0.6, winR * 1.2, winR * 0.8); ctx.fill();
    ctx.fillStyle = C.sky; ctx.fillRect(winCX - winR * 0.45, winCY - winR * 0.45, winR * 0.9, winR * 0.7);
    ctx.fillStyle = C.sun; ctx.fillRect(winCX - winR * 0.3, winCY - winR * 0.38, winR * 0.16, winR * 0.16);
    ctx.strokeStyle = C.wood; ctx.lineWidth = Math.max(3, winR * 0.14);
    ctx.beginPath(); ctx.arc(winCX, winCY - winR * 0.6, winR * 0.6, Math.PI, 0); ctx.rect(winCX - winR * 0.6, winCY - winR * 0.6, winR * 1.2, winR * 0.8); ctx.stroke();
    rect(w * 0.72, wallH * 0.14, Math.min(w, h) * 0.07, Math.min(w, h) * 0.06, C.wood);
    rect(w * 0.72 + 3, wallH * 0.14 + 3, Math.min(w, h) * 0.07 - 6, Math.min(w, h) * 0.06 - 6, C.rug_c);
    rect(w * 0.72 + Math.min(w, h) * 0.02, wallH * 0.14 + 4, 6, 6, C.leaf);
  }

  // 场景道具
  if (type === 'study') {
    rect(w * 0.06, floorY - 6, w * 0.5, 8, C.wood_d);
    rect(w * 0.07, floorY - 24, w * 0.18, 18, C.wood);
    rect(w * 0.08, floorY - 22, 8, 14, C.book1); rect(w * 0.17, floorY - 24, 6, 16, C.book2); rect(w * 0.24, floorY - 20, 7, 12, C.book3);
    rect(w * 0.36, floorY - 18, 4, 12, C.wood); rect(w * 0.36, floorY - 22, 10, 5, C.sun);
  } else if (type === 'shop') {
    rect(w * 0.1, floorY - 34, w * 0.3, 34, C.wood);
    rect(w * 0.12, floorY - 28, w * 0.26, 8, C.wood_l);
    rect(w * 0.13, floorY - 18, 8, 8, C.fish); rect(w * 0.23, floorY - 18, 8, 8, C.pot); rect(w * 0.33, floorY - 18, 8, 8, C.book1);
    rect(w * 0.7, floorY - 26, w * 0.16, 26, C.wood_d);
    rect(w * 0.72, floorY - 22, 6, 8, C.book2); rect(w * 0.79, floorY - 24, 6, 10, C.book3);
    for (let i = 0; i < 5; i++) rect(w * 0.1 + i * 12, floorY - 42, 12, 8, i % 2 ? C.heart : C.rug_c);
    rect(w * 0.1, floorY - 34, w * 0.3, 3, C.wood_d);
  } else if (type === 'tools') {
    rect(w * 0.12, floorY - 20, 26, 20, C.wood_d);
    rect(w * 0.13, floorY - 18, 10, 6, '#c8b8a0'); rect(w * 0.25, floorY - 18, 10, 6, '#c8b8a0');
    rect(w * 0.76, floorY - 14, 16, 14, '#a9a9a9'); rect(w * 0.77, floorY - 20, 4, 6, '#8a8a8a');
  } else if (type === 'settings') {
    rect(w * 0.14, wallH * 0.32, 14, 14, C.pot); rect(w * 0.17, wallH * 0.26, 14, 14, C.book2);
  }
  if (type !== 'garden') {
    // 地毯(宠物站立处)
    const rgX = w * 0.5, rgY = floorY + floorH * 0.4, rgRX = w * 0.14, rgRY = floorH * 0.18;
    ctx.fillStyle = C.rug_d; ctx.beginPath(); ctx.ellipse(rgX, rgY, rgRX, rgRY, 0, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = C.rug; ctx.beginPath(); ctx.ellipse(rgX, rgY, rgRX * 0.82, rgRY * 0.8, 0, 0, Math.PI * 2); ctx.fill();
    ctx.fillStyle = C.rug_c;
    for (let a = 0; a < Math.PI * 2; a += 0.5) { ctx.beginPath(); ctx.ellipse(rgX, rgY, rgRX * 0.55, rgRY * 0.55, 0, a, a + 0.18); ctx.fill(); }
  }
  // 宠物脚下阴影
  ctx.fillStyle = 'rgba(60,40,20,.18)';
  ctx.beginPath(); ctx.ellipse(w * 0.5, h * 0.74 + 3, w * 0.07, 7, 0, 0, Math.PI * 2); ctx.fill();
  // 背景点缀(小星星/爱心)
  for (let i = 0; i < 5; i++) { const sx = w * (0.05 + i * 0.2), sy = h * (0.08 + (i % 3) * 0.09); rect(sx, sy, 3, 3, C.spark); rect(sx + 1, sy - 1, 1, 5, C.spark); rect(sx - 1, sy + 1, 5, 1, C.spark); }
  rect(w * 0.05, h * 0.3, 4, 4, C.heart); rect(w * 0.93, h * 0.24, 4, 4, C.heart);

  // 认证页:在地板上画宠物
  if (type === 'auth' && petInfo) {
    const acode = petInfo.code || 'CAT', asex = petInfo.sex || 'MALE';
    const pf = petFrames(acode, asex);
    const s = Math.max(7, Math.min(10, Math.round(Math.min(w, h) * 0.024)));
    const pw = pf.w * s, ph = pf.h * s;
    const px = Math.max(pw / 2 + 40, w * 0.23);
    ctx.fillStyle = 'rgba(60,40,20,.18)';
    ctx.beginPath(); ctx.ellipse(px, h * 0.74 + 3, pw * 0.42, 6, 0, 0, Math.PI * 2); ctx.fill();
    ctx.imageSmoothingEnabled = false;
    ctx.drawImage(pf.normal, px - pw / 2, h * 0.74 - ph, pw, ph);
  }
}

/* ═══════════════ 像素图标库(16×16 glyph → dataURL) ═══════════════ */
const GLYPHS = {
  home:   ['................','....OO..OO....','...OOOOOOOO...','..OO.OOOO.OO..','.OO..OOOO..OO.','.OO..OOOO..OO.','.OO..OOOO..OO.','.OO..OOOO..OO.','.OO..OOOO..OO.','.OO..OOOO..OO.','.OO..OOOO..OO.','.OO..OOOO..OO.','.OOO.OOOO.OOO.','..OOOOOOOOOO..','....OOOOOO....','................'],
  book:   ['................','.OOOOOOOOOOOO..','.O........OO....','.O.OOOOO..OO....','.O.OOO.O..OO....','.O.O.O.O..OO....','.O.O.OOO..OO....','.O.OOO.O..OO....','.O.O.O.O..OO....','.O.OOO.O..OO....','.O.O.O.O..OO....','.O.OOO.O..OO....','.O.......OO....','.OOOOOOOOOOOO...','................','................'],
  bag:    ['....OOOOOO....','..OOOOOOOOOO..','.OO..OOOO..OO.','.OO..OOOO..OO.','.OOOOOOOOOOOO.','.OOOOOOOOOOOO.','.OO.OOOOOO.OO.','.OO.OOOOOO.OO.','.OO.OOOOOO.OO.','.OO.OOOOOO.OO.','.OO.OOOOOO.OO.','.OO.OOOOOO.OO.','.OO........OO.','.OOOOOOOOOOOO.','................','................'],
  chat:   ['................','..OOOOOOOOOO..','.OOOOOOOOOOOO.','.OO........OO.','.OO........OO.','.OO........OO.','.OO........OO.','.OO........OO.','.OO........OO.','.OOOOOOOOOOOO.','..OOOOOOOOOO..','......OO......','.....OOOO.....','....OO..OO....','................','................'],
  puzzle: ['....OOO.......','...OOOOO......','..OOOOOOOOO...','.OOOOOOOOOOO..','.OOOOOOOOOOO..','.OOOOOOOOOOO..','.OOOOOOOOOOO..','.OOOOOOOOOOO..','..OOOOOOOOO...','...OOOOOOO....','....OOOOO.....','.....OOO......','................','................','................','................'],
  gear:   ['.....OOO......','....OOOOO.....','...OOOOOOO....','OO.OOOOOOO.OO.','OOO.OOOOO.OOO.','OOOOOOOOOOOOO.','OOOOOOOOOOOOO.','OOOOOOOOOOOOO.','OOOOOOOOOOOOO.','OOO.OOOOO.OOO.','OO.OOOOOOO.OO.','...OOOOOOO....','....OOOOO.....','.....OOO......','................','................'],
  heart:  ['................','..OO....OO....','.OOOO..OOOO...','.OOOOOOOOOOO..','.OOOOOOOOOOO..','.OOOOOOOOOOO..','..OOOOOOOOO...','...OOOOOOO....','....OOOOO.....','.....OOO......','......O.......','................','................','................','................','................'],
  star:   ['.......O.......','......OOO......','......OOO......','.OOOOOOOOOOOOO.','.OOOOOOOOOOOOO.','..OOOOOOOOOOO..','...OOOOOOOOO...','....OOOOOOO....','.....OOOOO.....','......OOO......','.......O.......','................','................','................','................','................'],
  fish:   ['................','................','................','...OOOO........','..OOOOOOO......','.OOOOOOOOOOO...','.OOOOOOOOOOOOO.','.OOOOOOOOOOOOO.','.OOOOOOOOOOO...','..OOOOOOO......','...OOOO........','................','................','................','................','................'],
  bone:   ['................','..OOO....OOO...','.OOOOOOOOOOOOO.','.OOOOOOOOOOOOO.','.OOOOOOOOOOOOO.','..OOO....OOO...','................','................','................','................','................','................','................','................','................','................'],
  clock:  ['................','....OOOOOO....','..OOOOOOOOOO..','.OOOOOOOOOOOO.','.OOO.OOOO.OOO.','.OO.OOOOOO.OO.','.OO.OOOOOO.OO.','.OO.OOOOOO.OO.','.OO.OOO.OO.OO.','.OOO.OOO.OOO.','.OOOOOOOOOOOO.','..OOOOOOOOOO..','....OOOOOO....','................','................','................'],
  egg:    ['................','......OO......','.....OOOO.....','....OOOOOO....','...OOOOOOOO...','..OOOOOOOOOO..','.OOOOOOOOOOOO.','.OOOOOOOOOOOO.','.OOOOOOOOOOOO.','..OOOOOOOOOO..','...OOOOOOOO...','....OOOOOO....','.....OOOO.....','......OO......','................','................'],
  paw:    ['................','..OO....OO.....','.OOOO..OOOO....','.OOOO..OOOO....','..OO....OO.....','................','..OO....OO.....','.OOOO..OOOO....','.OOOO..OOOO....','..OO....OO.....','................','................','................','................','................','................'],
  zzz:    ['................','...OO..........','..OOOOO........','...OOOOO.......','..OOOOO........','.OOOOO........','.OOOO..........','................','................','................','................','................','................','................','................','................'],
  crown:  ['................','...O..O..O.....','..OOO.OOO.OO...','.OOOOOOOOOOOO..','.OOOOOOOOOOOO..','.OOOOOOOOOOOO..','.OOOOOOOOOOOO..','.OOOOOOOOOOOO..','.OOOOOOOOOOOO..','.OOOOOOOOOOOO..','.OOOOOOOOOOOO..','..OOOOOOOOOO...','...OOOOOOOO....','....OOOOOO.....','................','................'],
  camera: ['................','....OOOOOO.....','..OOOOOOOOOO...','.OOOOOOOOOOOO..','.OOO.OOOO.OOO..','.OOOOOOOOOOOO..','.OOOOOOOOOOOO..','.OOOOOOOOOOOO..','.OOOOOOOOOOOO..','.OOOOOOOOOOOO..','.OOOOOOOOOOOO..','..OOOOOOOOOO...','....OOOOOO.....','................','................','................'],
  grave:  ['................','...OOOOOOOOO...','...O.OOO.O.O...','...OOOOOOOOO...','...OOOOOOOOO...','...OOOOOOOOO...','...OOOOOOOOO...','...OOOOOOOOO...','...OOOOOOOOO...','...OOOOOOOOO...','...OOOOOOOOO...','...OOOOOOOOO...','..OOOOOOOOOOO..','.OOOOOOOOOOOOO.','................','................'],
  gear2:  ['................','.....OOO.......','....OOOOO......','...OOOOOOO.....','OO.OOOOOOO.OO..','OOO.OOOOO.OOO..','OOOOOOOOOOOOO..','OOOOOOOOOOOOO..','OOOOOOOOOOOOO..','OOO.OOOOO.OOO..','OO.OOOOOOO.OO..','...OOOOOOO.....','....OOOOO......','.....OOO.......','................','................'],
  mail:   ['................','..OOOOOOOOOO...','.OOOOOOOOOOOO..','.OO.OOOOOO.OO..','.OOO.OOOO.OOO..','.OOOO.OO.OOOO..','.OOOOOOOOOOOO..','.OOOOOOOOOOOO..','.OOOOOOOOOOOO..','..OOOOOOOOOO...','................','................','................','................','................','................'],
};

const glyphCache = {};
function glyphData(name, color) {
  const key = name + '_' + (color || 'd');
  if (glyphCache[key]) return glyphCache[key];
  const g = GLYPHS[name] || GLYPHS.star;
  const w = g[0].length, h = g.length;
  const cv = document.createElement('canvas');
  cv.width = w; cv.height = h;
  const cx = cv.getContext('2d');
  cx.fillStyle = color || '#4a3a2a';
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) if (g[y][x] === 'O') cx.fillRect(x, y, 1, 1);
  glyphCache[key] = cv.toDataURL();
  return glyphCache[key];
}

/* ═══════════════ 导出 API ═══════════════ */
export {
  SPECIES, SPRITES, SPRITE_PAL, GLYPHS,
  resolveColors, gridToCanvas, petFrames, addBow,
  drawScene, glyphData, petCodeName,
};

export function pixelPetFrames(code, sex) {
  return petFrames(code || 'CAT', sex || 'MALE');
}

export function pixelPetDataUrl(code, sex) {
  return petFrames(code || 'CAT', sex || 'MALE').normal.toDataURL();
}

export function pixelPetName(code) {
  return (SPECIES[code] || { n: code }).n;
}
