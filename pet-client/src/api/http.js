import axios from 'axios';

// 后端地址:优先从 Electron preload 注入的 window.__API_BASE__ 读取(Electron 打包后可动态配置),
// 退化到 localStorage(用户手动配置),最终退化到本地开发地址
const API_BASE = (typeof window !== 'undefined' && window.__API_BASE__)
  || localStorage.getItem('pet_api_base')
  || 'http://localhost:8080';
const http = axios.create({ baseURL: API_BASE, timeout: 15000 });

// 将后端返回的文件路径(如 /uploads/admin/xxx.png)转为完整URL
export function fileUrl(path) {
  if (!path) return '';
  if (path.startsWith('http://') || path.startsWith('https://') || path.startsWith('data:')) return path;
  if (path.startsWith('/')) return API_BASE + path;
  return API_BASE + '/' + path;
}

export { API_BASE };

// 请求拦截:自动带登录 token
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('pet_token');
  if (token) {
    config.headers.Authorization = 'Bearer ' + token;
  }
  return config;
});

// 响应拦截:统一解包 {code, msg, data};401 回登录
http.interceptors.response.use(
  (resp) => {
    const body = resp.data;
    if (body && body.code === 200) {
      return body.data;
    }
    const err = new Error((body && body.msg) || '请求失败');
    if (body && body.code === 401) {
      logout();
    }
    return Promise.reject(err);
  },
  (error) => {
    const msg = (error.response && error.response.data && error.response.data.msg) || error.message || '网络错误';
    return Promise.reject(new Error(msg));
  }
);

export function logout() {
  localStorage.removeItem('pet_token');
  localStorage.removeItem('pet_user');
  // 通知主进程清掉推送用的 token
  if (window.petAPI) window.petAPI.setToken(null);
  if (window.location.search.indexOf('floating') < 0) {
    window.location.reload();
  }
}

export default http;
