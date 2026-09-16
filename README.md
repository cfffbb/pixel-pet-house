# 像素宠物小屋 (Pixel Pet House)

> 一个桌面 AI 宠物陪伴应用 —— 养一只像素小动物，陪你写代码、聊聊天、管日程。

桌面上有一只属于你的像素宠物。它会饿、会开心、会陪你聊天，番茄钟专注时它在一旁守着，日程到了它会提醒你。这不是一个冷冰冰的工具，而是一个有温度的小伙伴。

## 核心功能

| 模块 | 说明 |
|------|------|
| AI 对话 | 文字 / 语音双模式，支持 OpenAI 兼容接口（硅基流动、DeepSeek 等），AI 会以宠物人格回复 |
| 番茄钟 | 专注计时 + 任务管理，完成番茄可喂食宠物，边干活边养宠 |
| 日程提醒 | 创建日程，到点弹窗 + 桌面通知 + 宠物气泡提醒 |
| 宠物养成 | 饥饿值 / 心情值 / 体重 / 经验，喂食、互动、散步，宠物会随陪伴成长 |
| 繁育系统 | 宠物配对 → 怀孕 → 产仔，后代继承父母外观特征 |
| 好友系统 | 添加好友、互发消息、赠送礼物 |
| 商店 | 金币购买宠物食品、玩具、装饰品 |
| 桌面整理 | 一键将桌面文件归类到文件夹 |
| 摄像头互动 | MediaPipe 手势识别，用手势和宠物互动 |
| 相册 | 拍照记录宠物成长瞬间 |
| 宠物墓地 | 寿终正寝的宠物可以立碑纪念 |
| 主题切换 | 传统木屋 / 赛博木屋 / 魔法小屋 三套场景，分春夏秋冬白天夜晚 |
| 管理后台 | 用户管理、宠物管理、商品管理、API 配置、数据统计 |

## 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 运行时 |
| Spring Boot | 3.5.9 | Web 框架 |
| MyBatis-Plus | 3.5.12 | ORM + 分页 + 逻辑删除 |
| MySQL | 8.0 | 主数据库 |
| Hutool | 5.8.40 | 工具库（JSON、HTTP 等） |
| LangChain4j | 0.36.2 | AI 对话框架（OpenAI 兼容） |
| Spring Mail | - | 邮箱验证码注册 |
| Spring Security Crypto | - | BCrypt 密码加密（仅用加密模块） |
| H2 | - | 测试内存数据库 |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.5.x | UI 框架 |
| Vite | 6.0.x | 构建工具 |
| Electron | 33.x | 桌面打包 |
| ECharts | 5.5.x | 数据可视化（管理后台统计） |
| MediaPipe | 0.10.x | 摄像头手势识别 |
| Axios | 1.7.x | HTTP 请求 |

## 项目结构

```
pet/
├── pet-backend/                          # Spring Boot 后端
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd                   # Maven Wrapper（无需全局安装 Maven）
│   └── src/main/
│       ├── java/com/pet/
│       │   ├── PetApplication.java       # 启动类
│       │   ├── common/                   # 统一返回 Result<T>、异常处理
│       │   ├── config/                   # MyBatis-Plus 配置、CORS
│       │   ├── controller/               # 22 个控制器
│       │   ├── service/                  # 24 个服务
│       │   ├── entity/                   # 26 个实体
│       │   ├── mapper/                   # MyBatis 映射接口
│       │   ├── dto/                      # 数据传输对象
│       │   ├── security/                 # JWT 拦截器、AES-256 加密
│       │   └── job/                      # 定时推送任务
│       └── resources/
│           ├── application.yml           # 配置文件（密码已脱敏）
│           └── sql/                      # 建库脚本 + 迭代迁移脚本
│
├── pet-client/                           # Vue 3 + Electron 前端
│   ├── package.json
│   ├── vite.config.js
│   ├── electron/
│   │   ├── main.js                       # 主进程（窗口、托盘、快捷键、推送轮询）
│   │   └── preload.js                     # 预加载脚本
│   ├── src/
│   │   ├── App.vue
│   │   ├── main.js                       # Vue 入口
│   │   ├── api/http.js                   # Axios 封装
│   │   ├── components/                   # 17 个功能组件
│   │   │   ├── ChatPanel.vue             # AI 对话
│   │   │   ├── PomodoroPanel.vue         # 番茄钟
│   │   │   ├── SchedulePanel.vue         # 日程
│   │   │   ├── PetPanel.vue             # 宠物面板
│   │   │   ├── FeedingPanel.vue          # 喂食
│   │   │   ├── BackyardPanel.vue        # 后院（繁育）
│   │   │   ├── ShopPanel.vue             # 商店
│   │   │   ├── FriendPanel.vue           # 好友
│   │   │   ├── CameraPanel.vue          # 摄像头互动
│   │   │   ├── SettingPanel.vue          # 设置
│   │   │   ├── AdminPanel.vue            # 管理后台
│   │   │   └── ...
│   │   ├── views/
│   │   │   ├── LoginView.vue             # 登录/注册
│   │   │   ├── MainView.vue             # 主界面
│   │   │   └── AdminConsole.vue          # 管理控制台
│   │   ├── stores/                       # Pinia 状态管理
│   │   ├── pixelPet.js                   # 像素宠物动画引擎
│   │   ├── soundStore.js                 # 音效管理
│   │   └── themeStore.js                 # 主题切换
│   └── public/
│       ├── pets/                         # 26 种宠物像素图
│       ├── scenes/                       # 三套主题背景图
│       ├── sounds/                       # 音效文件
│       └── models/                       # MediaPipe 模型
│
├── docs/                                  # 项目文档（HTML 格式）
│   ├── SRS-需求规格说明书.html
│   ├── 系统设计文档.html
│   ├── 接口文档-V1.0.html
│   ├── 部署与用户手册.html
│   └── ...
│
├── 启动教程.md                             # 详细启动教程
├── 启动宠物助手.bat                        # 一键启动脚本
└── .gitignore
```

