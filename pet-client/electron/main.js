// Electron 主进程:主控窗口 + 悬浮宠物窗 + 系统托盘 + 日程/陪伴推送轮询(M5) + 桌面整理(M8)
const { app, BrowserWindow, Tray, Menu, nativeImage, ipcMain, Notification, session, globalShortcut } = require('electron');
const path = require('path');
const fs = require('fs');
const os = require('os');

const DEV_URL = 'http://localhost:5173';
const BASE_URL = 'http://localhost:8080'; // M4 拍板:写死本地后端地址
const isDev = true; // 强制开发模式,始终从 Vite 加载最新代码

// 悬浮窗渲染优化:
// 1. 默认开启硬件加速(透明窗口更流畅、更清晰、更少闪烁)
// 2. 如遇部分显卡崩溃,可将 ENABLE_HW_ACCEL 改为 false 回退到软件渲染
// 3. 关闭透明度抖动 + 启用帧调度,减少透明窗口重绘闪烁
const ENABLE_HW_ACCEL = true;
if (!ENABLE_HW_ACCEL) {
  app.disableHardwareAcceleration();
}
// 减少透明窗口重绘导致的闪烁:禁用 compositor 帧节流
app.commandLine.appendSwitch('disable-frame-rate-limit');
// 减少 GPU 进程崩溃后黑屏
app.commandLine.appendSwitch('disable-gpu-watchdog');
// 启用 DPI 感知(由系统自动处理,不强制 scale factor,避免高 DPI 模糊)
app.commandLine.appendSwitch('high-dpi-support', '1');

// 将 userData 设到项目目录内,避免沙箱环境(如 TRAE)拒绝访问 AppData
const userDataDir = path.join(__dirname, '../.userData');
try { fs.mkdirSync(userDataDir, { recursive: true }); } catch (_) { /* 忽略 */ }
app.setPath('userData', userDataDir);

// 单实例锁:崩溃残留进程会锁住磁盘缓存目录,新实例抢不到缓存直接崩;
// 加锁后同用户同时只跑一个实例,重复启动只唤出现有窗口
if (!app.requestSingleInstanceLock()) {
  app.quit();
} else {
  app.on('second-instance', () => {
    showMain();
  });
}

// 启动时清空磁盘缓存目录(每次崩溃留下的锁/脏缓存会被清除,避免"拒绝访问"崩溃)
function clearElectronCache() {
  try {
    const base = app.getPath('userData');
    for (const d of ['Cache', 'GPUCache', 'Code Cache', 'ShaderCache', 'DawnGraphiteCache', 'DawnWebGPUCache']) {
      fs.rmSync(path.join(base, d), { recursive: true, force: true });
    }
  } catch (_) {
    /* 忽略 */
  }
}

// 渲染进程崩溃不退出:自动重载窗口
app.on('web-contents-created', (_e, contents) => {
  contents.on('render-process-gone', () => {
    setTimeout(() => {
      try {
        contents.reload();
      } catch (_) {
        /* 忽略 */
      }
    }, 500);
  });
});

let mainWindow = null;
let floatingWindow = null;
let tray = null;
let authToken = null;

// ---------- 与后端交互(M5 推送轮询;M8 上报整理记录) ----------
async function api(path, method = 'GET', body) {
  if (!authToken) {
    console.log('[api] No authToken, cannot call', path);
    return null;
  }
  try {
    const opts = { method, headers: { Authorization: 'Bearer ' + authToken } };
    if (body !== undefined) {
      opts.headers['Content-Type'] = 'application/json';
      opts.body = JSON.stringify(body);
    }
    const resp = await fetch(BASE_URL + path, opts);
    const json = await resp.json();
    return json && json.code === 200 ? json.data : null;
  } catch (e) {
    console.error('[api] Error calling', path, e.message);
    return null;
  }
}

function showNotification(title, body) {
  if (!Notification.isSupported()) return;
  const n = new Notification({ title, body, silent: false });
  n.on('click', showMain);
}

