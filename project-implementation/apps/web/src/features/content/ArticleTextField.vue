<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { editableHtml, escapeHtml } from './articleBlocks'
const props = defineProps<{ modelValue: string; label: string; id: string; placeholder?: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
const field = ref<HTMLDivElement | null>(null)
function sync() {
  const html = editableHtml(props.modelValue)
  if (field.value && field.value.innerHTML !== html) field.value.innerHTML = html
}
function update() { if (field.value) emit('update:modelValue', editableHtml(field.value.innerHTML)) }
function format(command: string, value?: string) {
  field.value?.focus()
  document.execCommand?.(command, false, value)
  update()
}
function paste(event: ClipboardEvent) {
  const clipboard = event.clipboardData
  if (!clipboard || !field.value) return
  const html = clipboard.getData('text/html') || escapeHtml(clipboard.getData('text/plain')).replace(/\n/g, '<br>')
  const selection = window.getSelection()
  if (!selection?.rangeCount) return
  const range = selection.getRangeAt(0)
  if (!field.value.contains(range.commonAncestorContainer)) return
  range.deleteContents()
  const fragment = range.createContextualFragment(editableHtml(html))
  const last = fragment.lastChild
  range.insertNode(fragment)
  if (last) { range.setStartAfter(last); range.collapse(true); selection.removeAllRanges(); selection.addRange(range) }
  update()
}
onMounted(sync)
watch(() => props.modelValue, () => { if (document.activeElement !== field.value) sync() })
</script>
<template>
  <div class="article-text-field">
    <label :for="id">{{ label }}</label>
    <div class="article-text-tools" role="toolbar" :aria-label="`${label}文字格式`">
      <button type="button" @mousedown.prevent @click="format('formatBlock', 'p')">正文</button>
      <button type="button" @mousedown.prevent @click="format('formatBlock', 'h3')">小标题</button>
      <button type="button" @mousedown.prevent @click="format('bold')"><strong>加粗</strong></button>
      <button type="button" @mousedown.prevent @click="format('insertUnorderedList')">列表</button>
    </div>
    <div :id="id" ref="field" class="article-rich-input" contenteditable="true" role="textbox" aria-multiline="true" :aria-label="label" :data-placeholder="placeholder || '直接输入文字，也可以粘贴已有的段落与列表。'" @input="update" @paste.prevent="paste" @drop.prevent />
  </div>
</template>
<style scoped>
.article-text-field { display: grid; gap: 8px; }
.article-text-field > label { color: #5a6255; font-size: 12px; font-weight: 600; }
.article-text-tools { display: flex; flex-wrap: wrap; gap: 4px; }
.article-text-tools button { padding: 5px 10px; color: #566548; background: #f2f4e9; border: 1px solid #d5d9c8; border-radius: var(--radius-control, 12px); font: inherit; font-size: 11px; cursor: pointer; }
.article-rich-input { min-height: 118px; padding: 12px 14px; background: #fdfcf7; border: 1px solid #c8cebf; border-radius: var(--radius-control, 12px); outline-color: #74875c; color: #363e31; line-height: 1.8; font-size: 14px; overflow-wrap: anywhere; }
.article-rich-input:empty::before { content: attr(data-placeholder); color: #969b8f; pointer-events: none; }
.article-rich-input :deep(p) { margin: 0 0 10px; }
.article-rich-input :deep(h2), .article-rich-input :deep(h3) { margin: 10px 0; }
.article-rich-input :deep(img) { border-radius: var(--radius-control, 12px); max-width: 100%; max-height: 240px; object-fit: contain; }
</style>