## 快速开始

### 环境要求

| 软件 | 版本 |
|------|------|
| JDK | 17+ |
| Node.js | 18+ |
| MySQL | 8.0 |
| Maven | 3.8+（或用项目自带 mvnw） |

### 第一步：初始化数据库

```sql
CREATE DATABASE pet_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pet_ai;
SOURCE pet-backend/src/main/resources/sql/schema.sql;
-- 依次执行迁移脚本
SOURCE pet-backend/src/main/resources/sql/migration_m3.sql;
SOURCE pet-backend/src/main/resources/sql/migration_m4.sql;
-- ...直到 migration_m13.sql
```

### 第二步：配置后端

编辑 `pet-backend/src/main/resources/application.yml`，将占位符替换为你自己的值：

```yaml
spring:
  datasource:
    password: <你的MySQL密码>        # ← 改成你的密码
  mail:
    username: your-email@qq.com      # ← 改成你的邮箱
    password: <你的邮箱授权码>        # ← QQ邮箱→设置→账户→SMTP授权码
    from: your-email@qq.com

pet:
  security:
    jwt-secret: <你的JWT密钥>        # ← 随便写一串长字符串
    aes-key: <你的AES密钥>            # ← 随便写一串字符串
```

### 第三步：启动后端

```powershell
cd pet-backend
.\mvnw.cmd spring-boot:run
```

看到 `Started PetApplication` 即启动成功。后端运行在 `http://localhost:8080`。

### 第四步：启动前端

```powershell
cd pet-client
npm install
npm run dev
```

Electron 窗口会自动弹出，显示登录界面。

### 第五步：注册 & 使用

1. 在登录界面点击「注册」，填写用户名、邮箱、密码
2. 输入邮箱收到的验证码完成注册
3. 登录后，点击底部「设置」配置你的 AI API Key（推荐 [硅基流动](https://siliconflow.cn)，免费额度多）
4. 开始养你的像素宠物

> 如需管理员权限，在 MySQL 中执行：
> ```sql
> UPDATE sys_user SET role = 'ADMIN' WHERE username = '你的用户名';
> ```

## 快捷键

| 快捷键 | 功能 |
|--------|------|
| `Ctrl+Shift+M` | 唤起 / 隐藏主窗口 |
| `Ctrl+Shift+P` | 唤起 / 隐藏悬浮宠物 |

## 接口规范

所有 API 遵循 RESTful 标准，统一返回格式：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

接口前缀：`/api/v1/xxx`

详细接口文档见 [docs/接口文档-V1.0.html](docs/接口文档-V1.0.html)。

## 打包安装版

```powershell
cd pet-client
npm run build
```

生成的 Windows 安装包在 `pet-client/release/` 目录下，双击 `.exe` 即可安装。

## UI 设计

界面采用暖色调设计：奶油色背景 + 橙色点缀，搭配像素风格宠物和手绘场景，营造温馨的陪伴感。

三套主题场景随心切换：
- **传统木屋** — 温暖的木质小屋
- **赛博木屋** — 赛博朋克风格
- **魔法小屋** — 童话魔法风

每套场景支持 春/夏/秋/冬 × 白天/夜晚 共 8 种背景。

## 开发相关

### 后端测试

```powershell
cd pet-backend
.\mvnw.cmd test
```

测试使用 H2 内存数据库，不依赖真实 MySQL。

### 前端开发

Vite 开发服务器运行在 `http://localhost:5173`，Electron 会自动加载。修改前端代码后自动热更新。

## 技术亮点

- **像素宠物动画引擎** (`pixelPet.js`)：纯 JS 实现的帧动画系统，支持宠物移动、表情、互动反馈
- **多主题系统**：三套场景 × 四季 × 昼夜 = 24 种背景组合，含灯开关、音效、表盘切换
- **AES-256 API Key 加密**：用户 API Key 在数据库中加密存储，前端仅显示首尾 4 位
- **语音对话链路**：前端录音 → WAV 转换 → 后端转发讯飞 ASR → AI 回复 → 讯飞 TTS → 前端播放 MP3
- **桌面整理**：Electron 主进程扫描桌面文件，按类型自动归类到文件夹
- **MediaPipe 手势识别**：摄像头捕捉手势，映射为宠物互动指令

## License

MIT
