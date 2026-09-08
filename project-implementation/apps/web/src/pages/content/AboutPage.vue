<script setup lang="ts">
import SketchIcon from '../../components/SketchIcon.vue'
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import PublicShell from '../../components/PublicShell.vue'
import ContentHero from '../../features/content/ContentHero.vue'
import ContentState from '../../features/content/ContentState.vue'
import ContentBody from '../../features/content/ContentBody'
import { readSettings } from '../../features/content/api'
import { mediaUrl, messageOf } from '../../features/content/presentation'
import type { SiteSettings } from '../../features/content/types'
import '../../features/content/style.css'
const route = useRoute(), settings = ref<SiteSettings | null>(null), loading = ref(true), error = ref('')
const kind = computed(() => route.path === '/terms' ? 'terms' : route.path === '/privacy' ? 'privacy' : 'about')
const copy = computed(() => kind.value === 'terms' ? { eyebrow: 'TERMS OF SERVICE', title: 'Play by the rules.', subtitle: '服务条款', note: '在开始之前，了解我们的服务约定。' } : kind.value === 'privacy' ? { eyebrow: 'YOUR PRIVACY MATTERS', title: 'A little trust.', subtitle: '隐私政策', note: '了解我们如何处理与保护你的信息。' } : { eyebrow: 'A WORLD WITH MORE PLAY', title: 'We move, together.', subtitle: '关于 WEMOVE', note: '给日常留一点空白，让运动和玩耍自然发生。' })
async function load() { loading.value = true; error.value = ''; try { settings.value = await readSettings() } catch (cause) { error.value = messageOf(cause) } finally { loading.value = false } }
onMounted(load)
</script>
<template><PublicShell><ContentHero v-bind="copy" icon="home" /><section class="content-public content-reading"><ContentState :loading="loading" :error="error" @retry="load"><template v-if="settings"><article v-if="kind === 'about'" class="content-about"><img v-if="settings.logoMediaId" :src="mediaUrl(settings.logoMediaId)" :alt="settings.brandName" class="content-about-logo" /><h2>{{ settings.brandName }}</h2><ContentBody :body="settings.brandDescription" /><div class="content-contact-grid"><div><p class="content-eyebrow">SAY HELLO</p><h3><SketchIcon name="chat" :size="26" />保持联系</h3><a v-if="settings.contactEmail" :href="`mailto:${settings.contactEmail}`">{{ settings.contactEmail }}</a><a v-if="settings.contactPhone" :href="`tel:${settings.contactPhone}`">{{ settings.contactPhone }}</a></div><div><p class="content-eyebrow">FIND US</p><h3><SketchIcon name="home" :size="26" />我们在这里</h3><address>{{ settings.contactAddress || '更多联系信息即将更新。' }}</address></div></div></article><article v-else class="content-story"><p class="content-kicker">版本 {{ kind === 'terms' ? settings.termsVersion : settings.privacyVersion }}</p><ContentBody :body="kind === 'terms' ? settings.termsText : settings.privacyText" /><p v-if="!(kind === 'terms' ? settings.termsText : settings.privacyText)" class="content-note">内容正在整理，如有疑问请联系我们。</p></article></template></ContentState></section></PublicShell></template>