// 日程到点提醒:每分钟轮询;10% 概率被宠物"卖萌替代"(M5 拍板)
async function checkScheduleReminders() {
  if (!authToken && mainWindow && !mainWindow.isDestroyed()) {
    try {
      const result = await mainWindow.webContents.executeJavaScript('localStorage.getItem("pet_token")');
      if (result) { authToken = result; console.log('[token] Recovered for schedule check'); }
    } catch (_) { /* ignore */ }
  }
  const due = await api('/api/schedule/remind');
  if (!due || !due.length) return;
  for (const item of due) {
    if (Math.random() < 0.1) {
      const naughty = await api('/api/push/naughty');
      showNotification('🐾 日程提醒(被宠物接管)', (naughty && naughty.text) || '喵?');
    } else {
      showNotification('📅 日程提醒', `主人,该去『${item.title}』啦!`);
    }
  }
}
setInterval(checkScheduleReminders, 60000);

// 随机陪伴推送:一天 2–3 次(客户端计数,M5 拍板);学习中静音
let companionCount = 0;
let companionDate = '';
setInterval(async () => {
  if (pomoActive()) return; // 学习中不打扰(M12)
  const today = new Date().toDateString();
  if (companionDate !== today) {
    companionDate = today;
    companionCount = 0;
  }
  const limit = 2 + Math.floor(Math.random() * 2); // 2–3 次
  if (companionCount >= limit) return;
  const c = await api('/api/push/companion');
  if (c && c.text) {
    showNotification('🐾 宠物找你', c.text);
    companionCount++;
  }
}, 30 * 60 * 1000);

// ---------- token 传递(渲染进程登录后写入主进程) ----------
let appMonitorEnabled = false;

async function refreshSettings() {
  const s = await api('/api/settings');
  if (s) {
    appMonitorEnabled = !!s.appMonitorEnabled;
  }
}

ipcMain.handle('auth:set-token', (e, token) => {
  authToken = token || null;
  if (authToken) refreshSettings();
});
ipcMain.handle('auth:get-token', () => authToken);
ipcMain.handle('app-monitor:set', (e, on) => {
  appMonitorEnabled = !!on;
});

// ---------- 应用感知采样(M9;默认关,隐私红线;仅进程名;保留 7 天) ----------
const APP_LINES = [
  { kw: ['idea', 'jetbrains', 'code', 'java'], line: '又在写代码?记得起来活动活动哦!' },
  { kw: ['chrome', 'edge', 'firefox'], line: '刷网页别太久,眼睛会累的~' },
  { kw: ['wechat', 'weixin', 'qq'], line: '和朋友聊天呢?别忘了今天的日程哦' },
  { kw: ['word', 'excel', 'wps', 'onenote'], line: '做文档辛苦了,喝口水吧' },
  { kw: ['steam', 'game'], line: '游戏好玩吗?别忘了一会儿要学习!' },
  { kw: ['music', 'spotify', 'cloudmusic'], line: '听歌呢?心情不错嘛' },
];
const APP_GENERIC = ['看到你认真做事的样子,我也要努力长大!', '我在这儿陪着你呢~', '累了就休息一下下吧'];

function sampleForegroundApps() {
  return new Promise((resolve) => {
    const { exec } = require('child_process');
    exec(
      'powershell -NoProfile -Command "Get-Process | Where-Object {$_.MainWindowHandle -ne 0} | Select-Object -ExpandProperty ProcessName"',
      { timeout: 10000 },
      (err, stdout) => {
        if (err) return resolve([]);
        resolve(
          String(stdout)
            .split('\n')
            .map((s) => s.trim())
            .filter(Boolean)
        );
      }
    );
  });
}

