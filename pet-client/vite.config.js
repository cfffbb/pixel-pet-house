import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

// base './' 让打包后能用 file:// 协议直接打开(Electron 加载本地文件必需)
export default defineConfig({
  base: './',
  plugins: [vue()],
  server: {
    port: 5173,
    strictPort: true,
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
});
