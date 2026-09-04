import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    open: false,
    // 本地开发代理：转发到后端 Spring Boot 服务
    proxy: {
      '/graph': {
        target: 'http://127.0.0.1:8009',
        changeOrigin: true,
      },
    },
  },
})