// 每 5 分钟采样一次;40% 概率发聊天泡(系统通知 + 悬浮窗气泡,M9 拍板)
setInterval(async () => {
  if (!appMonitorEnabled || !authToken) return;
  const apps = await sampleForegroundApps();
  if (!apps || !apps.length) return;
  const app = apps[0];
  await api('/api/app-usage/log', 'POST', { appName: app, windowTitle: null });
  if (Math.random() < 0.4) {
    const hit = APP_LINES.find((r) => r.kw.some((k) => app.toLowerCase().includes(k)));
    const text = hit ? hit.line : APP_GENERIC[Math.floor(Math.random() * APP_GENERIC.length)];
    showNotification('🐾 宠物找你', text);
    if (floatingWindow && !floatingWindow.isDestroyed()) {
      floatingWindow.webContents.send('pet:bubble', text);
    }
  }
}, 5 * 60 * 1000);

// ---------- 窗口 ----------
function loadPage(win, query) {
  const url = isDev
    ? DEV_URL + (query || '')
    : 'file://' + path.join(__dirname, '../dist/index.html').replace(/\\/g, '/') + (query || '');
  console.log('[loadPage] Loading URL:', url);
  win.loadURL(url);
}

/** 像素版主界面(Vue 之外的第二套 UI,open design 生成) */
function loadPixelPage(win) {
  const url = isDev
    ? DEV_URL + '/pixel-pet-app.html'
    : 'file://' + path.join(__dirname, '../dist/pixel-pet-app.html').replace(/\\/g, '/');
  win.loadURL(url);
}

function createMainWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 820,
    minWidth: 1000,
    minHeight: 700,
    title: '像素宠物小屋',
    icon: path.join(__dirname, '../build/icon.ico'),
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      webSecurity: false, // 允许渲染进程访问 file:// 本地音频文件
    },
  });
  // 主窗口 = Vue 应用(登录 + 全部功能面板);像素页保留为可选第二窗口
  loadPage(mainWindow, '');
  mainWindow.on('close', (e) => {
    if (!app.isQuitting) {
      e.preventDefault();
      mainWindow.hide();
    }
  });
}

function createFloatingWindow() {
  floatingWindow = new BrowserWindow({
    width: 180,
    height: 180,
    frame: false,
    transparent: true,
    alwaysOnTop: true,
    resizable: false,
    skipTaskbar: true,
    hasShadow: false,
    backgroundColor: '#00000000', // 完全透明背景,减少首次绘制闪烁
    show: false, // 先隐藏,等 ready-to-show 再显示,避免白屏
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      backgroundThrottling: false, // 悬浮窗在后台也正常渲染
      webSecurity: false, // 允许悬浮窗访问本地音频
    },
  });
  floatingWindow.setAlwaysOnTop(true, 'screen-saver');
  // 等页面渲染好再显示,避免透明窗口加载时的白框/闪烁
  floatingWindow.once('ready-to-show', () => {
    floatingWindow.show();
  });
  loadPage(floatingWindow, '?floating=true');
  // 悬浮窗加载完成后,如果有进行中的番茄钟,立即同步倒计时(避免刚打开时不显示)
  floatingWindow.webContents.once('did-finish-load', () => {
    if (pomoEndAt && pomoEndAt > Date.now()) {
      const remain = Math.max(0, pomoEndAt - Date.now());
      const m = String(Math.floor(remain / 60000)).padStart(2, '0');
      const s = String(Math.floor((remain % 60000) / 1000)).padStart(2, '0');
      floatingWindow.webContents.send('pet:countdown', m + ':' + s);
    }
  });
  floatingWindow.on('close', (e) => {
    if (!app.isQuitting) {
      e.preventDefault();
      floatingWindow.hide();
    }
  });
}

function createTray() {
  // 优先用生成的圆脸图标,缺失时退回 1x1 占位
  const iconPath = path.join(__dirname, 'tray-icon.png');
  let icon = fs.existsSync(iconPath)
    ? nativeImage.createFromPath(iconPath)
    : nativeImage
        .createFromDataURL('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==')
        .resize({ width: 16, height: 16 });
  tray = new Tray(icon);
  tray.setToolTip('像素宠物小屋');
  tray.setContextMenu(
    Menu.buildFromTemplate([
      { label: '打开主界面', click: showMain },
      { label: '打开完整功能版(全部功能)', click: openFullWindow },
      { label: '新开测试窗口(像素版)', click: openSecondWindow },
      { label: '显示/隐藏悬浮宠物', click: toggleFloating },
      { type: 'separator' },
      {
        label: '退出',
        click: () => {
          app.isQuitting = true;
          app.quit();
        },
      },
    ])
  );
  tray.on('click', showMain);
}

