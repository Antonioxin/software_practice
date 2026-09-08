<script setup lang="ts">
import SketchIcon from '../../components/SketchIcon.vue'
import { computed, ref, watch } from 'vue'
import ContentBody from './ContentBody'
import ArticleTextField from './ArticleTextField.vue'
import { articleIllustrations, blockId, parseArticleBlocks, serializeArticleBlocks, type ArticleBlock, type ImageBlock } from './articleBlocks'
import { safeImage } from './bodyMarkup'
import type { Media } from './types'
const model = defineModel<string>({ required: true })
const props = withDefaults(defineProps<{ id: string; label: string; required?: boolean; maxlength?: number; textLimit?: number; article?: boolean; media?: Media[] }>(), { article: false, media: () => [] })
const visibleLength = computed(() => { const doc = new DOMParser().parseFromString(model.value, 'text/html'); doc.querySelectorAll('script, style, iframe, object, embed, svg, math, form').forEach(node => node.remove()); return [...(doc.body.textContent ?? '').trim()].length })
const input = ref<HTMLTextAreaElement | null>(null)
const preview = ref(props.article)
const sourceMode = ref(false)
const blocks = ref<ArticleBlock[]>(parseArticleBlocks(model.value))
const operation = ref('')
const pickerId = ref('')
const availableMedia = computed(() => props.media.filter(image => safeImage(`/api/v1/media/${image.id}/content`)))
let serialized = model.value
watch(model, value => {
  if (value === serialized) return
  serialized = value
  blocks.value = parseArticleBlocks(value)
})
function persist() { serialized = serializeArticleBlocks(blocks.value); model.value = serialized }
function wrap(before: string, after: string) {
  const field = input.value; if (!field) return
  const from = field.selectionStart, to = field.selectionEnd
  model.value = `${model.value.slice(0, from)}${before}${model.value.slice(from, to) || '填写文字'}${after}${model.value.slice(to)}`
  field.focus()
}
function addText(after = blocks.value.length - 1) {
  blocks.value.splice(after + 1, 0, { id: blockId(), type: 'text', html: '' })
  persist(); operation.value = '已添加文字块。'
}
function addImage(after = blocks.value.length - 1) {
  const preset = articleIllustrations[0]!
  const image: ImageBlock = { id: blockId(), type: 'image', layout: 'image-left', src: preset.src, alt: preset.alt, caption: '', title: '', html: '' }
  blocks.value.splice(after + 1, 0, image)
  pickerId.value = image.id; persist(); operation.value = '已添加图文块，可以选择图片和排版。'
}
function move(index: number, direction: number) {
  const next = index + direction
  if (next < 0 || next >= blocks.value.length) return
  const block = blocks.value.splice(index, 1)[0]!
  blocks.value.splice(next, 0, block); persist(); operation.value = `已将区块移至第 ${next + 1} 位。`
}
function remove(index: number) { blocks.value.splice(index, 1); persist(); operation.value = '已移除区块，保存文章后生效。' }
function choose(block: ImageBlock, src: string, alt: string) {
  if (!safeImage(src)) return
  block.src = src; block.alt = alt; persist()
}
function chooseMedia(block: ImageBlock, event: Event) {
  const image = availableMedia.value.find(item => item.id === (event.target as HTMLSelectElement).value)
  if (image) choose(block, `/api/v1/media/${image.id}/content`, image.altText || image.filename)
}
function imageName(src: string) { return articleIllustrations.find(image => image.src === src)?.label || '已上传的图片' }
function mediaId(src: string) { return /^\/api\/v1\/media\/([^/]+)\/content$/.exec(src)?.[1] || '' }
</script>
<template>
  <div class="content-editor" :class="{ 'article-editor': article }">
    <div class="article-editor-heading"><label :for="article && !sourceMode ? `${id}-blocks` : id">{{ label }}</label><button v-if="article" type="button" class="article-source-toggle" :aria-pressed="sourceMode" @click="sourceMode = !sourceMode">{{ sourceMode ? '返回图文编辑' : '编辑 HTML 源码' }}</button></div>
    <template v-if="article && !sourceMode">
      <p class="article-editor-hint">像搭积木一样编排文章：先写一段故事，再配一张玩法插图。</p>
      <div :id="`${id}-blocks`" class="article-blocks" :aria-label="label">
        <div v-if="!blocks.length" class="article-editor-empty"><span>从第一段故事开始</span><p>添加文字或配图，让玩法更容易被看见。</p></div>
        <section v-for="(block, index) in blocks" :key="block.id" class="article-editor-block" :data-block-type="block.type">
          <header class="article-block-heading"><strong><span>{{ String(index + 1).padStart(2, '0') }}</span>{{ block.type === 'image' ? '图文块' : '文字块' }}</strong><div><button type="button" :disabled="index === 0" :aria-label="`上移第 ${index + 1} 块`" @click="move(index, -1)"><SketchIcon name="arrow-left" :size="16" class="article-move-up" />上移</button><button type="button" :disabled="index === blocks.length - 1" :aria-label="`下移第 ${index + 1} 块`" @click="move(index, 1)"><SketchIcon name="arrow-left" :size="16" class="article-move-down" />下移</button><button type="button" :aria-label="`移除第 ${index + 1} 块`" @click="remove(index)"><SketchIcon name="trash" :size="16" />移除</button></div></header>
          <ArticleTextField v-if="block.type === 'text'" :id="`${id}-${block.id}`" v-model="block.html" label="段落内容" @update:model-value="persist" />
          <template v-else>
            <div class="article-layout-options" role="group" aria-label="图片排版"><button v-for="(name, layout) in { 'image-left': '图左文右', 'image-right': '图右文左', full: '通栏图片' }" :key="layout" type="button" :aria-pressed="block.layout === layout" @click="block.layout = layout; persist()"><span class="article-layout-icon" :data-layout="layout"><i /><i /></span>{{ name }}</button></div>
            <div class="article-image-fields"><img class="article-selected-image" :src="block.src" :alt="block.alt" /><div><label>替代文字<input v-model="block.alt" maxlength="300" placeholder="描述图片中的人物、产品与动作" @input="persist" /></label><small>图片无法显示或使用读屏时，会读出这段描述。</small><label>图注<input v-model="block.caption" maxlength="300" placeholder="可选，显示在图片下方" @input="persist" /></label></div></div>
            <details class="article-image-picker" :open="pickerId === block.id"><summary>更换插图 · {{ imageName(block.src) }}</summary><div class="article-preset-grid"><button v-for="illustration in articleIllustrations" :key="illustration.key" type="button" :aria-pressed="block.src === illustration.src" :aria-label="`选择${illustration.label}`" @click="choose(block, illustration.src, illustration.alt)"><img :src="illustration.src" :alt="illustration.alt" loading="lazy" /><span>{{ illustration.label }}</span></button></div><label>或使用已上传的图片<select :value="mediaId(block.src)" @change="chooseMedia(block, $event)"><option value="">选择媒体库图片</option><option v-for="image in availableMedia" :key="image.id" :value="image.id">{{ image.altText || image.filename }}</option></select></label><small v-if="!availableMedia.length">媒体库暂无图片，可到“文件与媒体”上传后重新加载资源。</small></details>
            <label class="article-image-title">{{ block.layout === 'full' ? '图片下方标题（可选）' : '配文标题（可选）' }}<input v-model="block.title" maxlength="200" placeholder="例如：让每一步，都成为一场冒险" @input="persist" /></label>
            <ArticleTextField :id="`${id}-${block.id}`" v-model="block.html" :label="block.layout === 'full' ? '图片下方文字（可选）' : '图片旁的文字'" placeholder="说说这张图片里的玩法，也可以写下小贴士。" @update:model-value="persist" />
          </template>
          <div class="article-insert-after"><button type="button" @click="addText(index)"><SketchIcon name="plus" :size="16" />在此后加文字</button><button type="button" @click="addImage(index)"><SketchIcon name="plus" :size="16" />在此后加图文</button></div>
        </section>
      </div>
      <div class="article-add-blocks"><button type="button" @click="addText()"><SketchIcon name="plus" :size="18" />添加文字块</button><button type="button" @click="addImage()"><SketchIcon name="plus" :size="18" />添加图文块</button></div>
      <span class="article-operation" role="status" aria-live="polite">{{ operation }}</span>
    </template>
    <template v-else>
      <div class="content-editor-tools"><button type="button" @click="wrap('<h2>', '</h2>')">小标题</button><button type="button" @click="wrap('<strong>', '</strong>')">加粗</button><button type="button" @click="wrap('<p>', '</p>')">段落</button><button type="button" @click="wrap('<ul><li>', '</li></ul>')">列表</button><button v-if="!article" type="button" :aria-pressed="preview" @click="preview = !preview">{{ preview ? '收起预览' : '预览排版' }}</button></div>
      <textarea :id="id" ref="input" v-model="model" :required="required" :maxlength="maxlength || 50000" rows="8" placeholder="写下内容，可使用工具栏添加段落、标题与列表。" />
    </template>
    <small v-if="textLimit" :class="{ 'content-limit-exceeded': visibleLength > textLimit }">正文文字约 {{ visibleLength }} / {{ textLimit }} 字</small>
    <div v-if="article" class="article-preview-heading"><strong>阅读效果</strong><button type="button" :aria-pressed="preview" @click="preview = !preview">{{ preview ? '收起预览' : '展开实时预览' }}</button></div>
    <ContentBody v-if="preview" :body="model" />
    <small v-if="!article">支持段落、列表、加粗与链接；不支持脚本、内嵌页面和任意样式。</small>
  </div>
