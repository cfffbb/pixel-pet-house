<template>
  <div v-if="isAdmin">
    <!-- Tab 切换 -->
    <div class="admin-tabs">
      <button class="chip" :class="{ active: tab === 'dashboard' }" @click="tab = 'dashboard'; loadStats()">📊 数据总览</button>
      <button class="chip" :class="{ active: tab === 'users' }" @click="tab = 'users'">🛡️ 用户管理</button>
      <button class="chip" :class="{ active: tab === 'api' }" @click="tab = 'api'; loadApiConfig()">🔑 API 配置</button>
      <button class="chip" :class="{ active: tab === 'bubbles' }" @click="tab = 'bubbles'; loadBubbles()">💬 气泡库</button>
      <button class="chip" :class="{ active: tab === 'broadcast' }" @click="tab = 'broadcast'">📢 群发通知</button>
      <div class="tab-spacer"></div>
      <button class="chip chip-ghost" :class="{ active: tab === 'test' }" @click="tab = 'test'">🧪 测试</button>
    </div>

    <!-- 数据总览 -->
    <div v-if="tab === 'dashboard'">
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon">👥</div>
          <div class="stat-num">{{ stats.userCount || 0 }}</div>
          <div class="stat-label">总用户</div>
          <div class="stat-sub">活跃 {{ stats.activeUserCount || 0 }} · 禁用 {{ stats.disabledUserCount || 0 }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">🐾</div>
          <div class="stat-num">{{ stats.petCount || 0 }}</div>
          <div class="stat-label">总宠物</div>
          <div class="stat-sub">存活 {{ stats.alivePetCount || 0 }} · 死亡 {{ stats.deadPetCount || 0 }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">💬</div>
          <div class="stat-num">{{ stats.chatCount || 0 }}</div>
          <div class="stat-label">对话消息</div>
          <div class="stat-sub">AI 交互记录</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">🍅</div>
          <div class="stat-num">{{ stats.pomodoroCount || 0 }}</div>
          <div class="stat-label">番茄钟记录</div>
          <div class="stat-sub">已完成 {{ stats.completedPomodoroCount || 0 }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">📅</div>
          <div class="stat-num">{{ stats.scheduleCount || 0 }}</div>
          <div class="stat-label">日程数量</div>
          <div class="stat-sub">用户计划任务</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">🪦</div>
          <div class="stat-num">{{ stats.gravestoneCount || 0 }}</div>
          <div class="stat-label">墓地记录</div>
          <div class="stat-sub">已安息的宠物</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">🚩</div>
          <div class="stat-num">{{ stats.pendingReports || 0 }}</div>
          <div class="stat-label">待处理举报</div>
          <div class="stat-sub" :class="{ error: stats.pendingReports > 0 }">{{ stats.pendingReports > 0 ? '需及时处理' : '无待处理' }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">🔑</div>
          <div class="stat-num">{{ stats.apiConfigCount || 0 }}</div>
          <div class="stat-label">API 配置</div>
          <div class="stat-sub">全局接口配置数</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">💭</div>
          <div class="stat-num">{{ stats.bubbleCount || 0 }}</div>
          <div class="stat-label">气泡提示</div>
          <div class="stat-sub">宠物对话文本</div>
        </div>
      </div>
      <div class="card" style="margin-top:12px">
        <button class="pix-btn btn-soft btn-sm" @click="loadStats">🔄 刷新数据</button>
        <span class="muted small" style="margin-left:8px" v-if="statsLoadedAt">最后刷新:{{ statsLoadedAt }}</span>
      </div>
    </div>

    <!-- 用户管理 -->
    <div class="card" v-if="tab === 'users'">
      <h3>🛡️ 用户管理</h3>
      <div class="row">
        <input v-model="keyword" placeholder="按用户名/昵称搜索" @keyup.enter="load" style="max-width: 220px" />
        <button class="pix-btn btn-ghost btn-sm" @click="load">搜索</button>
      </div>
      <table>
        <thead>
          <tr>
            <th>用户名</th>
            <th>昵称</th>
            <th>角色</th>
            <th>状态</th>
            <th>API Key</th>
            <th>注册时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id">
            <td>{{ u.username }}</td>
            <td>{{ u.nickname }}</td>
            <td>{{ u.role === 'ADMIN' ? '管理员' : '用户' }}</td>
            <td :class="u.status === 1 ? 'ok' : 'error'">{{ u.status === 1 ? '启用' : '禁用' }}</td>
            <td>{{ u.hasApiKey ? '✅' : '—' }}</td>
            <td class="muted">{{ fmt(u.createdAt) }}</td>
            <td>
              <button class="pix-btn btn-ghost btn-sm" @click="toggleStatus(u)">{{ u.status === 1 ? '禁用' : '启用' }}</button>
              <button class="pix-btn btn-ghost btn-sm" @click="showKeys(u)">看 Key</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="row">
        <button class="pix-btn btn-ghost btn-sm" @click="prev" :disabled="page <= 1">上一页</button>
        <span class="muted">第 {{ page }} 页</span>
        <button class="pix-btn btn-ghost btn-sm" @click="next" :disabled="users.length < size">下一页</button>
      </div>
      <p v-if="msg" class="ok">{{ msg }}</p>
      <p v-if="err" class="error">{{ err }}</p>

      <div v-if="keyList.length" class="card" style="margin-top:10px">
        <h4>{{ keyOwner }} 的 Key 配置</h4>
        <div v-for="k in keyList" :key="k.provider" class="muted">{{ k.provider }} · {{ k.enabled === 1 ? '启用' : '停用' }}</div>
        <button class="pix-btn btn-ghost btn-sm" @click="keyList = []">关闭</button>
      </div>
    </div>

    <!-- API 配置 -->
    <div class="card" v-if="tab === 'api'">
      <h3>🔑 全局 API 配置(统一应用到所有用户)</h3>
      <p class="muted">配置三类 API:文本对话 / 语音TTS / 图片识别。用户可选择使用管理员配置或自行配置。</p>

      <!-- 已配置列表 -->
      <div v-for="c in apiConfigs" :key="c.id" class="api-config-row">
        <div class="api-config-info">
          <span class="api-type-tag" :class="'type-' + c.apiType">{{ apiTypeLabel(c.apiType) }}</span>
          <b>{{ c.provider }}</b>
          <span class="muted">{{ c.modelName || '默认模型' }}</span>
          <span v-if="c.voice" class="muted">音色:{{ c.voice }}</span>
          <span class="muted">{{ c.keyMasked }}</span>
          <span :class="c.enabled ? 'ok' : 'error'">{{ c.enabled ? '启用' : '停用' }}</span>
        </div>
        <button class="pix-btn btn-ghost btn-sm" @click="editApiConfig(c)">编辑</button>
        <button class="pix-btn btn-ghost btn-sm" @click="deleteApiConfig(c.id)">删除</button>
      </div>
      <p v-if="!apiConfigs.length" class="muted">还没有配置任何 API。下方添加。</p>

      <!-- 新增/编辑表单 -->
      <div class="api-form" v-if="showApiForm">
        <h4>{{ editingApiId ? '编辑 API 配置' : '新增 API 配置' }}</h4>
        <div class="field">
          <label>API 类型</label>
          <select v-model="apiForm.apiType" :disabled="editingApiId">
            <option value="text">文本对话(chat/completions)</option>
            <option value="voice">语音合成(audio/speech TTS)</option>
            <option value="stt">语音转写(audio/transcriptions STT)</option>
            <option value="image">图片识别(vision)</option>
          </select>
        </div>
        <div class="field">
          <label>服务商</label>
          <select v-model="apiForm.provider" @change="onApiProviderChange">
            <option value="openai">OpenAI</option>
            <option value="qwen">通义千问(Qwen)</option>
            <option value="siliconflow">硅基流动(SiliconFlow)</option>
            <option value="zhipu">智谱(GLM)</option>
            <option value="xfyun" v-if="apiForm.apiType === 'voice' || apiForm.apiType === 'stt'">讯飞语音(Xfyun)</option>
            <option value="custom">自定义</option>
          </select>
        </div>
        <div class="field" v-if="apiForm.provider === 'xfyun'">
          <label>讯飞 AppID{{ editingApiId ? '(留空=不修改)' : '(必填)' }}</label>
          <input v-model="apiForm.appId" type="text" placeholder="讯飞控制台获取的 AppID" />
        </div>
        <div class="field">
          <label>接口地址{{ apiForm.provider === 'custom' ? '(必填)' : apiForm.provider === 'xfyun' ? '(讯飞专用,一般不改)' : '(一般不用改)' }}</label>
          <input v-model="apiForm.baseUrl" :placeholder="apiPlaceholder.baseUrl" />
        </div>
        <div class="field">
          <label>API Key{{ editingApiId ? '(留空=不修改)' : '(必填)' }}</label>
          <input v-model="apiForm.apiKey" type="password" :placeholder="apiPlaceholder.key" />
        </div>
        <div class="field" v-if="apiForm.provider === 'xfyun'">
          <label>讯飞 APISecret{{ editingApiId ? '(留空=不修改)' : '(必填)' }}</label>
          <input v-model="apiForm.apiSecret" type="password" placeholder="讯飞控制台获取的 APISecret" />
        </div>
        <div class="field" v-if="apiForm.provider !== 'xfyun'">
          <label>模型名</label>
          <input v-model="apiForm.modelName" :placeholder="apiPlaceholder.model" />
        </div>
        <div class="field" v-if="apiForm.apiType === 'voice'">
          <label>TTS 音色</label>
          <select v-model="apiForm.voice">
            <option value="alloy" v-if="apiForm.provider !== 'xfyun'">Alloy(中性)</option>
            <option value="echo" v-if="apiForm.provider !== 'xfyun'">Echo(男声)</option>
            <option value="fable" v-if="apiForm.provider !== 'xfyun'">Fable(叙事)</option>
            <option value="onyx" v-if="apiForm.provider !== 'xfyun'">Onyx(深沉)</option>
            <option value="nova" v-if="apiForm.provider !== 'xfyun'">Nova(女声)</option>
            <option value="shimmer" v-if="apiForm.provider !== 'xfyun'">Shimmer(清亮)</option>
            <option value="xiaoyan" v-if="apiForm.provider === 'xfyun'">小燕(女声,普通话)</option>
            <option value="xiaofeng" v-if="apiForm.provider === 'xfyun'">小峰(男声,普通话)</option>
            <option value="xiaoqi" v-if="apiForm.provider === 'xfyun'">小琪(女声,甜美女声)</option>
            <option value="xiaomei" v-if="apiForm.provider === 'xfyun'">小眉(女声,粤语)</option>
            <option value="xiaolin" v-if="apiForm.provider === 'xfyun'">小琳(女声,台湾腔)</option>
            <option value="xiaorou" v-if="apiForm.provider === 'xfyun'">小蓉(女声,四川话)</option>
            <option value="xiaoyun" v-if="apiForm.provider === 'xfyun'">小芸(女声,东北话)</option>
            <option value="xiaokan" v-if="apiForm.provider === 'xfyun'">小燕(女声,英语)</option>
          </select>
        </div>
        <div class="field">
          <label>备注(可选)</label>
          <input v-model="apiForm.remark" placeholder="如:主对话 Key" />
        </div>
        <div class="api-form-btns">
          <button class="pix-btn btn-primary btn-sm" @click="saveApiConfig">{{ editingApiId ? '保存修改' : '添加' }}</button>
          <button class="pix-btn btn-ghost btn-sm" @click="showApiForm = false">取消</button>
        </div>
      </div>
      <button v-else class="pix-btn btn-primary btn-sm" @click="openApiForm()">+ 新增 API 配置</button>
      <p v-if="apiMsg" class="ok">{{ apiMsg }}</p>
      <p v-if="apiErr" class="error">{{ apiErr }}</p>
    </div>

    <!-- 气泡提示库 -->
    <div class="card" v-if="tab === 'bubbles'">
      <h3>💬 气泡提示库管理</h3>
      <p class="muted">按种类/性格/性别/阶段组合,设置多样化气泡。留空=通用匹配。</p>

      <div class="bubble-form">
        <div class="bubble-form-row">
          <select v-model="bubbleForm.species">
            <option value="">种类:通用</option>
            <option value="CAT">猫</option>
            <option value="DOG">狗</option>
            <option value="RABBIT">兔</option>
            <option value="RAT">鼠</option>
            <option value="TIGER">虎</option>
            <option value="DRAGON">龙</option>
          </select>
          <select v-model="bubbleForm.personality">
            <option value="">性格:通用</option>
            <option value="活泼">活泼</option>
            <option value="温柔">温柔</option>
            <option value="傲娇">傲娇</option>
            <option value="胆小">胆小</option>
            <option value="忠诚">忠诚</option>
          </select>
          <select v-model="bubbleForm.gender">
            <option value="">性别:通用</option>
            <option value="MALE">公</option>
            <option value="FEMALE">母</option>
          </select>
          <select v-model="bubbleForm.category">
            <option value="random">随机</option>
            <option value="greeting">问候</option>
            <option value="happy">开心</option>
            <option value="sad">难过</option>
            <option value="hungry">饥饿</option>
            <option value="sleepy">困倦</option>
            <option value="study">学习</option>
            <option value="play">玩耍</option>
          </select>
        </div>
        <div class="bubble-form-row">
          <input v-model="bubbleForm.content" placeholder="输入气泡文本..." style="flex:1" />
          <button class="pix-btn btn-primary btn-sm" @click="addBubble">添加</button>
        </div>
      </div>

      <table class="bubble-table">
        <thead>
          <tr>
            <th>种类</th><th>性格</th><th>性别</th><th>类别</th><th>内容</th><th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="b in bubbles" :key="b.id">
            <td>{{ b.species || '通用' }}</td>
            <td>{{ b.personality || '通用' }}</td>
            <td>{{ b.gender || '通用' }}</td>
            <td>{{ b.category }}</td>
            <td>{{ b.content }}</td>
            <td><button class="pix-btn btn-ghost btn-sm" @click="deleteBubble(b.id)">删除</button></td>
          </tr>
        </tbody>
      </table>
      <p v-if="!bubbles.length" class="muted">没有气泡数据。</p>
    </div>

    <!-- 举报管理 -->
    <div class="card" v-if="tab === 'reports'">
      <h3>🚩 举报管理</h3>
      <p class="muted">用户举报记录,可拉黑被举报者或驳回举报。</p>
      <table v-if="reports.length">
        <thead>
          <tr>
            <th>举报人</th>
            <th>被举报人</th>
            <th>原因</th>
            <th>关联消息</th>
            <th>状态</th>
            <th>时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in reports" :key="r.id">
            <td>{{ r.reporterName || '?' }}</td>
            <td>{{ r.targetName || '?' }}</td>
            <td>{{ r.content }}</td>
            <td class="muted small">{{ r.messageContent ? (r.messageContent.length > 20 ? r.messageContent.slice(0,20) + '...' : r.messageContent) : '—' }}</td>
            <td>
              <span class="report-status" :class="'rs-' + (r.status || '').toLowerCase()">{{ reportStatusLabel(r.status) }}</span>
            </td>
            <td class="muted">{{ fmt(r.createdAt) }}</td>
            <td v-if="r.status === 'PENDING'">
              <button class="pix-btn btn-danger btn-sm" @click="resolveReport(r.id, 'BLOCK')">拉黑</button>
              <button class="pix-btn btn-ghost btn-sm" @click="resolveReport(r.id, 'DISMISS')">驳回</button>
            </td>
            <td v-else class="muted">已处理</td>
          </tr>
        </tbody>
      </table>
      <p v-if="!reports.length" class="muted">暂无举报记录。</p>
      <p v-if="reportMsg" class="ok">{{ reportMsg }}</p>
      <p v-if="reportErr" class="error">{{ reportErr }}</p>
    </div>

    <!-- 音乐管理 -->
    <div class="card" v-if="tab === 'music'">
      <h3>🎵 背景音乐管理</h3>
      <p class="muted">导入本地音乐文件作为背景音乐,所有用户共享此播放列表。支持 MP3、WAV、OGG 格式。</p>
      
      <!-- 上传区域 -->
      <div class="music-upload-area">
        <div class="upload-box" @click="triggerMusicUpload" @dragover.prevent @drop.prevent="onMusicDrop">
          <div class="upload-icon">📁</div>
          <div class="upload-text">点击或拖拽音乐文件到此处</div>
          <div class="upload-hint">支持 MP3 / WAV / OGG,单文件建议不超过 10MB</div>
        </div>
        <input ref="musicFileInput" type="file" accept="audio/*" multiple @change="onMusicFilesSelected" style="display:none" />
      </div>

      <div class="row" style="gap: 10px; margin: 12px 0">
        <button class="pix-btn btn-primary btn-sm" @click="triggerMusicUpload">
          ➕ 导入音乐
        </button>
        <button class="pix-btn btn-ghost btn-sm" @click="clearAllMusic" :disabled="!adminPlaylist.length">
          🗑️ 清空列表
        </button>
      </div>

      <!-- 播放列表 -->
      <div class="admin-music-list">
        <div class="music-list-header">
          <span class="music-list-title">📋 播放列表 ({{ adminPlaylist.length }} 首)</span>
        </div>
        <div v-if="adminPlaylist.length" class="music-list-items">
          <div
            v-for="(track, idx) in adminPlaylist"
            :key="track.id"
            class="music-list-item"
          >
            <span class="music-idx">{{ idx + 1 }}</span>
            <span class="music-name">{{ track.name }}</span>
            <span class="music-source">{{ track.source === 'default' ? '默认' : '已导入' }}</span>
            <div class="music-actions">
              <button class="pix-btn btn-soft btn-xs" @click="playAdminTrack(idx)">▶️ 试听</button>
              <button class="pix-btn btn-ghost btn-xs" @click="removeAdminTrack(idx)" :disabled="track.source === 'default' && adminPlaylist.length <= 1">
                删除
              </button>
            </div>
          </div>
        </div>
        <p v-else class="muted" style="text-align:center;padding:20px">
          还没有导入音乐,点击上方按钮添加
        </p>
      </div>

      <p v-if="musicMsg" class="ok">{{ musicMsg }}</p>
      <p v-if="musicErr" class="error">{{ musicErr }}</p>

      <div class="note" style="margin-top: 12px">
        <strong>💡 提示:</strong><br>
        · 导入的音乐在当前会话中有效,重启应用后需要重新导入(后续版本将支持持久化)<br>
        · 默认的「背景音乐」对应 public/sounds/bg-music.mp3 文件<br>
        · 如音乐文件较大,建议使用 MP3 格式以节省内存
      </div>
    </div>

    <!-- 群发通知 -->
    <div class="card" v-if="tab === 'broadcast'">
      <h3>📢 群发通知</h3>
      <p class="muted">向所有活跃用户发送官方通知消息,用户会在社交列表的「官方助手」对话中收到。</p>
      <div class="row" style="margin: 10px 0">
        <textarea v-model="broadcastText" rows="4" placeholder="输入通知内容,如:【更新公告】v2.0 新增社交模块..." style="flex:1; padding:10px; border:2px solid #f0e0cc; border-radius:10px; font-size:14px; background:#fffdf8; resize:vertical"></textarea>
      </div>
      <div class="row" style="gap: 10px">
        <button class="pix-btn btn-primary btn-sm" @click="sendBroadcast" :disabled="!broadcastText.trim() || broadcasting">
          {{ broadcasting ? '发送中...' : '📤 群发通知' }}
        </button>
        <button class="pix-btn btn-ghost btn-sm" @click="sendTestBroadcast" :disabled="broadcasting">
          🧪 测试(仅发给自己)
        </button>
      </div>
      <p v-if="broadcastMsg" class="ok">{{ broadcastMsg }}</p>
      <p v-if="broadcastErr" class="error">{{ broadcastErr }}</p>
    </div>

    <!-- 系统测试 -->
    <div class="card" v-if="tab === 'test'">
      <h3>🧪 系统总测试</h3>
      <p class="muted">一键检测各核心模块接口连通性。点击下方按钮开始测试。</p>
      <button class="pix-btn btn-primary btn-sm" @click="runTests" :disabled="testing">
        {{ testing ? '测试中...' : '▶️ 开始测试' }}
      </button>
      <div class="test-results" v-if="testResults.length">
        <div v-for="t in testResults" :key="t.name" class="test-item" :class="t.pass ? 'pass' : 'fail'">
          <span class="test-icon">{{ t.pass ? '✅' : '❌' }}</span>
          <span class="test-name">{{ t.name }}</span>
          <span class="test-detail">{{ t.detail }}</span>
        </div>
        <div class="test-summary">
          通过 {{ testResults.filter(t => t.pass).length }} / {{ testResults.length }} 项
          <span :class="testResults.every(t => t.pass) ? 'ok' : 'error'">
            {{ testResults.every(t => t.pass) ? '🎉 全部通过' : '⚠️ 存在失败项' }}
          </span>
        </div>
      </div>
    </div>

    <!-- 背景季节测试(管理员) -->
    <div class="card" v-if="tab === 'test'">
      <h3>🎨 背景季节测试</h3>
      <p class="muted">手动切换春夏秋冬背景图,用于测试不同季节的主题效果。选择「自动」恢复根据月份判断。</p>
      <div class="season-test-row">
        <button class="pix-btn btn-ghost btn-sm season-btn" :class="{ active: !seasonOverride }" @click="setSeason(null)">自动</button>
        <button class="pix-btn btn-ghost btn-sm season-btn" :class="{ active: seasonOverride === 'spring' }" @click="setSeason('spring')">🌸 春</button>
        <button class="pix-btn btn-ghost btn-sm season-btn" :class="{ active: seasonOverride === 'summer' }" @click="setSeason('summer')">☀️ 夏</button>
        <button class="pix-btn btn-ghost btn-sm season-btn" :class="{ active: seasonOverride === 'autumn' }" @click="setSeason('autumn')">🍂 秋</button>
        <button class="pix-btn btn-ghost btn-sm season-btn" :class="{ active: seasonOverride === 'winter' }" @click="setSeason('winter')">❄️ 冬</button>
      </div>
      <p class="muted small" style="margin-top:8px">当前: <b>{{ seasonOverride ? seasonOverride : '自动(' + currentSeasonName + ')' }}</b> · 切换后回到主页查看背景变化</p>
    </div>

    <!-- 管理员换宠测试 -->
    <div class="card" v-if="tab === 'test'">
      <h3>🐾 快速换宠(测试用)</h3>
      <p class="muted">一键切换自己的宠物种类和性别,方便测试不同宠物的 UI 效果。旧宠物会标为已死。</p>
      <div class="row" style="gap: 8px; margin: 10px 0; flex-wrap: wrap">
        <select v-model="switchPetForm.typeCode">
          <option v-for="t in petTypes" :key="t.typeCode" :value="t.typeCode">{{ t.typeName }}</option>
        </select>
        <select v-model="switchPetForm.gender">
          <option value="MALE">公</option>
          <option value="FEMALE">母</option>
        </select>
        <select v-model="switchPetForm.personality">
          <option value="">随机性格</option>
          <option value="活泼">活泼</option>
          <option value="温柔">温柔</option>
          <option value="傲娇">傲娇</option>
          <option value="胆小">胆小</option>
          <option value="忠诚">忠诚</option>
        </select>
        <button class="pix-btn btn-primary btn-sm" @click="doSwitchPet" :disabled="switching">
          {{ switching ? '切换中...' : '🔄 切换宠物' }}
        </button>
      </div>
      <p v-if="switchMsg" class="ok">{{ switchMsg }}</p>
      <p v-if="switchErr" class="error">{{ switchErr }}</p>
    </div>

    <!-- 繁育调试 -->
    <div class="card" v-if="tab === 'test'">
      <h3>🐣 繁育调试(测试用)</h3>
      <p class="muted">配种周期已缩短为1天。如果不想等,点下面按钮可以立即完成怀孕→出生,或清除配种冷却。</p>
      <div class="row" style="gap: 8px; margin: 10px 0">
        <button class="pix-btn btn-primary btn-sm" @click="debugFinishBreeding" :disabled="breedingDebug">
          {{ breedingDebug ? '执行中...' : '⚡ 立即完成繁育' }}
        </button>
      </div>
      <p v-if="breedMsg" class="ok">{{ breedMsg }}</p>
      <p v-if="breedErr" class="error">{{ breedErr }}</p>
    </div>
  </div>

  <div v-else class="card">
    <p class="error">只有管理员账号可以使用此页面。</p>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import http from '../api/http';
import { useSound } from '../soundStore';
import { useTheme } from '../themeStore';

const { playSound } = useSound();
const { seasonOverride, setSeasonOverride, currentSeason } = useTheme();

const props = defineProps({ user: Object });
const isAdmin = computed(() => props.user && props.user.role === 'ADMIN');

const seasonNames = { spring: '春', summer: '夏', autumn: '秋', winter: '冬' };
const currentSeasonName = computed(() => seasonNames[currentSeason.value] || currentSeason.value);
function setSeason(s) {
  playSound('button');
  setSeasonOverride(s);
}

const tab = ref('dashboard');
const users = ref([]);
const keyword = ref('');
const page = ref(1);
const size = 10;
const msg = ref('');
const err = ref('');
const keyList = ref([]);
const keyOwner = ref('');

// 数据总览
const stats = ref({});
const statsLoadedAt = ref('');

// 举报管理
const reports = ref([]);
const reportMsg = ref('');
const reportErr = ref('');

// 群发通知
const broadcastText = ref('');
const broadcasting = ref(false);
const broadcastMsg = ref('');
const broadcastErr = ref('');

// 系统测试
const testing = ref(false);
const testResults = ref([]);

// 换宠测试
const petTypes = ref([]);
const switchPetForm = ref({ typeCode: 'CAT', gender: 'MALE', personality: '' });
const switching = ref(false);
const switchMsg = ref('');
const switchErr = ref('');

async function doSwitchPet() {
  playSound('button');
  switching.value = true;
  switchMsg.value = '';
  switchErr.value = '';
  try {
    const data = await http.post('/api/admin/switch-pet', { ...switchPetForm.value });
    switchMsg.value = '切换成功!新宠物: ' + (data.petName || '未知') + ' (' + (data.typeCode || switchPetForm.value.typeCode) + ')';
    setTimeout(() => { switchMsg.value = ''; }, 4000);
    // 刷新宠物数据
    if (window.petAPI && window.petAPI.refreshPet) window.petAPI.refreshPet();
  } catch (e) {
    switchErr.value = e.message;
  }
  switching.value = false;
}

// 繁育调试
const breedingDebug = ref(false);
const breedMsg = ref('');
const breedErr = ref('');

async function debugFinishBreeding() {
  playSound('button');
  breedingDebug.value = true;
  breedMsg.value = '';
  breedErr.value = '';
  try {
    const data = await http.post('/api/breeding/debug/finish');
    breedMsg.value = data.message || '操作完成';
    setTimeout(() => { breedMsg.value = ''; }, 5000);
  } catch (e) {
    breedErr.value = e.message;
  }
  breedingDebug.value = false;
}

// API 配置
const apiConfigs = ref([]);
const showApiForm = ref(false);
const editingApiId = ref(null);
const apiMsg = ref('');
const apiErr = ref('');
const apiForm = ref({ apiType: 'text', provider: 'openai', baseUrl: '', apiKey: '', apiSecret: '', appId: '', modelName: '', voice: 'alloy', remark: '' });
const apiPlaceholder = ref({ baseUrl: 'https://api.openai.com/v1', key: 'sk-...', model: 'gpt-4o-mini' });

// 气泡
const bubbles = ref([]);
const bubbleForm = ref({ species: '', personality: '', gender: '', category: 'random', content: '' });

// 音乐管理
const { playlist: adminPlaylist, addTrack, removeTrack, clearPlaylist, playTrack: playAdminTrackFn } = useSound();
const musicFileInput = ref(null);
const musicMsg = ref('');
const musicErr = ref('');

function triggerMusicUpload() {
  playSound('button');
  musicFileInput.value?.click();
}

function onMusicFilesSelected(e) {
  const files = e.target.files;
  if (!files || files.length === 0) return;
  handleMusicFiles(Array.from(files));
  e.target.value = '';
}

function onMusicDrop(e) {
  const files = e.dataTransfer?.files;
  if (!files || files.length === 0) return;
  handleMusicFiles(Array.from(files).filter(f => f.type.startsWith('audio/')));
}

async function handleMusicFiles(files) {
  musicMsg.value = '';
  musicErr.value = '';
  
  if (!files.length) {
    musicErr.value = '请选择音频文件';
    return;
  }

  let added = 0;
  for (const file of files) {
    if (!file.type.startsWith('audio/')) continue;
    
    try {
      // 使用 FileReader 读取为 base64
      const dataUrl = await readFileAsDataURL(file);
      const name = file.name.replace(/\.[^/.]+$/, ''); // 去掉扩展名
      addTrack({
        name,
        url: dataUrl,
        source: 'admin'
      });
      added++;
    } catch (e) {
      console.error('导入音乐失败:', file.name, e);
    }
  }

  if (added > 0) {
    musicMsg.value = `成功导入 ${added} 首音乐`;
    playSound('play');
  } else {
    musicErr.value = '没有成功导入任何音乐文件';
  }
  
  setTimeout(() => {
    musicMsg.value = '';
    musicErr.value = '';
  }, 3000);
}

function readFileAsDataURL(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

function playAdminTrack(idx) {
  playSound('button');
  playAdminTrackFn(idx);
  musicMsg.value = '正在播放: ' + adminPlaylist.value[idx].name;
  setTimeout(() => { musicMsg.value = ''; }, 2000);
}

function removeAdminTrack(idx) {
  playSound('button');
  const track = adminPlaylist.value[idx];
  if (track.source === 'default' && adminPlaylist.value.length <= 1) {
    musicErr.value = '至少保留一首默认音乐';
    setTimeout(() => { musicErr.value = ''; }, 2000);
    return;
  }
  removeTrack(idx);
  musicMsg.value = '已删除: ' + track.name;
  setTimeout(() => { musicMsg.value = ''; }, 2000);
}

function clearAllMusic() {
  if (!confirm('确定要清空所有导入的音乐吗?默认音乐会保留。')) return;
  playSound('button');
  
  // 只保留默认音乐
  const defaultTracks = adminPlaylist.value.filter(t => t.source === 'default');
  clearPlaylist();
  defaultTracks.forEach(t => addTrack(t));
  
  musicMsg.value = '已清空导入的音乐';
  setTimeout(() => { musicMsg.value = ''; }, 2000);
}

function fmt(t) { return t ? String(t).replace('T', ' ').slice(0, 16) : ''; }
function apiTypeLabel(t) { return { text: '文本', voice: '语音', stt: '转写', image: '图片' }[t] || t; }

// ── 用户管理 ──
async function load() {
  err.value = '';
  try {
    const p = await http.get('/api/admin/users', { params: { page: page.value, size, keyword: keyword.value } });
    users.value = p.records;
  } catch (e) { err.value = e.message; }
}
async function toggleStatus(u) {
  msg.value = ''; err.value = '';
  try {
    const target = u.status === 1 ? 0 : 1;
    await http.put('/api/admin/users/' + u.id + '/status', { status: target });
    msg.value = '已' + (target === 1 ? '启用' : '禁用') + ' ' + u.username;
    await load();
  } catch (e) { err.value = e.message; }
}
async function showKeys(u) {
  err.value = '';
  try {
    keyList.value = await http.get('/api/admin/users/' + u.id + '/api-keys');
    keyOwner.value = u.username;
  } catch (e) { err.value = e.message; }
}
function prev() { if (page.value > 1) { page.value--; load(); } }
function next() { page.value++; load(); }

// ── API 配置 ──
async function loadApiConfig() {
  apiErr.value = '';
  try { apiConfigs.value = await http.get('/api/admin/api-config'); } catch (e) { apiErr.value = e.message; }
}
function openApiForm() {
  showApiForm.value = true;
  editingApiId.value = null;
  apiForm.value = { apiType: 'text', provider: 'openai', baseUrl: '', apiKey: '', apiSecret: '', appId: '', modelName: '', voice: 'alloy', remark: '' };
  onApiProviderChange();
}
function editApiConfig(c) {
  showApiForm.value = true;
  editingApiId.value = c.id;
  apiForm.value = {
    apiType: c.apiType,
    provider: c.provider,
    baseUrl: c.baseUrl || '',
    apiKey: '',
    apiSecret: '',
    appId: c.appId || '',
    modelName: c.modelName || '',
    voice: c.voice || 'alloy',
    remark: c.remark || '',
  };
  onApiProviderChange();
}
function onApiProviderChange() {
  const presets = {
    openai: { baseUrl: 'https://api.openai.com/v1', key: 'sk-...', model: apiForm.value.apiType === 'voice' ? 'tts-1' : apiForm.value.apiType === 'stt' ? 'whisper-1' : 'gpt-4o-mini' },
    qwen: { baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1', key: 'sk-...', model: 'qwen-plus' },
    siliconflow: { baseUrl: 'https://api.siliconflow.cn/v1', key: 'sk-...', model: 'Qwen/Qwen2.5-7B-Instruct' },
    zhipu: { baseUrl: 'https://open.bigmodel.cn/api/paas/v4', key: '...', model: 'glm-4-flash' },
    xfyun: { baseUrl: apiForm.value.apiType === 'stt' ? 'wss://iat-api.xfyun.cn/v2/iat' : 'wss://tts-api.xfyun.cn/v2/tts', key: '讯飞 APIKey', model: '' },
    custom: { baseUrl: '', key: '', model: '' },
  };
  const p = presets[apiForm.value.provider] || presets.custom;
  apiPlaceholder.value = p;
  if (apiForm.value.provider !== 'custom' && !apiForm.value.baseUrl) {
    apiForm.value.baseUrl = p.baseUrl;
  }
  if (apiForm.value.provider !== 'custom' && apiForm.value.provider !== 'xfyun' && !apiForm.value.modelName) {
    apiForm.value.modelName = p.model;
  }
  // 讯飞默认音色
  if (apiForm.value.provider === 'xfyun' && apiForm.value.apiType === 'voice' && !apiForm.value.voice) {
    apiForm.value.voice = 'xiaoyan';
  }
}
async function saveApiConfig() {
  apiMsg.value = ''; apiErr.value = '';
  const body = { ...apiForm.value, id: editingApiId.value };
  if (!body.apiKey && !editingApiId.value) { apiErr.value = '请填写 API Key'; return; }
  try {
    await http.post('/api/admin/api-config', body);
    apiMsg.value = editingApiId.value ? '已更新' : '已添加';
    showApiForm.value = false;
    await loadApiConfig();
  } catch (e) { apiErr.value = e.message; }
}
async function deleteApiConfig(id) {
  if (!confirm('确认删除此 API 配置?')) return;
  try {
    await http.delete('/api/admin/api-config/' + id);
    await loadApiConfig();
  } catch (e) { apiErr.value = e.message; }
}

// ── 气泡管理 ──
async function loadBubbles() {
  try { bubbles.value = await http.get('/api/admin/bubbles'); } catch (e) {}
}
async function addBubble() {
  if (!bubbleForm.value.content.trim()) return;
  try {
    await http.post('/api/admin/bubbles', { ...bubbleForm.value, enabled: 1 });
    bubbleForm.value.content = '';
    await loadBubbles();
  } catch (e) {}
}
async function deleteBubble(id) {
  try {
    await http.delete('/api/admin/bubbles/' + id);
    await loadBubbles();
  } catch (e) {}
}

// ── 数据总览 ──
async function loadStats() {
  playSound('button');
  try {
    stats.value = await http.get('/api/admin/stats');
    statsLoadedAt.value = new Date().toLocaleTimeString();
  } catch (e) { err.value = e.message; }
}

// ── 举报管理 ──
function reportStatusLabel(s) {
  return { PENDING: '⏳ 待处理', BLOCKED: '🚫 已拉黑', DISMISSED: '✅ 已驳回' }[s] || s;
}
async function loadReports() {
  playSound('button');
  reportErr.value = '';
  try { reports.value = await http.get('/api/admin/reports'); } catch (e) { reportErr.value = e.message; }
}
async function resolveReport(id, action) {
  playSound('button');
  reportMsg.value = ''; reportErr.value = '';
  if (!confirm(action === 'BLOCK' ? '确认拉黑被举报用户?该用户将被禁用。' : '确认驳回此举报?')) return;
  try {
    await http.post('/api/admin/reports/' + id + '/resolve', { action });
    reportMsg.value = action === 'BLOCK' ? '已拉黑并处理' : '已驳回';
    await loadReports();
    await loadStats();
  } catch (e) { reportErr.value = e.message; }
}

// ── 群发通知 ──
async function sendBroadcast() {
  playSound('button');
  broadcastMsg.value = '';
  broadcastErr.value = '';
  broadcasting.value = true;
  try {
    const data = await http.post('/api/admin/broadcast', { content: broadcastText.value });
    broadcastMsg.value = `通知已发送给 ${data.sentCount} 位用户`;
    broadcastText.value = '';
    setTimeout(() => { broadcastMsg.value = ''; }, 4000);
  } catch (e) {
    broadcastErr.value = e.message;
  }
  broadcasting.value = false;
}

async function sendTestBroadcast() {
  playSound('button');
  broadcastMsg.value = '';
  broadcastErr.value = '';
  broadcasting.value = true;
  try {
    await http.post('/api/admin/broadcast/test', { content: broadcastText.value || '' });
    broadcastMsg.value = '测试通知已发送到你的社交列表,请查看「官方助手」对话';
    setTimeout(() => { broadcastMsg.value = ''; }, 4000);
  } catch (e) {
    broadcastErr.value = e.message;
  }
  broadcasting.value = false;
}

// ── 系统测试 ──
async function runTests() {
  playSound('button');
  testing.value = true;
  testResults.value = [];
  const tests = [
    // ── GET 连通性测试 ──
    { name: '宠物数据(GET)', fn: () => http.get('/api/pet/my') },
    { name: '日程管理(GET)', fn: () => http.get('/api/schedule') },
    { name: '番茄钟记录(GET)', fn: () => http.get('/api/pomodoro/my', { params: { page: 1, size: 1 } }) },
    { name: '商店物品(GET)', fn: () => http.get('/api/shop/items') },
    { name: 'AI 对话历史(GET)', fn: () => http.get('/api/chat/history') },
    { name: '聊天语音偏好(GET)', fn: () => http.get('/api/chat/voice-pref') },
    { name: '气泡提示库(GET)', fn: () => http.get('/api/chat/bubbles') },
    { name: '管理员统计(GET)', fn: () => http.get('/api/admin/stats') },
    { name: '管理员 API 配置(GET)', fn: () => http.get('/api/admin/api-config') },
    // ── 写操作测试(创建→清理,不影响业务数据) ──
    { name: '日程创建+删除(POST+DELETE)', fn: async () => {
      const created = await http.post('/api/schedule', { title: '[系统测试] 临时任务', scheduledAt: new Date(Date.now() + 86400000).toISOString().slice(0, 16), priority: 'LOW', repeat: 'NONE' });
      if (created && created.id) await http.delete('/api/schedule/' + created.id);
      return created;
    }},
    { name: '番茄钟启动+放弃(POST)', fn: async () => {
      const started = await http.post('/api/pomodoro/start', { durationMinutes: 1, label: '[系统测试]' });
      if (started && started.recordId) await http.post('/api/pomodoro/' + started.recordId + '/abandon');
      return started;
    }},
    { name: '气泡提示创建+删除(POST+DELETE)', fn: async () => {
      const added = await http.post('/api/admin/bubbles', { species: '', personality: '', gender: '', category: 'random', content: '[系统测试] 临时气泡', enabled: 1 });
      if (added && added.id) await http.delete('/api/admin/bubbles/' + added.id);
      return added;
    }},
  ];
  for (const t of tests) {
    try {
      await t.fn();
      testResults.value.push({ name: t.name, pass: true, detail: 'OK' });
    } catch (e) {
      testResults.value.push({ name: t.name, pass: false, detail: e.message });
    }
  }
  testing.value = false;
}

onMounted(async () => {
  await loadStats();
  // 加载宠物类型(用于换宠测试)
  try {
    petTypes.value = await http.get('/api/pet/types');
    if (petTypes.value.length > 0) {
      switchPetForm.value.typeCode = petTypes.value[0].typeCode;
    }
  } catch (e) {}
});
</script>

<style scoped>
.admin-tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 12px;
  flex-wrap: wrap;
  align-items: center;
}
.tab-spacer {
  flex: 1;
}
.chip-ghost {
  opacity: 0.6;
  font-size: 12px !important;
  padding: 5px 10px !important;
}
.chip-ghost:hover {
  opacity: 1;
}
table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 10px;
  font-size: 13px;
}
th, td {
  text-align: left;
  padding: 7px 6px;
  border-bottom: 1px solid var(--border);
}
.api-config-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px dashed var(--border);
  flex-wrap: wrap;
}
.api-config-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.api-type-tag {
  padding: 2px 8px;
  border-radius: var(--radius-xl);
  font-size: 11px;
  font-weight: 600;
}
.type-text { background: color-mix(in oklch, var(--accent) 15%, var(--surface)); color: color-mix(in oklch, var(--accent) 60%, black); }
.type-voice { background: color-mix(in oklch, #5b9a8b 15%, var(--surface)); color: #3d6b60; }
.type-stt { background: color-mix(in oklch, #7b6cc7 15%, var(--surface)); color: #5a4a9a; }
.type-image { background: color-mix(in oklch, #e8c84a 20%, var(--surface)); color: #8a6d00; }
.api-form {
  margin-top: 12px;
  padding: 12px;
  border: 2px dashed var(--border);
  border-radius: var(--radius-lg);
}
.api-form .field { margin-bottom: 8px; }
.api-form label { font-size: 12px; color: var(--muted); font-weight: 600; display: block; margin-bottom: 3px; }
.api-form input, .api-form select {
  width: 100% !important;
  padding: 8px 12px !important;
  border: 2.5px solid var(--border) !important;
  border-radius: 10px !important;
  font-size: 13px !important;
  background: var(--surface) !important;
  color: var(--fg) !important;
  box-shadow: none !important;
}
.api-form-btns { display: flex; gap: 8px; margin-top: 8px; }
.bubble-form { margin-bottom: 12px; }
.bubble-form-row {
  display: flex;
  gap: 6px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}
.bubble-form-row select, .bubble-form-row input {
  width: auto !important;
  padding: 6px 10px !important;
  border: 2.5px solid var(--border) !important;
  border-radius: 10px !important;
  font-size: 13px !important;
  background: var(--surface) !important;
  color: var(--fg) !important;
  box-shadow: none !important;
}
.bubble-table { font-size: 12px; }

/* ── 数据总览 ── */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 10px;
}
.stat-card {
  background: var(--surface);
  border: 2px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 14px 10px;
  text-align: center;
  transition: transform 0.2s;
}
.stat-card:hover { transform: translateY(-2px); }
.stat-icon { font-size: 28px; }
.stat-num {
  font-family: var(--font-pix);
  font-size: 28px;
  color: var(--accent);
  margin: 4px 0;
}
.stat-label {
  font-size: 13px;
  font-weight: 700;
  color: var(--fg);
}
.stat-sub {
  font-size: 11px;
  color: var(--muted);
  margin-top: 2px;
}

/* ── 举报管理 ── */
.report-status {
  padding: 2px 8px;
  border-radius: var(--radius-xl);
  font-size: 11px;
  font-weight: 600;
}
.rs-pending { background: color-mix(in oklch, #e8c84a 20%, var(--surface)); color: #8a6d00; }
.rs-blocked { background: color-mix(in oklch, #d1453b 15%, var(--surface)); color: #a03028; }
.rs-dismissed { background: color-mix(in oklch, #5b9a8b 15%, var(--surface)); color: #3d6b60; }

/* ── 系统测试 ── */
.test-results { margin-top: 12px; }
.test-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: var(--radius);
  margin-bottom: 4px;
  font-size: 13px;
}
.test-item.pass { background: color-mix(in oklch, #5b9a8b 10%, var(--surface)); }
.test-item.fail { background: color-mix(in oklch, #d1453b 8%, var(--surface)); }
.test-icon { font-size: 16px; }
.test-name { font-weight: 600; min-width: 120px; }
.test-detail { color: var(--muted); font-size: 12px; }
.test-summary {
  margin-top: 10px;
  padding: 10px;
  text-align: center;
  font-size: 14px;
  font-weight: 700;
  border-radius: var(--radius);
  background: var(--accent-soft);
}

/* ── 季节测试 ── */
.season-test-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
}
.season-btn {
  min-width: 70px;
  padding: 8px 16px !important;
  font-size: 14px !important;
}
.season-btn.active {
  background: var(--accent) !important;
  color: var(--surface) !important;
  border-color: color-mix(in oklch, var(--accent) 55%, black) !important;
}

/* 音乐管理 */
.music-upload-area {
  margin: 12px 0;
}
.upload-box {
  border: 2px dashed #e0c8a0;
  border-radius: 12px;
  padding: 30px 20px;
  text-align: center;
  background: #fffaf0;
  cursor: pointer;
  transition: all 0.2s;
}
.upload-box:hover {
  border-color: #e8a050;
  background: #fff4e0;
}
.upload-icon {
  font-size: 36px;
  margin-bottom: 8px;
}
.upload-text {
  font-size: 14px;
  font-weight: 600;
  color: #8b5a2b;
  margin-bottom: 4px;
}
.upload-hint {
  font-size: 12px;
  color: #a08060;
}
.admin-music-list {
  margin-top: 16px;
}
.music-list-header {
  margin-bottom: 8px;
}
.music-list-title {
  font-size: 13px;
  font-weight: 600;
  color: #8b5a2b;
}
.music-list-items {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid #f0e0c8;
  border-radius: 8px;
  background: #fffaf0;
}
.music-list-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-bottom: 1px solid #f5ead8;
}
.music-list-item:last-child {
  border-bottom: none;
}
.music-idx {
  width: 24px;
  font-size: 12px;
  color: #b09070;
  text-align: center;
  font-family: 'Courier New', monospace;
}
.music-name {
  flex: 1;
  font-size: 13px;
  color: #6b5030;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.music-source {
  font-size: 11px;
  color: #a08060;
  background: #f5ead8;
  padding: 2px 8px;
  border-radius: 4px;
}
.music-actions {
  display: flex;
  gap: 6px;
}
.pix-btn.btn-xs {
  padding: 3px 8px;
  font-size: 11px;
}
</style>