function showMain() {
  if (!mainWindow) {
    createMainWindow();
  }
  mainWindow.show();
  mainWindow.focus();
}

function toggleFloating() {
  if (!floatingWindow) {
    createFloatingWindow();
  }
  if (floatingWindow.isVisible()) {
    floatingWindow.hide();
  } else {
    floatingWindow.show();
  }
}

function openSecondWindow() {
  // 双账号测试:再开一个像素版窗口
  const win = new BrowserWindow({
    width: 1280,
    height: 820,
    title: '像素宠物小屋 · 测试窗口 2(像素版)',
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
    },
  });
  loadPixelPage(win);
  win.on('closed', () => {
    const idx = extraWindows.indexOf(win);
    if (idx >= 0) extraWindows.splice(idx, 1);
  });
  extraWindows.push(win);
}

// ---------- 像素版/管理员后台 窗口 ----------
// 主窗口现在就是像素版界面;openPixel 用于从 Vue 端调回主窗口
function openPixelWindow() {
  showMain();
}
ipcMain.handle('window:open-pixel', openPixelWindow);

// 管理员后台(Vue 应用窗口,ADMIN 登录可见控制面板)
let adminWindow = null;
function openAdminWindow() {
  if (adminWindow && !adminWindow.isDestroyed()) {
    adminWindow.show();
    adminWindow.focus();
    return;
  }
  adminWindow = new BrowserWindow({
    width: 1100,
    height: 760,
    title: '像素宠物小屋 · 管理员后台',
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
    },
  });
  loadPage(adminWindow, '');
  adminWindow.on('closed', () => {
    adminWindow = null;
  });
}
ipcMain.handle('window:open-admin', openAdminWindow);

// 完整功能版(Vue 应用:13 个页签 + 管理后台 + 全部功能)
let fullWindow = null;
function openFullWindow() {
  if (fullWindow && !fullWindow.isDestroyed()) {
    fullWindow.show();
    fullWindow.focus();
    return;
  }
  fullWindow = new BrowserWindow({
    width: 1280,
    height: 820,
    minWidth: 1000,
    minHeight: 700,
    title: '像素宠物小屋 · 完整功能版',
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
    },
  });
  loadPage(fullWindow, '');
  fullWindow.on('closed', () => {
    fullWindow = null;
  });
}
ipcMain.handle('window:open-full', openFullWindow);

ipcMain.handle('window:show-main', () => {
  showMain();
});

// ---------- 摄像头互动(M10:识别搓手后让悬浮窗宠物转圈) ----------
ipcMain.handle('pet:trigger-spin', () => {
  if (floatingWindow && !floatingWindow.isDestroyed()) {
    floatingWindow.webContents.send('pet:spin');
  }
  showNotification('🐾 宠物转圈啦', '它超开心的!');
});

// ---------- 悬浮窗控制(关闭/右键菜单,修"关不掉/没交互") ----------
ipcMain.handle('window:hide-floating', () => {
  if (floatingWindow && !floatingWindow.isDestroyed()) {
    floatingWindow.hide();
  }
});

ipcMain.handle('window:show-floating', () => {
  if (floatingWindow && !floatingWindow.isDestroyed()) {
    floatingWindow.show();
    floatingWindow.focus();
  }
});

ipcMain.handle('window:resize-floating', (e, w, h) => {
  if (floatingWindow && !floatingWindow.isDestroyed()) {
    const [curX, curY] = floatingWindow.getPosition();
    // 保持底部不动,向上扩展
    const newY = curY + (floatingWindow.getSize()[1] - h);
    floatingWindow.setSize(w, h);
    floatingWindow.setPosition(curX, Math.max(0, newY));
  }
});

