<script setup lang="ts">
import SketchIcon from '../../components/SketchIcon.vue'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import PublicShell from '../../components/PublicShell.vue'
import ContentHero from '../../features/content/ContentHero.vue'
import ContentState from '../../features/content/ContentState.vue'
import ContentPager from '../../features/content/ContentPager.vue'
import { downloadFile, readFiles, readProducts } from '../../features/content/api'
import { messageOf } from '../../features/content/presentation'
import { formatDate, formatSize, type ContentFile, type PageResult, type ProductChoice } from '../../features/content/types'
import { ApiProblem } from '../../services/http'
import '../../features/content/style.css'
const route = useRoute()
const result = ref<PageResult<ContentFile> | null>(null), products = ref<ProductChoice[]>([])
const loading = ref(true), error = ref(''), actionError = ref(''), notice = ref(''), downloading = ref(''), keyword = ref(''), type = ref(''), productId = ref(typeof route.query.productId === 'string' ? route.query.productId : '')
let request = 0
async function load(page = 1) { const current = ++request; loading.value = true; error.value = ''; result.value = null; try { const data = await readFiles({ keyword: keyword.value, type: type.value, productId: productId.value, page, pageSize: 12 }); if (current === request) result.value = data } catch (cause) { if (current === request) error.value = messageOf(cause) } finally { if (current === request) loading.value = false } }
async function download(item: ContentFile) { downloading.value = item.id; actionError.value = ''; notice.value = ''; try { await downloadFile(item); notice.value = `“${item.title}”已准备下载，请查看浏览器下载记录。` } catch (cause) { actionError.value = messageOf(cause); if (cause instanceof ApiProblem && [401, 403, 404].includes(cause.problem.status)) { result.value = null; await load() } } finally { downloading.value = '' } }
function accountChanged() { result.value = null; actionError.value = ''; notice.value = ''; void load() }
onMounted(async () => { window.addEventListener('wemove:account-changed', accountChanged); window.addEventListener('wemove:auth-invalid', accountChanged); void load(); try { products.value = await readProducts() } catch { /* Keyword and type filters stay usable. */ } })
onBeforeUnmount(() => { request++; window.removeEventListener('wemove:account-changed', accountChanged); window.removeEventListener('wemove:auth-invalid', accountChanged) })
</script>
<template><PublicShell><ContentHero eyebrow="THE USEFUL LITTLE LIBRARY" title="Ready, set, play." subtitle="让每一次开玩，都更轻松" note="产品说明、安全指南与实用资料，随时取用，安心开始。" icon="orders" /><section class="content-public"><form class="content-filters" @submit.prevent="load(1)"><label>资料关键词<input v-model="keyword" type="search" class="wm-search-input" placeholder="搜索资料名称" maxlength="100" /></label><label>资料类型<input v-model="type" placeholder="例如：产品说明" maxlength="50" /></label><label>关联商品<select v-model="productId"><option value="">所有商品</option><option v-for="product in products" :key="product.id" :value="product.id">{{ product.name }}</option></select></label><button class="primary-button" type="submit">查找资料</button></form><p v-if="actionError" class="error-summary" role="alert">{{ actionError }}</p><p v-if="notice" class="content-success" role="status">{{ notice }}</p><ContentState :loading="loading" :error="error" :empty="!result?.items.length" empty-title="暂时没有可下载的资料" empty-note="试试其他筛选条件。部分资料仅对已登录的经销商开放。" @retry="load()"><div class="content-file-grid"><article v-for="file in result?.items" :key="file.id" class="content-file-card"><div class="content-file-icon" aria-hidden="true"><SketchIcon name="orders" :size="32" />PDF</div><p class="content-kicker">{{ file.type }}</p><h2>{{ file.title }}</h2><p>{{ file.versionNote }}</p><small>{{ formatSize(file.sizeBytes) }} · 更新于 {{ formatDate(file.updatedAt) }}</small><small v-if="file.visibility === 'DEALER'">经销商专属资料</small><button class="content-text-link" type="button" :disabled="!!downloading" @click="download(file)">{{ downloading === file.id ? '正在准备…' : '下载 PDF' }} <SketchIcon name="arrow-left" :size="22" class="content-download-icon" /></button></article></div><ContentPager v-if="result" :meta="result.meta" @change="load" /></ContentState><p class="content-download-note">资料由 WEMOVE 整理与维护。下载前请核对商品型号及版本说明。</p></section></PublicShell></template>
