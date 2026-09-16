// 预加载脚本:安全地向渲染进程暴露少量能力
const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('petAPI', {
  showMain: () => ipcRenderer.invoke('window:show-main'),
  // 登录后把 token 交给主进程,用于日程/陪伴推送轮询(M5)
  setToken: (token) => ipcRenderer.invoke('auth:set-token', token),
  getToken: () => ipcRenderer.invoke('auth:get-token'),
  // 应用感知开关(M9,主进程内存标记)
  setAppMonitor: (on) => ipcRenderer.invoke('app-monitor:set', on),
  // 悬浮窗气泡(主进程推送聊天泡)
  onBubble: (cb) => ipcRenderer.on('pet:bubble', (e, text) => cb(text)),
  // 摄像头互动(M10):触发宠物转圈 / 接收转圈指令
  triggerSpin: () => ipcRenderer.invoke('pet:trigger-spin'),
  onSpin: (cb) => ipcRenderer.on('pet:spin', () => cb()),
  // 悬浮窗控制:隐藏 / 显示 / 右键菜单 / 调整大小
  hideFloating: () => ipcRenderer.invoke('window:hide-floating'),
  showFloating: () => ipcRenderer.invoke('window:show-floating'),
  resizeFloating: (w, h) => ipcRenderer.invoke('window:resize-floating', w, h),
  showPetMenu: () => ipcRenderer.invoke('pet:show-menu'),
  // 番茄钟:同步结束时间到主进程(宠物头顶倒计时 + 完成通知)
  setPomodoro: (endAt) => ipcRenderer.invoke('pomo:sync', endAt),
  onCountdown: (cb) => ipcRenderer.on('pet:countdown', (e, text) => cb(text)),
  onPomodoroComplete: (cb) => ipcRenderer.on('pet:pomodoro-complete', () => cb()),
  // 悬浮窗玩耍(右键菜单触发)
  onPlay: (cb) => ipcRenderer.on('pet:play', () => cb()),
  // 投喂(悬浮窗菜单)
  feedPet: () => ipcRenderer.invoke('pet:feed'),
  // 测试消息(管理员检查 UI;学习中静音)
  sendBubble: (text) => ipcRenderer.invoke('pet:send-bubble', text),
  // 宠物主动邀请(陪玩/学习)
  triggerInvite: () => ipcRenderer.invoke('pet:invite'),
  // 双账号测试:再开一个主控窗口
  openSecondWindow: () => ipcRenderer.invoke('window:open-second'),
  // 像素版前端(open design 生成)
  openPixel: () => ipcRenderer.invoke('window:open-pixel'),
  // 管理员后台(Vue 应用窗口)
  openAdmin: () => ipcRenderer.invoke('window:open-admin'),
  // 桌面整理(M8):扫描预览 / 执行移动 / 撤销
  scanDesktop: () => ipcRenderer.invoke('desktop:scan'),
  organizeDesktop: (plan) => ipcRenderer.invoke('desktop:organize', plan),
  undoDesktop: () => ipcRenderer.invoke('desktop:undo'),
  // 番茄钟完成通知(右下角弹窗+声音)
  notifyPomodoro: (title, body) => ipcRenderer.invoke('pomo:notify', title, body),
  // 语音交互:开始监听 / 停止监听 / 回调
  startVoiceListen: () => ipcRenderer.invoke('voice:start-listen'),
  stopVoiceListen: () => ipcRenderer.invoke('voice:stop-listen'),
  onVoiceResult: (cb) => ipcRenderer.on('voice:result', (e, text) => cb(text)),
  onVoiceError: (cb) => ipcRenderer.on('voice:error', (e, err) => cb(err)),
  // 背景音乐:导入本地文件
  importMusic: (filePath) => ipcRenderer.invoke('music:import', filePath),
  onMusicImported: (cb) => ipcRenderer.on('music:imported', (e, track) => cb(track)),
});
