import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';

export default defineConfig({
    plugins: [vue()],
    resolve: {
        alias: {
            '@': resolve(__dirname, 'src'),
        },
    },
    build: {
        cssCodeSplit: true,
        cssMinify: 'esbuild',
        emptyOutDir: false,
        manifest: true,
        minify: 'esbuild',
        sourcemap: false,
        // The enforced gzip budget is 350 KiB per chunk; keep Vite's raw-size hint aligned with it.
        chunkSizeWarningLimit: 900,
        rollupOptions: {
            output: {
                manualChunks(id) {
                    const moduleId = id.replaceAll('\\', '/');
                    if (!moduleId.includes('/node_modules/')) {
                        return undefined;
                    }
                    if (moduleId.includes('/echarts/') || moduleId.includes('/zrender/')) {
                        return 'charts';
                    }
                    if (moduleId.includes('/element-plus/') || moduleId.includes('/@element-plus/icons-vue/')) {
                        return 'ui';
                    }
                    if (moduleId.includes('/vue/') || moduleId.includes('/@vue/') || moduleId.includes('/pinia/') || moduleId.includes('/vue-router/')) {
                        return 'vue';
                    }
                    return 'vendor';
                },
            },
        },
    },
    server: {
        port: 5173,
        proxy: {
            '/api': {
                // 固定使用 IPv4，确保代理连接到开发环境中的同一个本地 API 实例。
                target: 'http://127.0.0.1:8081',
                changeOrigin: true,
            },
        },
    },
});
