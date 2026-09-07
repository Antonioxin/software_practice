<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PublicShell from '../../components/PublicShell.vue'
import { getChannels } from '../../features/dealership/api'
import type { Channel } from '../../features/dealership/types'
import '../../features/dealership/style.css'

const channels = ref<Channel[]>([])
const country = ref('')
const city = ref('')
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true; error.value = ''
  try {
    const query = new URLSearchParams()
    if (country.value.trim()) query.set('countryOrRegion', country.value.trim())
    if (city.value.trim()) query.set('city', city.value.trim())
    channels.value = (await getChannels(query.toString())).items
  } catch (cause) { error.value = (cause as Error).message } finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <PublicShell>
    <section class="dealer-public-hero">
      <p>OFFICIAL CHANNELS</p><h1>在你所在的城市<br />找到 WEMOVE</h1>
      <span>这里只展示管理员明确发布的公开渠道；合作申请中的联系方式不会自动公开。</span>
    </section>
    <section class="dealer-public-content">
      <form class="dealer-filter" @submit.prevent="load">
        <label>国家／地区<input v-model="country" placeholder="例如：中国" /></label>
        <label>城市<input v-model="city" placeholder="例如：上海市" /></label>
        <button class="primary-button" type="submit">查询渠道</button>
      </form>
      <p v-if="error" class="error-summary" role="alert">{{ error }}</p>
      <div v-if="loading" class="dealer-empty">正在查询公开渠道…</div>
      <div v-else-if="!channels.length" class="dealer-empty"><h2>暂时没有匹配渠道</h2><p>可以调整地区条件，或通过站内商品页完成模拟零售。</p></div>
      <div v-else class="channel-grid">
        <article v-for="channel in channels" :key="channel.id" class="channel-card">
          <p>{{ channel.countryOrRegion }} · {{ channel.city }}</p><h2>{{ channel.name }}</h2>
          <address>{{ channel.address }}</address><a :href="`tel:${channel.phone}`">{{ channel.phone }}</a>
          <a v-if="channel.website" :href="channel.website" target="_blank" rel="noreferrer">访问渠道网站 ↗</a>
        </article>
      </div>
    </section>
  </PublicShell>
</template>
