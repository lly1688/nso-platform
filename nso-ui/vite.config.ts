import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  build: {
    minify: false,
    emptyOutDir: false,
    chunkSizeWarningLimit: 4000
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        // Keep the proxy on IPv4 so it reaches the same local API instance as the dev setup.
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
