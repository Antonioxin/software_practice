<script setup lang="ts">
import SketchIcon from '../../components/SketchIcon.vue'
import { computed, onMounted, reactive, ref } from 'vue'
import SiteShell from '../../components/SiteShell.vue'
import ContentState from '../../features/content/ContentState.vue'
import ContentEditor from '../../features/content/ContentEditor.vue'
import ProductSelector from '../../features/content/ProductSelector.vue'
import { patchSettings, readAllArticles, readHomeSettings, readAllMedia, readProducts, readSettings } from '../../features/content/api'
import { messageOf } from '../../features/content/presentation'
import { statusLabels, type Article, type HomeSettings, type Media, type ProductChoice, type SiteSettings } from '../../features/content/types'
import '../../features/content/style.css'
const loading = ref(true), error = ref(''), choicesError = ref(''), saving = ref(''), saveError = ref(''), notice = ref('')
const products = ref<ProductChoice[]>([]), articles = ref<Article[]>([]), media = ref<Media[]>([])
const settings = reactive<SiteSettings>({ brandName: '', brandDescription: '', contactEmail: '', contactPhone: '', contactAddress: '', logoMediaId: null, termsText: '', termsVersion: '', privacyText: '', privacyVersion: '', version: 0 })
const home = reactive<HomeSettings>({ recommendedProductIds: [], featuredArticleIds: [], version: 0 })
const articleOptions = computed(() => [...articles.value, ...home.featuredArticleIds.filter(id => !articles.value.some(article => article.id === id)).map(id => ({ id, title: '已选文章（当前不可访问）', status: 'OFFLINE' as const }))])
async function load() {
  loading.value = true; error.value = ''; choicesError.value = ''
  try { const [brand, homepage] = await Promise.all([readSettings(true), readHomeSettings()]); Object.assign(settings, brand); Object.assign(home, homepage) }
  catch (cause) { error.value = messageOf(cause) }
  finally { loading.value = false }
  const choices = await Promise.allSettled([readProducts(true), readAllArticles(), readAllMedia()])
  if (choices[0].status === 'fulfilled') products.value = choices[0].value
  if (choices[1].status === 'fulfilled') articles.value = choices[1].value
  if (choices[2].status === 'fulfilled') media.value = choices[2].value
  if (choices.some(item => item.status === 'rejected')) choicesError.value = '部分商品、文章或媒体选项没有加载成功；已有选择会保留。'
}
async function save(kind: 'brand' | 'home') {
  if (saving.value) return
  saving.value = kind; saveError.value = ''; notice.value = ''
  try {
    if (kind === 'brand') { const { version, ...fields } = settings; const result = await patchSettings('/admin/site-settings', { ...fields, logoMediaId: settings.logoMediaId || null, expectedVersion: version }); Object.assign(settings, result) }
    else { const result = await patchSettings('/admin/home', { recommendedProductIds: home.recommendedProductIds, featuredArticleIds: home.featuredArticleIds, expectedVersion: home.version }); Object.assign(home, result) }
    notice.value = kind === 'brand' ? '品牌与联系信息已保存，公开页面已同步更新。' : '首页推荐已保存，首页只展示仍处于发布状态的内容。'
  } catch (cause) { saveError.value = messageOf(cause) } finally { saving.value = '' }
}
onMounted(load)
</script>
<template><SiteShell title="品牌与首页设置" admin eyebrow="BRAND / SETTINGS"><div class="content-admin-intro"><div><p>把 WEMOVE 的故事讲完整。</p><span>维护品牌介绍、联系方式、政策文本，以及首页精选内容。</span></div><RouterLink to="/admin/content" class="secondary-button"><SketchIcon name="arrow-left" :size="20" />返回内容管理</RouterLink></div><p v-if="notice" class="content-success" role="status">{{ notice }}</p><p v-if="saveError" class="error-summary" role="alert">{{ saveError }}</p><p v-if="choicesError" class="content-note">{{ choicesError }} <button type="button" class="row-link" @click="load">重新加载</button></p><ContentState :loading="loading" :error="error" @retry="load"><form class="paper-section content-settings-section content-form" @submit.prevent="save('brand')"><div class="wide content-section-title"><div><p class="content-eyebrow">01 / THE BRAND</p><h2><SketchIcon name="chat" :size="26" />品牌与联系</h2></div><RouterLink to="/about" class="content-text-link">查看公开页面 ↗</RouterLink></div><label>品牌名称<input v-model="settings.brandName" required maxlength="100" /></label><label>品牌标识<select v-model="settings.logoMediaId"><option :value="null">使用默认品牌标识</option><option v-for="image in media" :key="image.id" :value="image.id">{{ image.altText || image.filename }}</option></select></label><ContentEditor id="brand-description" v-model="settings.brandDescription" class="wide" label="品牌介绍" :maxlength="20000" required /><label>联系邮箱<input v-model="settings.contactEmail" type="email" maxlength="200" /></label><label>联系电话<input v-model="settings.contactPhone" type="tel" maxlength="50" /></label><label class="wide">联系地址<input v-model="settings.contactAddress" maxlength="250" /></label><div class="wide content-section-title"><div><p class="content-eyebrow">02 / TRUST & CLARITY</p><h2><SketchIcon name="orders" :size="26" />服务条款与隐私政策</h2></div></div><label>服务条款版本<input v-model="settings.termsVersion" required maxlength="32" placeholder="例如：2026-09" /></label><label>隐私政策版本<input v-model="settings.privacyVersion" required maxlength="32" placeholder="例如：2026-09" /></label><ContentEditor id="terms-text" v-model="settings.termsText" class="wide" label="服务条款正文" required /><ContentEditor id="privacy-text" v-model="settings.privacyText" class="wide" label="隐私政策正文" required /><div class="wide content-form-actions"><button class="primary-button" type="submit" :disabled="!!saving">{{ saving === 'brand' ? '正在保存…' : '保存品牌与政策' }}</button></div></form><form class="paper-section content-settings-section content-form" @submit.prevent="save('home')"><div class="wide content-section-title"><div><p class="content-eyebrow">03 / ON THE FRONT PAGE</p><h2><SketchIcon name="home" :size="26" />首页推荐</h2></div><RouterLink to="/" class="content-text-link">查看首页 ↗</RouterLink></div><p class="wide content-note">按选择顺序展示。已下架商品、下线文章会自动从公开首页隐藏。首页 Banner 在内容管理中维护。</p><ProductSelector v-model="home.recommendedProductIds" :products="products" :max="12" label="推荐商品（按勾选顺序）" class="wide" /><fieldset class="content-choice-field wide"><legend>精选文章（按勾选顺序）</legend><div class="content-choice-grid"><label v-for="article in articleOptions" :key="article.id"><input v-model="home.featuredArticleIds" type="checkbox" :value="article.id" :disabled="!home.featuredArticleIds.includes(article.id) && (home.featuredArticleIds.length >= 6 || article.status !== 'PUBLISHED')" /><span>{{ article.title }}<small v-if="article.status !== 'PUBLISHED'"> · {{ statusLabels[article.status] }}</small></span></label></div><p v-if="!articleOptions.length" class="content-note">暂无文章，请先在内容管理中发布。</p><small>已选择 {{ home.featuredArticleIds.length }} 篇</small></fieldset><div class="wide content-form-actions"><button class="primary-button" type="submit" :disabled="!!saving">{{ saving === 'home' ? '正在保存…' : '保存首页推荐' }}</button></div></form></ContentState></SiteShell></template>
