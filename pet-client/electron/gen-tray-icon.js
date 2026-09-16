// 一次性工具:生成托盘图标(32x32 橙色圆脸 PNG),纯 Node 实现(zlib + 手写 PNG 块)
// 输出: pet-client/electron/tray-icon.png
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const W = 32, H = 32;
const px = Buffer.alloc(W * H * 4, 0); // RGBA,透明

function set(x, y, r, g, b, a = 255) {
  if (x < 0 || y < 0 || x >= W || y >= H) return;
  const i = (y * W + x) * 4;
  px[i] = r; px[i + 1] = g; px[i + 2] = b; px[i + 3] = a;
}

// 圆
function fillCircle(cx, cy, rad, r, g, b) {
  for (let y = cy - rad; y <= cy + rad; y++) {
    for (let x = cx - rad; x <= cx + rad; x++) {
      const d = Math.hypot(x - cx, y - cy);
      if (d <= rad) set(x, y, r, g, b);
    }
  }
}

const ORANGE = [255, 159, 69];
const DARK = [74, 47, 0];
fillCircle(16, 17, 14, ...ORANGE);          // 脸
fillCircle(11, 13, 2.4, ...DARK);           // 左眼
fillCircle(21, 13, 2.4, ...DARK);           // 右眼
for (let x = 13; x <= 19; x++) {            // 微笑
  const y = 18 + Math.round(1.6 * Math.sin(((x - 16) / 3) * Math.PI));
  set(x, y, ...DARK);
}

// PNG 编码
function crc32(buf) {
  let c, table = [];
  for (let n = 0; n < 256; n++) {
    c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    table[n] = c >>> 0;
  }
  let crc = 0xffffffff;
  for (let i = 0; i < buf.length; i++) crc = table[(crc ^ buf[i]) & 0xff] ^ (crc >>> 8);
  return (crc ^ 0xffffffff) >>> 0;
}
function chunk(type, data) {
  const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
  const td = Buffer.concat([Buffer.from(type, 'ascii'), data]);
  const crc = Buffer.alloc(4); crc.writeUInt32BE(crc32(td));
  return Buffer.concat([len, td, crc]);
}
const ihdr = Buffer.alloc(13);
ihdr.writeUInt32BE(W, 0); ihdr.writeUInt32BE(H, 4);
ihdr[8] = 8; ihdr[9] = 6; // 8bit RGBA
const raw = Buffer.alloc(H * (W * 4 + 1));
for (let y = 0; y < H; y++) {
  raw[y * (W * 4 + 1)] = 0; // filter none
  px.copy(raw, y * (W * 4 + 1) + 1, y * W * 4, (y + 1) * W * 4);
}
const png = Buffer.concat([
  Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
  chunk('IHDR', ihdr),
  chunk('IDAT', zlib.deflateSync(raw)),
  chunk('IEND', Buffer.alloc(0)),
]);
const out = path.join(__dirname, 'tray-icon.png');
fs.writeFileSync(out, png);
console.log('tray icon written:', out, png.length, 'bytes');