ipcMain.handle('pet:show-menu', () => {
  if (!floatingWindow || floatingWindow.isDestroyed()) return;
  Menu.buildFromTemplate([
    { label: '🎮 玩耍', click: () => floatingWindow.webContents.send('pet:play') },
    { label: '🍖 喂食', click: feedPet },
    { label: '隐藏悬浮宠物', click: () => floatingWindow.hide() },
    { label: '打开主控窗口', click: showMain },
    { type: 'separator' },
    {
      label: '退出',
      click: () => {
        app.isQuitting = true;
        app.quit();
      },
    },
  ]).popup({ window: floatingWindow });
});

// ---------- 悬浮窗投喂(自动选背包第一种食物) ----------
async function feedPet() {
  const inv = await api('/api/shop/inventory');
  if (!inv || !inv.length) {
    showNotification('🍖 喂食', '背包里没有食物,先去商店买点吧');
    return;
  }
  const food = inv.find((i) => i.itemType === 'FOOD' && i.quantity > 0);
  if (!food) {
    showNotification('🍖 喂食', '背包里没有食物,先去商店买点吧');
    return;
  }
  const r = await api('/api/shop/feed', 'POST', { itemId: food.itemId });
  if (r) {
    showNotification('🍖 喂食', r.message);
    if (floatingWindow && !floatingWindow.isDestroyed()) {
      floatingWindow.webContents.send('pet:bubble', r.message);
    }
  } else {
    showNotification('🍖 喂食', '喂食失败');
  }
}
ipcMain.handle('pet:feed', feedPet);

// ---------- 番茄钟:悬浮宠物头顶倒计时 + 完成右下角提示 ----------
let pomoEndAt = null;
let pomoNotified = false; // 防止重复通知

function pomoActive() {
  return pomoEndAt != null && pomoEndAt > Date.now();
}

ipcMain.handle('pomo:sync', (e, endAt) => {
  pomoEndAt = endAt ? Number(endAt) : null;
  pomoNotified = false; // 重置通知标记
  // 放弃/完成后立即清掉悬浮窗倒计时(M13 bug 修复)
  if (!pomoEndAt && floatingWindow && !floatingWindow.isDestroyed()) {
    floatingWindow.webContents.send('pet:countdown', null);
  }
});

// 番茄钟完成:主动发系统通知 + 播放声音
ipcMain.handle('pomo:notify', (e, title, body) => {
  showNotification(title || '🍅 番茄钟完成', body || '专注时间结束!');
});

setInterval(() => {
  if (!pomoEndAt || !floatingWindow || floatingWindow.isDestroyed()) return;
  const remain = Math.max(0, pomoEndAt - Date.now());
  if (remain <= 0) {
    pomoEndAt = null;
    if (!pomoNotified) {
      pomoNotified = true;
      showNotification('🍅 时间到', '专注时间结束,回主控窗口点"完成"领奖励吧!');
    }
    floatingWindow.webContents.send('pet:countdown', '时间到!');
    // 触发宠物庆祝特效
    if (floatingWindow && !floatingWindow.isDestroyed()) {
      floatingWindow.webContents.send('pet:pomodoro-complete');
    }
    setTimeout(() => {
      if (!floatingWindow.isDestroyed()) floatingWindow.webContents.send('pet:countdown', null);
    }, 4000);
    return;
  }
  const m = String(Math.floor(remain / 60000)).padStart(2, '0');
  const s = String(Math.floor((remain % 60000) / 1000)).padStart(2, '0');
  floatingWindow.webContents.send('pet:countdown', m + ':' + s);
}, 1000);

// ---------- 测试/推送辅助(管理员检查 UI;学习中静音) ----------
ipcMain.handle('pet:send-bubble', (e, text) => {
  const t = text || '🐾 咕噜咕噜~';
  if (pomoActive()) {
    showNotification('🔕 学习中静音', '宠物在学习,消息暂不打扰(测试静音效果)');
    return;
  }
  if (floatingWindow && !floatingWindow.isDestroyed()) {
    floatingWindow.webContents.send('pet:bubble', t);
  }
  showNotification('🐾 宠物消息', t);
});

