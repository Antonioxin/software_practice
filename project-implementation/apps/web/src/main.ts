import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'element-plus/es/components/dialog/style/css'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import './styles.css'
import './features/commerce/style.css'
import './fonts/yozai.css'
import './theme.css'
import App from './App.vue'
import { router } from './router'

createApp(App).use(createPinia()).use(router).mount('#app')
