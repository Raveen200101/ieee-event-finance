import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

const backend = 'http://127.0.0.1:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': backend,
      '/actuator/health': backend,
    },
  },
})
