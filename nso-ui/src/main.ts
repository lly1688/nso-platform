import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import { ElMessage } from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import { router } from '@/router'
import { userFacingError } from '@/utils/request'
import './styles.css'

const app = createApp(App)

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus)

function showFailure(error: unknown) {
  ElMessage.error(userFacingError(error))
}

window.addEventListener('nso-auth-required', () => {
  if (router.currentRoute.value.name !== 'login') {
    void router.replace({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
  }
})
window.addEventListener('nso-access-denied', () => ElMessage.warning('当前账号没有执行此操作的权限'))
window.addEventListener('unhandledrejection', (event) => {
  event.preventDefault()
  showFailure(event.reason)
})
app.config.errorHandler = (error) => showFailure(error)
app.mount('#app')