// ---------- 宠物主动发起(陪玩/学习邀请,M13) ----------
const INVITE_LINES = ['陪我玩一会儿嘛~', '该学习啦!一起番茄钟?', '我有点无聊,陪我坐坐?', '今天的日程完成了没?加油!'];
let inviteCount = 0;
let inviteDate = '';
function pushInvite() {
  if (!authToken || pomoActive()) return;
  const text = INVITE_LINES[Math.floor(Math.random() * INVITE_LINES.length)];
  if (floatingWindow && !floatingWindow.isDestroyed()) {
    floatingWindow.webContents.send('pet:bubble', text);
  }
  showNotification('🐾 宠物邀请', text);
  inviteCount++;
}
ipcMain.handle('pet:invite', () => {
  pushInvite();
});
// 每 45–90 分钟随机一次,一天最多 3 次
setInterval(() => {
  const today = new Date().toDateString();
  if (inviteDate !== today) {
    inviteDate = today;
    inviteCount = 0;
  }
  if (inviteCount >= 3 || !authToken) return;
  pushInvite();
}, 45 * 60 * 1000 + Math.floor(Math.random() * 45 * 60 * 1000));

// ---------- 第二个测试窗口(双账号交互测试) ----------
const extraWindows = [];
ipcMain.handle('window:open-second', () => {
  const win = new BrowserWindow({
    width: 960,
    height: 700,
    title: '像素宠物小屋 · 测试窗口 2',
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
    },
  });
  loadPage(win, '');
  win.on('closed', () => {
    const idx = extraWindows.indexOf(win);
    if (idx >= 0) extraWindows.splice(idx, 1);
  });
  extraWindows.push(win);
});

// ---------- 桌面整理(M8:文件扫描/移动由主进程做,规则与记录存后端) ----------
function desktopDir() {
  const home = os.homedir();
  // 检查 OneDrive 重定向(中文 Windows 常见)
  const candidates = [
    path.join(home, 'Desktop'),
    path.join(home, 'OneDrive', 'Desktop'),
    path.join(home, 'OneDrive', '桌面'),
  ];
  for (const dir of candidates) {
    if (fs.existsSync(dir)) return dir;
  }
  return path.join(home, 'Desktop');
}

// 从渲染进程恢复 token(通用辅助)
async function recoverTokenFromRenderer(event) {
  if (authToken) return;
  if (!event || !event.sender) return;
  try {
    const result = await event.sender.executeJavaScript('localStorage.getItem("pet_token")');
    if (result) {
      authToken = result;
      console.log('[token] Recovered from renderer');
    }
  } catch (_) { /* ignore */ }
}

ipcMain.handle('desktop:scan', async (event) => {
  await recoverTokenFromRenderer(event);

  let zones = await api('/api/desktop/zones');
  if (!zones || !zones.length) {
    // 内置默认分区规则，即使后端不可用也能用
    zones = [
      { zoneName: '文档', extensions: 'doc,docx,pdf,txt,md,xlsx,pptx,csv', targetDir: '文档', enabled: 1 },
      { zoneName: '图片', extensions: 'jpg,jpeg,png,gif,webp,bmp,svg', targetDir: '图片', enabled: 1 },
      { zoneName: '视频', extensions: 'mp4,avi,mkv,mov,wmv,flv', targetDir: '视频', enabled: 1 },
      { zoneName: '音频', extensions: 'mp3,wav,flac,aac,ogg', targetDir: '音频', enabled: 1 },
      { zoneName: '安装包', extensions: 'exe,msi,zip,rar,7z,iso', targetDir: '安装包', enabled: 1 },
      { zoneName: '其他', extensions: '', targetDir: '其他', enabled: 1 },
    ];
    console.log('[desktop:scan] Using built-in default zones');
  }
  const enabled = zones.filter((z) => z.enabled === 1);
  const dir = desktopDir();
  if (!fs.existsSync(dir)) return { zones, plan: [], error: '桌面目录不存在: ' + dir };
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  const zoneDirs = new Set(enabled.map((z) => z.targetDir));
  const plan = [];
  const skipFiles = new Set(['desktop.ini', 'thumbs.db', '.ds_store', 'ntuser.ini']);
  for (const e of entries) {
    if (e.isDirectory()) continue;
    if (zoneDirs.has(e.name)) continue;
    if (skipFiles.has(e.name.toLowerCase())) continue;
    const ext = path.extname(e.name).toLowerCase().replace('.', '');
    let target = enabled.find(
      (z) => z.extensions && z.extensions.split(',').map((s) => s.trim().toLowerCase()).includes(ext)
    );
    if (!target) target = enabled.find((z) => !z.extensions || z.extensions.trim() === '');
    if (!target) continue;
    plan.push({
      fileName: e.name,
      ext,
      zone: target.targetDir,
      from: path.join(dir, e.name),
      to: path.join(dir, target.targetDir, e.name),
    });
  }
  return { zones, plan };
});

