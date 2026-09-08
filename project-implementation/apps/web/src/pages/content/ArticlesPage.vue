<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PublicShell from '../../components/PublicShell.vue'
import ContentHero from '../../features/content/ContentHero.vue'
import ContentState from '../../features/content/ContentState.vue'
import ContentPager from '../../features/content/ContentPager.vue'
import { readArticles } from '../../features/content/api'
import { articleCover, messageOf } from '../../features/content/presentation'
import { formatDate, type Article, type PageResult } from '../../features/content/types'
import '../../features/content/style.css'
const result = ref<PageResult<Article> | null>(null)
const loading = ref(true), error = ref(''), keyword = ref(''), category = ref('')
let request = 0
async function load(page = 1) {
  const current = ++request; loading.value = true; error.value = ''
  try { const data = await readArticles({ keyword: keyword.value, category: category.value, page, pageSize: 9 }); if (current === request) result.value = data }
  catch (cause) { if (current === request) error.value = messageOf(cause) }
  finally { if (current === request) loading.value = false }
}
onMounted(load)
</script>
<template><PublicShell><ContentHero eyebrow="THE PLAY JOURNAL" title="Make room for play." subtitle="把灵感，带进每一天" note="一点户外时光，一次新的尝试。发现 WEMOVE 的玩法、故事与小小指南。" icon="heart" /><section class="content-public"><form class="content-filters" @submit.prevent="load(1)"><label>寻找灵感<input v-model="keyword" type="search" class="wm-search-input" placeholder="搜索标题或内容" maxlength="100" /></label><label>文章分类<input v-model="category" placeholder="例如：玩法灵感" maxlength="50" /></label><button class="primary-button" type="submit">查找文章</button><button v-if="keyword || category" class="secondary-button" type="button" @click="keyword = ''; category = ''; load(1)">清除筛选</button></form><ContentState :loading="loading" :error="error" :empty="!result?.items.length" empty-title="这里还在酝酿新的灵感" @retry="load()"><div class="content-article-grid"><article v-for="(article, index) in result?.items" :key="article.id" class="content-article-card"><RouterLink class="content-article-cover" :to="`/articles/${article.id}`" :aria-label="`阅读：${article.title}`"><img v-if="articleCover(article)" :src="articleCover(article)" alt="" loading="lazy" /><div v-else class="content-cover-doodle" aria-hidden="true">{{ ['✺', '↗', '✳'][index % 3] }}<small>WEMOVE JOURNAL</small></div></RouterLink><div class="content-article-copy"><p class="content-kicker">{{ article.category || '品牌故事' }} <span>{{ formatDate(article.publishedAt) }}</span></p><h2><RouterLink :to="`/articles/${article.id}`">{{ article.title }}</RouterLink></h2><p>{{ article.summary }}</p><RouterLink class="content-text-link" :to="`/articles/${article.id}`">慢慢读一读 <span aria-hidden="true">↗</span></RouterLink></div></article></div><ContentPager v-if="result" :meta="result.meta" @change="load" /></ContentState></section></PublicShell></template>
