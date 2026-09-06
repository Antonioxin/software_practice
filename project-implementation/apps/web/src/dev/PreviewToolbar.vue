<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { activePreview, previewScenes, type PreviewRole, type PreviewState } from './preview'

const route = useRoute()
const currentScene = computed(() => {
  const exact = previewScenes.find(scene => scene.path === route.path)
  if (exact) return exact.path
  return previewScenes.find(scene => /[0-9a-f]{8}-/.test(scene.path)
    && route.path.startsWith(scene.path.slice(0, scene.path.lastIndexOf('/') + 1)))?.path ?? ''
})

function navigate(path: string, role: PreviewRole, state: PreviewState = activePreview.state) {
  const url = new URL(path, window.location.origin)
  url.searchParams.set('preview', '1')
  url.searchParams.set('role', role)
  url.searchParams.set('state', state)
  window.location.assign(url.pathname + url.search)
}
function selectScene(event: Event) {
  const scene = previewScenes.find(item => item.path === (event.target as HTMLSelectElement).value)
  if (scene) navigate(scene.path, scene.role)
}
function selectRole(event: Event) {
  const role = (event.target as HTMLSelectElement).value as PreviewRole
  navigate(role === 'admin' ? '/admin/users' : role === 'user' ? '/account/profile' : '/products', role)
}
function selectState(event: Event) {
  navigate(route.path, activePreview.role, (event.target as HTMLSelectElement).value as PreviewState)
}
function exit() { window.location.assign('/products') }
</script>

<template>
  <div class="wm-preview-space" aria-hidden="true"></div>
  <aside class="wm-preview" aria-label="开发环境只读界面预览">
    <details class="wm-preview-panel" open>
      <summary class="wm-preview-summary">
        <span class="wm-preview-dot" aria-hidden="true"></span>
        <strong>只读界面预览</strong>
        <span>示例数据 · 不连接后端</span>
        <span class="wm-preview-fold" aria-hidden="true">收起 / 展开</span>
      </summary>
      <div class="wm-preview-controls">
        <label class="wm-preview-scene">场景
          <select :value="currentScene" @change="selectScene">
            <option v-if="!currentScene" value="" disabled>当前页面</option>
            <option v-for="scene in previewScenes" :key="scene.path" :value="scene.path">{{ scene.label }}</option>
          </select>
        </label>
        <label>身份
          <select :value="activePreview.role" @change="selectRole">
            <option value="guest">游客</option><option value="user">普通用户</option><option value="admin">管理员</option>
          </select>
        </label>
        <label>数据状态
          <select :value="activePreview.state" @change="selectState">
            <option value="normal">正常</option><option value="empty">空数据</option><option value="error">加载错误</option>
          </select>
        </label>
        <button type="button" @click="exit">退出预览 ↗</button>
      </div>
      <p class="wm-preview-note">可检查导航、筛选、表单及弹窗；提交不会保存。空数据与错误状态保留当前示例身份。</p>
    </details>
  </aside>
</template>

<style scoped>
.wm-preview-space { height: 140px; }
.wm-preview { position: fixed; z-index: 200; right: 20px; bottom: 16px; left: 20px; width: min(820px, calc(100% - 40px)); margin: auto; color: #284137; font: 12px/1.4 var(--font-body, system-ui, sans-serif); }
.wm-preview-panel { border: 1px solid rgb(255 255 255 / 90%); border-radius: 18px; background: rgb(247 249 244 / 95%); box-shadow: 0 6px 28px rgb(35 58 44 / 13%); backdrop-filter: blur(16px); -webkit-backdrop-filter: blur(16px); }
.wm-preview-summary { display: flex; flex-wrap: wrap; align-items: center; gap: 7px; padding: 11px 15px; list-style: none; cursor: pointer; }
.wm-preview-summary::-webkit-details-marker { display: none; }
.wm-preview-summary strong { font-size: 12px; }
.wm-preview-summary > span:not(.wm-preview-dot) { color: #5d6d62; font-size: 11px; }
.wm-preview-dot { width: 7px; height: 7px; border-radius: 50%; background: #779b74; }
.wm-preview-fold { margin-left: auto; }
.wm-preview-controls { display: grid; grid-template-columns: 1.5fr 1fr 1fr auto; align-items: end; gap: 12px; padding: 0 15px; }
.wm-preview-controls label { display: grid; min-width: 0; gap: 4px; color: #5d6d62; font-size: 10px; }
.wm-preview-controls select, .wm-preview-controls button { width: 100%; min-width: 0; min-height: 34px; margin: 0; border: 1px solid #d5ddd2; border-radius: 8px; padding: 6px 9px; color: #284137; background: rgb(255 255 255 / 88%); font: inherit; font-size: 12px; }
.wm-preview-controls button { width: auto; cursor: pointer; white-space: nowrap; }
.wm-preview-controls :focus-visible, .wm-preview-summary:focus-visible { outline: 2px solid #527b58; outline-offset: 3px; }
.wm-preview-note { margin: 8px 15px 11px; color: #647265; font-size: 10px; }
@media (max-width: 600px) {
  .wm-preview { right: 10px; bottom: 8px; left: 10px; width: calc(100% - 20px); }
  .wm-preview-controls { grid-template-columns: 1fr 1fr; gap: 8px; }
  .wm-preview-space { height: 215px; }
  .wm-preview-controls button { min-height: 34px; }
  .wm-preview-summary { gap: 5px; }
  .wm-preview-summary > span:not(.wm-preview-dot) { font-size: 10px; }
}
</style>