ipcMain.handle('desktop:organize', async (e, plan) => {
  await recoverTokenFromRenderer(e);
  if (!plan || !plan.length) return { moved: 0, errors: ['没有可整理的文件'] };
  const dirs = new Set(plan.map((p) => path.dirname(p.to)));
  for (const d of dirs) { try { fs.mkdirSync(d, { recursive: true }); } catch (err) { /* ignore */ } }
  let moved = 0;
  const errors = [];
  for (const p of plan) {
    try {
      if (fs.existsSync(p.from) && !fs.existsSync(p.to)) {
        try {
          fs.renameSync(p.from, p.to);
        } catch (renameErr) {
          // EPERM/EXDEV: try copy+unlink, skip if file is locked
          try {
            fs.copyFileSync(p.from, p.to);
            fs.unlinkSync(p.from);
          } catch (copyErr) {
            if (copyErr.code === 'EPERM' || copyErr.code === 'EBUSY') {
              errors.push(p.fileName + ': 文件被占用,请关闭相关程序后重试');
            } else {
              throw copyErr;
            }
          }
        }
        moved++;
      } else if (fs.existsSync(p.to)) {
        // 目标已存在，加时间戳重命名
        const ts = '_' + Date.now();
        const ext = path.extname(p.to);
        const base = p.to.slice(0, -ext.length);
        const newTo = base + ts + ext;
        try {
          fs.renameSync(p.from, newTo);
        } catch (renameErr) {
          try {
            fs.copyFileSync(p.from, newTo);
            fs.unlinkSync(p.from);
          } catch (copyErr) {
            if (copyErr.code === 'EPERM' || copyErr.code === 'EBUSY') {
              errors.push(p.fileName + ': 文件被占用,请关闭相关程序后重试');
            } else {
              throw copyErr;
            }
          }
        }
        moved++;
      }
    } catch (err) {
      errors.push(p.fileName + ': ' + err.message);
    }
  }
  const details = plan.map((p) => ({ fileName: p.fileName, from: p.from, to: p.to }));
  try { await api('/api/desktop/organize/record', 'POST', { movedCount: moved, detailsJson: JSON.stringify(details) }); } catch (_) { /* 不影响整理结果 */ }
  return { moved, errors };
});

ipcMain.handle('desktop:undo', async (event) => {
  await recoverTokenFromRenderer(event);
  const latest = await api('/api/desktop/organize/latest');
  if (!latest || !latest.details || !latest.details.length) return { undone: 0 };
  let undone = 0;
  for (const d of latest.details) {
    try {
      if (fs.existsSync(d.to) && !fs.existsSync(d.from)) {
        fs.renameSync(d.to, d.from);
        undone++;
      }
    } catch (err) {
      console.error('undo failed:', d.fileName, err);
    }
  }
  await api('/api/desktop/organize/' + latest.id + '/undone', 'POST', {});
  return { undone };
});

// ---------- 背景音乐导入 ----------
const { dialog } = require('electron');

