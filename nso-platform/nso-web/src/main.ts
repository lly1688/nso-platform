import { createApp } from 'vue';
import { createPinia } from 'pinia';
import { ElMessage } from 'element-plus';
import App from './App.vue';
import { router } from '@/router';
import { userFacingError } from '@/utils/request';
import { installElementPlus } from '@/plugins/element-plus';
import './styles.css';

// 应用启动顺序与全局依赖注册。
const app = createApp(App);
installElementPlus(app);
app.use(createPinia());
app.use(router);

function showFailure(error: unknown) {
    ElMessage.error(userFacingError(error));
}

// 会话失效、权限不足和未处理异常的统一提示。
window.addEventListener('nso-auth-required', () => {
    if (router.currentRoute.value.name !== 'login') {
        void router.replace({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } });
    }
});

window.addEventListener('nso-access-denied', () => ElMessage.warning('当前账号没有执行此操作的权限'));
window.addEventListener('unhandledrejection', (event) => {
    event.preventDefault();
    showFailure(event.reason);
});

app.config.errorHandler = (error) => showFailure(error);
app.mount('#app');
