<script setup lang="ts">
import { onMounted, ref } from 'vue'
import SiteShell from '../../components/SiteShell.vue'
import { getApplications } from '../../features/dealership/api'
import { applicationLabels, type DealerApplication } from '../../features/dealership/types'
import '../../features/dealership/style.css'
const applications = ref<DealerApplication[]>([]); const status = ref('PENDING'); const loading = ref(true); const error = ref('')
async function load() { loading.value = true; try { applications.value = (await getApplications(true, status.value ? `status=${status.value}` : '')).items } catch (cause) { error.value = (cause as Error).message } finally { loading.value = false } }
onMounted(load)
</script>
<template><SiteShell title="合作申请审核" admin eyebrow="OPERATIONS / APPLICATIONS"><section class="paper-section"><div class="dealer-toolbar"><select v-model="status" @change="load"><option value="">全部状态</option><option v-for="(label, value) in applicationLabels" :key="value" :value="value">{{ label }}</option></select></div><p v-if="error" class="error-summary">{{ error }}</p><div v-if="loading" class="dealer-empty">正在读取申请队列…</div><div v-else-if="!applications.length" class="dealer-empty"><h2>当前队列为空</h2></div><div v-else class="dealer-list"><article v-for="item in applications" :key="item.id"><div><p>{{ item.applicationNumber }}</p><h2>{{ item.versions.at(-1)?.companyName }}</h2><span>{{ item.versions.at(-1)?.countryOrRegion }} · {{ item.versions.at(-1)?.city }} · v{{ item.currentContentVersion }}</span></div><span v-if="item.suspectedDuplicate" class="dealer-warning">疑似同名企业</span><span class="dealer-status" :data-status="item.status">{{ applicationLabels[item.status] }}</span><RouterLink class="secondary-button" :to="`/admin/dealer-applications/${item.id}`">查看审核</RouterLink></article></div></section></SiteShell></template>