ipcMain.handle('music:import', async (event) => {
  try {
    const result = await dialog.showOpenDialog({
      title: '选择背景音乐文件',
      filters: [
        { name: '音频文件', extensions: ['mp3', 'wav', 'ogg', 'm4a', 'flac', 'aac'] },
      ],
      properties: ['openFile', 'multiSelections'],
    });
    if (result.canceled || !result.filePaths.length) {
      return { tracks: [] };
    }
    const tracks = result.filePaths.map((filePath) => {
      const name = path.basename(filePath);
      return {
        id: 'track_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8),
        name: name.replace(/\.[^/.]+$/, ''),
        url: 'file:///' + filePath.replace(/\\/g, '/'),
        source: 'local',
      };
    });
    // 通知渲染进程
    if (event && event.sender) {
      for (const t of tracks) {
        event.sender.send('music:imported', t);
      }
    }
    return { tracks };
  } catch (e) {
    return { tracks: [], error: e.message };
  }
});

// ---------- 语音交互(按键说话模式) ----------
// 利用渲染进程的 Web Speech API 或后端 STT
// 主进程负责协调:开始监听→渲染进程录音→发送后端转写→AI回复→TTS播放
let voiceListening = false;

ipcMain.handle('voice:start-listen', () => {
  voiceListening = true;
  if (floatingWindow && !floatingWindow.isDestroyed()) {
    floatingWindow.webContents.send('voice:listening', true);
  }
});

ipcMain.handle('voice:stop-listen', () => {
  voiceListening = false;
  if (floatingWindow && !floatingWindow.isDestroyed()) {
    floatingWindow.webContents.send('voice:listening', false);
  }
});

app.whenReady().then(async () => {
  // 放行摄像头等媒体权限:Electron 默认拒绝 getUserMedia,不设这句摄像头打不开
  session.defaultSession.setPermissionRequestHandler((webContents, permission, callback) => {
    const allowed = ['media', 'notifications', 'clipboard-read', 'clipboard-sanitized-write'];
    callback(allowed.includes(permission));
  });
  clearElectronCache();
  // 清除 HTTP session 缓存,确保开发模式加载最新代码
  await session.defaultSession.clearCache();
  await session.defaultSession.clearStorageData({ storages: ['cachestorage', 'serviceworkers'] });
  createMainWindow();
  createFloatingWindow();
  createTray();

  // 主窗口加载完成后,延迟 5 秒做首次日程提醒检查(等渲染进程设好 token)
  if (mainWindow) {
    mainWindow.webContents.once('did-finish-load', () => {
      setTimeout(checkScheduleReminders, 5000);
    });
  }

  // 全局快捷键:Ctrl+Shift+P 唤起/隐藏宠物悬浮窗
  globalShortcut.register('CommandOrControl+Shift+P', () => {
    if (floatingWindow && !floatingWindow.isDestroyed()) {
      if (floatingWindow.isVisible()) {
        floatingWindow.hide();
      } else {
        floatingWindow.show();
        floatingWindow.setAlwaysOnTop(true);
      }
    } else {
      createFloatingWindow();
      if (floatingWindow) floatingWindow.show();
    }
  });

  // Ctrl+Shift+M 显示/隐藏主窗口
  globalShortcut.register('CommandOrControl+Shift+M', () => {
    if (mainWindow && !mainWindow.isDestroyed()) {
      if (mainWindow.isVisible() && mainWindow.isFocused()) {
        mainWindow.hide();
      } else {
        mainWindow.show();
        mainWindow.focus();
      }
    } else {
      createMainWindow();
      if (mainWindow) { mainWindow.show(); mainWindow.focus(); }
    }
  });
});

app.on('window-all-closed', () => {
  if (app.isQuitting) {
    app.quit();
  } else {
    // 不退出,只隐藏(宠物悬浮窗继续工作)
    if (mainWindow) mainWindow.hide();
  }
});

// 退出时注销全局快捷键
app.on('will-quit', () => {
  globalShortcut.unregisterAll();
});