</template>
<style scoped>
.article-editor-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.article-editor-heading > label { color: #5a6255; font-size: 12px; font-weight: 600; }
.article-source-toggle, .article-preview-heading button { padding: 0; border: 0; background: none; color: #677852; font: inherit; font-size: 11px; cursor: pointer; text-decoration: underline; text-underline-offset: 3px; }
.article-editor-hint { margin: 0 0 8px; color: #818672; font-size: 12px; line-height: 1.75; }
.article-blocks { display: grid; gap: 16px; }
.article-editor-empty { border-radius: var(--radius-card, 20px); padding: 32px 20px; border: 1px dashed #bcc8aa; text-align: center; background: #f6f7ef; color: #5e7149; }
.article-editor-empty p { color: #858b7b; font-size: 12px; margin-bottom: 0; }
.article-editor-block { display: grid; gap: 16px; padding: 18px; border: 1px solid #d8dfcc; background: #fffef9; border-radius: var(--radius-card, 20px); min-width: 0; }
.article-block-heading, .article-block-heading > div { display: flex; align-items: center; gap: 10px; }
.article-block-heading { justify-content: space-between; padding-bottom: 12px; border-bottom: 1px solid #e5e8de; }
.article-block-heading strong { font-size: 13px; color: #526440; }
.article-block-heading strong span { margin-right: 9px; color: #a1ab91; font-size: 12px; }
.article-block-heading button, .article-insert-after button { display: inline-flex; align-items: center; gap: 4px; padding: 3px 0; border: 0; background: none; color: #70835c; font: inherit; font-size: 11px; cursor: pointer; }
.article-move-up { transform: rotate(90deg); }
.article-move-down { transform: rotate(-90deg); }
.article-block-heading button:disabled { opacity: .35; cursor: default; }
.article-layout-options { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.article-layout-options button { display: flex; align-items: center; justify-content: center; gap: 9px; padding: 9px 4px; border: 1px solid #d8dfcc; border-radius: var(--radius-control, 12px); color: #748265; background: #fdfdf7; font: inherit; font-size: 11px; cursor: pointer; }
.article-layout-options button[aria-pressed="true"] { border-color: #7f9467; color: #4d633c; background: #edf2e3; }
.article-layout-icon { display: flex; align-items: center; gap: 3px; width: 29px; height: 21px; }
.article-layout-icon i:first-child { width: 15px; height: 18px; border-radius: 1px; background: #a7b890; }
.article-layout-icon i:last-child { width: 11px; height: 15px; background: repeating-linear-gradient(to bottom, #a7b890 0 2px, transparent 2px 5px); }
.article-layout-icon[data-layout="image-right"] { flex-direction: row-reverse; }
.article-layout-icon[data-layout="full"] i:first-child { width: 29px; }
.article-layout-icon[data-layout="full"] i:last-child { display: none; }
.article-image-fields { display: grid; grid-template-columns: 145px minmax(0, 1fr); gap: 18px; align-items: start; }
.article-selected-image { width: 145px; aspect-ratio: 1; object-fit: contain; background: #f4f3e9; border-radius: var(--radius-control, 12px); }
.article-image-fields > div, .article-editor-block label { display: grid; gap: 8px; }
.article-editor-block label { color: #627153; font-size: 12px; font-weight: 600; }
.article-image-fields small { margin-top: -4px; }
.article-image-picker { padding: 12px; border: 1px solid #dde3d4; border-radius: var(--radius-card, 20px); }
.article-image-picker summary { color: #677953; font-size: 12px; cursor: pointer; }
.article-image-picker > label { margin-top: 14px; }
.article-preset-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 8px; margin-top: 14px; }
.article-preset-grid button { display: grid; padding: 5px; border: 1px solid #e1e6d9; border-radius: var(--radius-control, 12px); background: #fffef9; color: #69775d; cursor: pointer; min-width: 0; }
.article-preset-grid button[aria-pressed="true"] { border-color: #7b9560; background: #edf2e3; box-shadow: 0 0 0 1px #7b9560; }
.article-preset-grid img { border-radius: var(--radius-small, 8px); width: 100%; aspect-ratio: 1; object-fit: contain; }
.article-preset-grid span { padding: 8px 1px 3px; font-size: 10px; line-height: 1.5; }
.article-insert-after { display: flex; justify-content: flex-end; gap: 14px; padding-top: 4px; }
.article-add-blocks { display: flex; gap: 10px; padding-top: 5px; }
.article-add-blocks button { display: inline-flex; align-items: center; justify-content: center; gap: 6px; flex: 1; padding: 12px; border: 1px dashed #9bac86; border-radius: var(--radius-control, 12px); background: #f5f7ed; color: #5b7047; font: inherit; font-size: 12px; cursor: pointer; }
.article-operation { color: #738362; font-size: 11px; }
.article-operation:empty { display: none; }
.article-preview-heading { display: flex; justify-content: space-between; margin-top: 12px; padding-top: 18px; border-top: 1px solid #dce1d2; font-size: 12px; color: #61764c; }
@media (max-width: 560px) { .article-editor-block { padding: 12px; } .article-image-fields { grid-template-columns: 1fr; } .article-selected-image { width: 100%; max-height: 220px; } .article-preset-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .article-layout-options button { flex-direction: column; gap: 5px; } .article-block-heading > div { gap: 7px; } }
</style>
