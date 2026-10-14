import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import path from 'path';
// https://vite.dev/config/
export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, 'src')
        }
    },
    server: {
        port: 5173,
        proxy: {
            '/api': {
                // 2.0 统一从 API Gateway 进入，再由网关转发到各微服务
                target: 'http://localhost:8080',
                changeOrigin: true
            },
            '/uploads': {
                target: 'http://localhost:8103',
                changeOrigin: true
            }
        }
    },
    css: {
        preprocessorOptions: {
            scss: {
                additionalData: "@use \"@/styles/variables.scss\" as *;"
            }
        }
    }
});
