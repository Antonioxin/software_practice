<script setup lang="ts">
import SketchIcon from '../../components/SketchIcon.vue'
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElDialog } from 'element-plus'
import SiteShell from '../../components/SiteShell.vue'
import ContentState from '../../features/content/ContentState.vue'
import ContentPager from '../../features/content/ContentPager.vue'
import ContentEditor from '../../features/content/ContentEditor.vue'
import ProductSelector from '../../features/content/ProductSelector.vue'
import { readArticles, readBanners, readFaqs, readAllMedia, readProducts, saveRecord, setPublication } from '../../features/content/api'
import { mediaUrl, messageOf } from '../../features/content/presentation'
import { inlineImages } from '../../features/content/bodyMarkup'
import { formatDate, statusLabels, type Article, type Banner, type Faq, type Media, type PageResult, type ProductChoice } from '../../features/content/types'
import '../../features/content/style.css'
type Kind = 'articles' | 'faqs' | 'banners'
type RecordItem = Article | Faq | Banner
const tab = ref<Kind>('articles'), result = ref<PageResult<RecordItem> | null>(null), products = ref<ProductChoice[]>([]), media = ref<Media[]>([])
const loading = ref(true), error = ref(''), choicesError = ref(''), notice = ref(''), keyword = ref(''), status = ref('')
const title = computed(() => ({ articles: '文章', faqs: '常见问题', banners: '首页 Banner' })[tab.value])
const dialogOpen = ref(false), editing = ref<RecordItem | null>(null), saving = ref(false), saveError = ref('')
const confirmItem = ref<RecordItem | null>(null), confirmOpen = ref(false), actionError = ref('')
const form = reactive({ title: '', summary: '', pageDescription: '', body: '', category: '', productIds: [] as string[], mediaIds: [] as string[], question: '', answer: '', imageId: '', buttonText: '', targetUrl: '', sortOrder: 0 })
// API mediaIds includes automatic body references; only keep standalone choices here.
const inlineMediaIds = computed(() => new Set(inlineImages(form.body).flatMap(image => {
  const match = /^\/api\/v1\/media\/([^/]+)\/content$/.exec(image.src)
  return match ? [match[1]!.toLowerCase()] : []
})))
const usedInBody = (id: string) => inlineMediaIds.value.has(id.toLowerCase())
function separateBodyMedia() { form.mediaIds = form.mediaIds.filter(id => !usedInBody(id)) }
let request = 0
const characterCount = (value: string) => Array.from(value.trim()).length
function validateCharacters(value: string, label: string, min: number, max: number, required = false) {
  const count = characterCount(value)
  if (!value && !required) return
  if (count < min || count > max) throw new Error(`${label}需为 ${min}—${max} 个字符，当前为 ${count} 个。`)
}
const itemTitle = (item: RecordItem) => ('question' in item ? item.question : item.title) || '未命名草稿'
async function load(page = 1) {
  const current = ++request; loading.value = true; error.value = ''
  try {
    const params = { keyword: keyword.value, status: status.value, page, pageSize: 20 }
    const data = tab.value === 'articles' ? await readArticles(params, true) : tab.value === 'faqs' ? await readFaqs(params, true) : await readBanners(params)
    if (current === request) result.value = data
  } catch (cause) { if (current === request) error.value = messageOf(cause) } finally { if (current === request) loading.value = false }
}
async function loadChoices() {
  choicesError.value = ''
  const [productResult, mediaResult] = await Promise.allSettled([readProducts(true), readAllMedia()])
  if (productResult.status === 'fulfilled') products.value = productResult.value
  if (mediaResult.status === 'fulfilled') media.value = mediaResult.value
  if (productResult.status === 'rejected' || mediaResult.status === 'rejected') choicesError.value = '部分商品或媒体未能加载。可以重试加载资源；已有的关联会保留。'
}
function edit(item: RecordItem | null) {
  editing.value = item; saveError.value = ''
  Object.assign(form, { title: '', summary: '', pageDescription: '', body: '', category: '', productIds: [], mediaIds: [], question: '', answer: '', imageId: '', buttonText: '', targetUrl: '', sortOrder: 0 }, item ? JSON.parse(JSON.stringify(item)) : {})
  if (tab.value === 'articles') separateBodyMedia()
  form.imageId ||= ''; dialogOpen.value = true
}
async function save() {
  if (saving.value) return
  saving.value = true; saveError.value = ''; notice.value = ''
  try {
    const published = editing.value?.status === 'PUBLISHED'
    if (tab.value === 'faqs') validateCharacters(form.question, '问题', 2, 200, published)
    else validateCharacters(form.title, '标题', tab.value === 'articles' ? 2 : 1, 100, published)
    if (tab.value !== 'banners') validateCharacters(form.category, '分类', 1, 80, tab.value === 'faqs' && published)
    if (tab.value === 'banners') validateCharacters(form.targetUrl, '跳转地址', 1, 2048, published)
    const fields = tab.value === 'articles' ? { title: form.title, summary: form.summary, pageDescription: form.pageDescription, body: form.body, category: form.category, productIds: form.productIds, mediaIds: form.mediaIds.filter(id => !usedInBody(id)) } : tab.value === 'faqs' ? { question: form.question, answer: form.answer, category: form.category, productIds: form.productIds } : { title: form.title, imageId: form.imageId || null, buttonText: form.buttonText, targetUrl: form.targetUrl }
    await saveRecord(`/admin/${tab.value}`, { ...fields, sortOrder: Number(form.sortOrder), ...(editing.value ? { expectedVersion: editing.value.version } : {}) }, editing.value?.id)
    notice.value = `${title.value}已保存${editing.value ? '。' : '为草稿，可在列表中发布。'}`; dialogOpen.value = false; await load(result.value?.meta.page || 1)
  } catch (cause) { saveError.value = messageOf(cause) } finally { saving.value = false }
}
function askStatus(item: RecordItem) { confirmItem.value = item; confirmOpen.value = true; actionError.value = '' }
async function changeStatus() {
  if (!confirmItem.value || saving.value) return
  saving.value = true; actionError.value = ''; notice.value = ''
  try { const publish = confirmItem.value.status !== 'PUBLISHED'; await setPublication(`/admin/${tab.value}`, confirmItem.value, publish); notice.value = `${title.value}已${publish ? '发布' : '下线'}。`; confirmOpen.value = false; await load(result.value?.meta.page || 1) }
  catch (cause) { actionError.value = messageOf(cause) } finally { saving.value = false }
}
watch(() => form.body, separateBodyMedia)
watch(tab, () => { keyword.value = ''; status.value = ''; result.value = null; notice.value = ''; void load() })
onMounted(() => { void load(); void loadChoices() })
</script>
<template><SiteShell title="内容管理" admin eyebrow="BRAND / CONTENT"><div class="content-admin-intro"><div><p>让每一段故事，都有自己的位置。</p><span>管理文章、常见问题与首页展示。草稿保存后，可单独发布或下线。</span></div><RouterLink to="/admin/content/settings" class="secondary-button">品牌与首页设置 ↗</RouterLink></div><div class="content-tabs" role="tablist" aria-label="内容类型"><button v-for="(label, key) in { articles: '文章与玩法', faqs: '常见问题', banners: '首页 Banner' }" :id="`tab-${key}`" :key="key" type="button" role="tab" :aria-selected="tab === key" aria-controls="content-admin-list" @click="tab = key as Kind"><SketchIcon :name="key === 'articles' ? 'orders' : key === 'faqs' ? 'help' : 'home'" :size="21" />{{ label }}</button></div><div id="content-admin-list" role="tabpanel" :aria-labelledby="`tab-${tab}`"><form class="content-admin-filter" @submit.prevent="load(1)"><label>搜索{{ title }}<input v-model="keyword" type="search" class="wm-search-input" placeholder="输入关键词" maxlength="100" /></label><label>发布状态<select v-model="status"><option value="">全部状态</option><option v-for="(label, key) in statusLabels" :key="key" :value="key">{{ label }}</option></select></label><button type="submit" class="secondary-button">查询</button><button type="button" class="primary-button" @click="edit(null)"><SketchIcon name="plus" :size="20" />新建{{ title }}</button></form><p v-if="notice" class="content-success" role="status">{{ notice }}</p><ContentState :loading="loading" :error="error" :empty="!result?.items.length" :empty-title="`还没有${title}`" empty-note="点击新建，开始整理第一份内容。" @retry="load()"><div class="table-wrap"><table class="content-admin-table"><thead><tr><th>{{ title }}</th><th>状态</th><th>排序</th><th>最近更新</th><th>操作</th></tr></thead><tbody><tr v-for="item in result?.items" :key="item.id"><td><strong>{{ itemTitle(item) }}</strong><span v-if="'category' in item">{{ item.category }}</span><span v-if="'buttonText' in item">{{ item.buttonText || '无按钮' }} · {{ item.targetUrl || '无跳转' }}</span></td><td><span class="content-status" :data-status="item.status">{{ statusLabels[item.status] }}</span></td><td>{{ item.sortOrder }}</td><td>{{ formatDate(item.updatedAt) }}</td><td><div class="content-row-actions"><button type="button" class="row-link" @click="edit(item)">编辑</button><RouterLink v-if="tab === 'articles' && item.status === 'PUBLISHED'" :to="`/articles/${item.id}`" class="row-link">查看</RouterLink><button type="button" class="row-link" @click="askStatus(item)">{{ item.status === 'PUBLISHED' ? '下线' : '发布' }}</button></div></td></tr></tbody></table></div><ContentPager v-if="result" :meta="result.meta" @change="load" /></ContentState></div>
<ElDialog v-model="dialogOpen" :title="`${editing ? '编辑' : '新建'}${title}`" :width="tab === 'articles' ? 'min(960px, 94vw)' : 'min(820px, 94vw)'" :show-close="!saving" :close-on-click-modal="!saving" :close-on-press-escape="!saving"><form class="content-form" @submit.prevent="save"><p v-if="saveError" role="alert" class="error-summary wide">{{ saveError }}</p><p v-if="choicesError" class="content-note wide">{{ choicesError }} <button class="row-link" type="button" @click="loadChoices">重新加载资源</button></p><label v-if="tab !== 'faqs'" class="wide">标题<input v-model="form.title" :required="editing?.status === 'PUBLISHED'" maxlength="200" /><small :class="{ 'content-limit-exceeded': characterCount(form.title) > 100 }">{{ characterCount(form.title) }} / 100 字符</small></label><label v-else class="wide">问题<input v-model="form.question" :required="editing?.status === 'PUBLISHED'" maxlength="400" /><small :class="{ 'content-limit-exceeded': characterCount(form.question) > 200 }">{{ characterCount(form.question) }} / 200 字符</small></label><label v-if="tab !== 'banners'">分类<input v-model="form.category" :required="tab === 'faqs' && editing?.status === 'PUBLISHED'" maxlength="160" placeholder="例如：玩法灵感 / 使用与保养" /></label><label>排序<input v-model.number="form.sortOrder" type="number" min="0" max="999999" required /><small>数值越小越靠前。</small></label><template v-if="tab === 'articles'"><label class="wide">摘要<textarea v-model="form.summary" maxlength="500" rows="3" /></label><label class="wide">页面描述<input v-model="form.pageDescription" maxlength="300" placeholder="用于搜索结果摘要；留空沿用文章摘要" /></label><ContentEditor id="article-body" v-model="form.body" article :media="media" class="wide" label="正文" :maxlength="200000" :text-limit="20000" :required="editing?.status === 'PUBLISHED'" /><fieldset class="content-choice-field wide"><legend>独立封面与补充配图（可选，第一张为封面）</legend><p class="content-note">正文配图在图文块中统一管理；这里仅选择正文之外的封面或补充图。未另选封面时，文章列表使用正文第一张图。</p><div class="content-media-options"><label v-for="image in media" :key="image.id"><img :src="image.url || mediaUrl(image.id)" :alt="image.altText" loading="lazy" /><span><input v-model="form.mediaIds" type="checkbox" :value="image.id" :disabled="usedInBody(image.id) || (!form.mediaIds.includes(image.id) && form.mediaIds.length + inlineMediaIds.size >= 20)" /> {{ image.altText || image.filename }}</span><small v-if="usedInBody(image.id)">已用于正文 · 在图文块中管理</small></label></div><p v-if="!media.length" class="content-note">暂无媒体，请先到文件与媒体上传图片。</p></fieldset></template><ContentEditor v-if="tab === 'faqs'" id="faq-answer" v-model="form.answer" class="wide" label="答案" :maxlength="60000" :text-limit="5000" :required="editing?.status === 'PUBLISHED'" /><ProductSelector v-if="tab !== 'banners'" v-model="form.productIds" class="wide" :products="products" /><template v-if="tab === 'banners'"><label class="wide">展示图片<select v-model="form.imageId" :required="editing?.status === 'PUBLISHED'"><option value="">请选择已上传的图片</option><option v-for="image in media" :key="image.id" :value="image.id">{{ image.altText || image.filename }}</option></select><small>发布前需要选择有效图片，可在“文件与媒体”中上传。</small></label><img v-if="form.imageId" class="content-banner-preview wide" :src="media.find(m => m.id === form.imageId)?.url || mediaUrl(form.imageId)" :alt="form.title" /><label>按钮文字<input v-model="form.buttonText" maxlength="50" placeholder="例如：探索更多" /></label><label>跳转地址<input v-model="form.targetUrl" :required="editing?.status === 'PUBLISHED'" maxlength="4096" placeholder="/products 或 https://…" /><small :class="{ 'content-limit-exceeded': characterCount(form.targetUrl) > 2048 }">{{ characterCount(form.targetUrl) }} / 2048 字符</small></label><p class="content-note wide">按钮文字与跳转地址需一起填写；支持站内路径或 HTTP / HTTPS 链接。</p></template><p v-if="!editing || editing.status !== 'PUBLISHED'" class="content-note wide">尚未写完也可以保存为草稿。发布前需补齐标题与正文；Banner 需补齐图片和跳转地址。</p><div class="content-form-actions wide"><button type="button" class="secondary-button" :disabled="saving" @click="dialogOpen = false">取消</button><button type="submit" class="primary-button" :disabled="saving">{{ saving ? '正在保存…' : editing ? '保存修改' : '保存草稿' }}</button></div></form></ElDialog>
<ElDialog v-model="confirmOpen" :title="confirmItem?.status === 'PUBLISHED' ? '下线内容' : '发布内容'" width="min(480px, 94vw)" :show-close="!saving" :close-on-click-modal="!saving" :close-on-press-escape="!saving"><p v-if="confirmItem">{{ confirmItem.status === 'PUBLISHED' ? '下线后，访客将无法继续访问' : '发布后，访客将可以看到' }}“{{ itemTitle(confirmItem) }}”。</p><p v-if="actionError" role="alert" class="error-summary">{{ actionError }}</p><template #footer><div class="content-form-actions"><button type="button" class="secondary-button" :disabled="saving" @click="confirmOpen = false">取消</button><button type="button" class="primary-button" :disabled="saving" @click="changeStatus">{{ saving ? '正在处理…' : '确认' }}</button></div></template></ElDialog></SiteShell></template>
