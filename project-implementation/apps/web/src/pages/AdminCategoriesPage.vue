<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElDialog, ElMessage, ElMessageBox } from 'element-plus'
import SiteShell from '../components/SiteShell.vue'
import { api, ApiProblem, newIdempotencyKey } from '../services/http'
import type { Category } from '../types'

const categories = ref<Category[]>([])
const loading = ref(true)
const error = ref('')
const dialogOpen = ref(false)
const busy = ref(false)
const editing = ref<Category | null>(null)
const form = reactive({ name: '', description: '', sortOrder: 0, enabled: true })

async function load() {
  loading.value = true
  error.value = ''
  try { categories.value = (await api<Category[]>('/admin/categories')).data }
  catch (cause) { error.value = cause instanceof ApiProblem ? cause.problem.detail : '分类暂时无法加载。' }
  finally { loading.value = false }
}

function openCreate() {
  editing.value = null
  Object.assign(form, { name: '', description: '', sortOrder: (categories.value.at(-1)?.sortOrder ?? 0) + 10, enabled: true })
  dialogOpen.value = true
}

function openEdit(category: Category) {
  editing.value = category
  Object.assign(form, { name: category.name, description: category.description ?? '', sortOrder: category.sortOrder, enabled: category.enabled })
  dialogOpen.value = true
}

async function save() {
  busy.value = true
  try {
    const body = { name: form.name, description: form.description.trim() || null, sortOrder: form.sortOrder, enabled: form.enabled }
    if (editing.value) {
      await api<Category>(`/admin/categories/${editing.value.id}`, { method: 'PATCH', body: JSON.stringify({ ...body, expectedVersion: editing.value.version }) })
    } else {
      await api<Category>('/admin/categories', { method: 'POST', headers: { 'Idempotency-Key': newIdempotencyKey() }, body: JSON.stringify(body) })
    }
    ElMessage.success(editing.value ? '分类已更新' : '分类已创建')
    dialogOpen.value = false
    await load()
  } catch (cause) { ElMessage.error(cause instanceof ApiProblem ? cause.problem.detail : '分类保存失败。') }
  finally { busy.value = false }
}

async function remove(category: Category) {
  try {
    await ElMessageBox.confirm('只有未被任何商品引用的分类可以删除。此操作无法通过页面撤销。', `删除“${category.name}”`, { confirmButtonText: '删除', cancelButtonText: '返回', type: 'warning' })
    await api<{ id: string; deleted: boolean }>(`/admin/categories/${category.id}?expectedVersion=${category.version}`, { method: 'DELETE' })
    ElMessage.success('分类已删除')
    await load()
  } catch (cause) {
    if (cause === 'cancel' || cause === 'close') return
    ElMessage.error(cause instanceof ApiProblem ? cause.problem.detail : '分类删除失败。')
  }
}

onMounted(load)
</script>

<template>
  <SiteShell title="商品分类" eyebrow="OPERATIONS / TAXONOMY" admin>
    <section class="category-intro">
      <div><p>ONE LEVEL, CLEAR PURPOSE</p><h2>让每件商品都有清楚的归属</h2></div>
      <p>分类名称去除首尾空白后唯一。被商品引用的分类不能停用或删除，需要先迁移商品。</p>
      <button class="light-button" type="button" @click="openCreate">＋ 新建分类</button>
    </section>
    <div v-if="loading" class="state-panel"><span class="loader"></span><p>正在读取分类…</p></div>
    <div v-else-if="error" class="state-panel" role="alert"><h2>加载失败</h2><p>{{ error }}</p><button class="secondary-button" @click="load">重试</button></div>
    <div v-else class="category-grid">
      <article v-for="(category, index) in categories" :key="category.id" class="category-card">
        <span>{{ String(index + 1).padStart(2, '0') }}</span>
        <div><p>{{ category.enabled ? 'ENABLED' : 'DISABLED' }} · ORDER {{ category.sortOrder }}</p><h3>{{ category.name }}</h3><p>{{ category.description || '暂无分类说明' }}</p></div>
        <footer><small>版本 {{ category.version }}</small><div><button type="button" @click="openEdit(category)">编辑</button><button type="button" @click="remove(category)">删除</button></div></footer>
      </article>
      <button class="category-card category-add" type="button" @click="openCreate"><span>＋</span><strong>创建一个新分类</strong><small>单层分类 · 可排序 · 可停用</small></button>
    </div>

    <ElDialog v-model="dialogOpen" :title="editing ? '编辑分类' : '新建分类'" width="min(540px, 92vw)">
      <form class="identity-form" @submit.prevent="save">
        <label class="field"><span>分类名称</span><input v-model="form.name" minlength="2" maxlength="100" required /></label>
        <label class="field"><span>分类说明</span><textarea v-model="form.description" maxlength="500" rows="4"></textarea></label>
        <div class="field-pair"><label class="field"><span>显示顺序</span><input v-model.number="form.sortOrder" type="number" min="0" required /></label><label class="toggle-field"><input v-model="form.enabled" type="checkbox" /><span>启用公开筛选</span></label></div>
      </form>
      <template #footer><button class="secondary-button" @click="dialogOpen = false">取消</button><button class="primary-button" :disabled="busy || form.name.trim().length < 2" @click="save">{{ busy ? '正在保存…' : '保存分类' }}</button></template>
    </ElDialog>
  </SiteShell>
</template>
