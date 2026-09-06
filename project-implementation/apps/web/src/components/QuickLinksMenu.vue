<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, useId, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useSessionStore } from '../stores/session'
import SketchIcon from './SketchIcon.vue'

const session = useSessionStore()
const route = useRoute()
const open = ref(false)
const root = ref<HTMLElement | null>(null)
const trigger = ref<HTMLButtonElement | null>(null)
const panelId = `quick-links-${useId()}`
const entries = computed(() => [
  { title: '发现好玩的', description: '探索全部商品', icon: 'grid', to: '/products' },
  session.isAdmin
    ? { title: '整理好物', description: '商品、上架与库存', icon: 'package', to: '/admin/products' }
    : { title: '装一点快乐', description: '查看购物车，继续挑选', icon: 'cart', to: '/cart' },
  { title: '期待每次出发', description: session.isAdmin ? '管理订单与配送' : '查看我的订单与配送', icon: 'truck', to: session.isAdmin ? '/admin/orders' : '/account/orders' },
  { title: '为你留的位置', description: session.isAdmin ? '管理用户账户' : '个人资料与联系信息', icon: 'user', to: session.isAdmin ? '/admin/users' : '/account/profile' },
])

function close(returnFocus = false) {
  if (!open.value) return
  open.value = false
  if (returnFocus) trigger.value?.focus()
}
function outsidePointer(event: PointerEvent) {
  if (event.target instanceof Node && !root.value?.contains(event.target)) close()
}
function focusLeaves(event: FocusEvent) {
  if (event.relatedTarget instanceof Node && !root.value?.contains(event.relatedTarget)) close()
}
watch(() => route.fullPath, () => close())
onMounted(() => document.addEventListener('pointerdown', outsidePointer))
onBeforeUnmount(() => document.removeEventListener('pointerdown', outsidePointer))
</script>

<template>
  <div ref="root" class="wm-quick-links" @keydown.esc.prevent.stop="close(true)" @focusout="focusLeaves">
    <button ref="trigger" class="wm-quick-trigger" type="button" :aria-expanded="open" :aria-controls="panelId" @click="open = !open">
      <span>快捷入口</span><SketchIcon name="chevron-down" :size="17" :class="{ 'wm-quick-chevron-open': open }" />
    </button>
    <nav v-if="open" :id="panelId" class="wm-quick-panel" aria-label="快捷入口">
      <RouterLink v-for="entry in entries" :key="entry.to" class="wm-quick-link" :to="entry.to" @click="close()">
        <SketchIcon :name="entry.icon" :size="27" />
        <span class="wm-quick-copy"><strong>{{ entry.title }}</strong><small>{{ entry.description }}</small></span>
        <span class="wm-quick-arrow" aria-hidden="true">↗</span>
      </RouterLink>
    </nav>
  </div>
</template>

<style scoped>
.wm-quick-links { position: relative; flex: 0 0 auto; }
.wm-quick-trigger { display: inline-flex; width: 110px; min-height: 42px; align-items: center; justify-content: center; gap: 7px; border: 1px solid var(--line, #dce0df); border-radius: 22px; padding: 9px 11px; color: var(--ink, #292e32); background: rgb(255 255 255 / 66%); font: 14px/1.4 var(--font-body, system-ui, sans-serif); cursor: pointer; white-space: nowrap; }
.wm-quick-trigger:hover, .wm-quick-trigger[aria-expanded="true"] { background: #edf0ec; }
.wm-quick-chevron-open { transform: rotate(180deg); }
.wm-quick-panel { position: absolute; z-index: 45; top: calc(100% + 12px); right: 0; width: 278px; max-width: calc(100vw - 32px); padding: 7px; border: 1px solid rgb(255 255 255 / 95%); border-radius: 18px; background: rgb(253 254 250 / 96%); box-shadow: 0 12px 38px rgb(45 57 47 / 13%), inset 0 1px 0 white; backdrop-filter: blur(18px); -webkit-backdrop-filter: blur(18px); }
.wm-quick-link { display: flex; align-items: center; gap: 12px; min-height: 67px; padding: 11px 12px; border-radius: 11px; color: var(--ink, #292e32); text-decoration: none; }
.wm-quick-link + .wm-quick-link { border-top: 1px solid rgb(220 224 223 / 52%); }
.wm-quick-link:hover, .wm-quick-link:focus-visible { background: #edf0ec; }
.wm-quick-copy { display: grid; min-width: 0; gap: 4px; }
.wm-quick-copy strong { font: 500 14px/1.35 var(--font-body, system-ui, sans-serif); }
.wm-quick-copy small { color: var(--muted, #626970); font: 11px/1.5 var(--font-body, system-ui, sans-serif); }
.wm-quick-arrow { margin-left: auto; color: var(--muted, #626970); font-size: 15px; }
.wm-quick-trigger:focus-visible, .wm-quick-link:focus-visible { outline: 2px solid #426bac; outline-offset: 3px; }
@media (max-width: 640px) {
  .wm-quick-links { position: static; }
  .wm-quick-trigger { width: 80px; min-height: 40px; gap: 3px; padding: 8px 5px; font-size: 12px; }
  .wm-quick-trigger .sketch-icon { width: 13px; height: 13px; }
  .wm-quick-panel { top: calc(100% + 8px); right: 0; left: 0; width: auto; max-width: none; }
}
</style>
