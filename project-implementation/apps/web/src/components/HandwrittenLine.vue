<script setup lang="ts">
import { ref, useId } from 'vue'
import glyphData from '../features/home/heroGlyphs.json'
import { englishStrokes } from '../features/home/englishStrokes'
import { chineseStrokes } from '../features/home/chineseStrokes'

const props = defineProps<{ text: string; language: 'english' | 'chinese' }>()
const root = ref<HTMLElement>()
const id = `handwriting-${useId()}`
const font: Record<string, { advance: number; path: string }> = glyphData[props.language]
const trajectories = props.language === 'english' ? englishStrokes : chineseStrokes
const tracking = props.language === 'english' ? -18 : 1000 / 12
let cursor = 0
let advance = 0
const glyphs = [...props.text].map((character, index) => {
  const glyph = font[character]!
  const result = { ...glyph, character, x: cursor, id: `${id}-${index}`, strokes: trajectories[character] ?? [] }
  cursor += glyph.advance + tracking
  advance += glyph.advance
  return result
})
// Use exported advance widths to reserve the final layout even before webfonts load.
const lineWidth = `calc(${advance / 1000}em + ${props.text.length} * var(--handwriting-tracking, 0px))`

type Pen = { element: SVGPathElement; start: number; duration: number }
let pens: Pen[] = []
let glyphEnds: { element: SVGPathElement; end: number }[] = []
let duration = 0

// Measure real centerline lengths once. The filled font outlines are never stroked:
// these separate pen paths uncover the existing ink through a luminance mask.
function prepare(start: number, lineDuration: number) {
  if (!root.value) return
  const strokes = [...root.value.querySelectorAll<SVGPathElement>('[data-pen]')]
  const lengths = strokes.map(path => Math.max(80, path.getTotalLength()))
  const lift = props.language === 'english' ? 22 : 10
  const totalLength = lengths.reduce((sum, length) => sum + length, 0)
  const drawingTime = lineDuration - lift * Math.max(0, strokes.length - 1)
  let time = start
  pens = strokes.map((element, index) => {
    const pen = { element, start: time, duration: drawingTime * lengths[index]! / totalLength }
    time += pen.duration + lift
    return pen
  })
  duration = start + lineDuration
  let strokeIndex = 0
  glyphEnds = [...root.value.querySelectorAll<SVGPathElement>('[data-ink]')].map(element => {
    strokeIndex += Number(element.dataset.strokes)
    const lastPen = pens[strokeIndex - 1]
    return { element, end: lastPen ? lastPen.start + lastPen.duration : start }
  })
}

function draw(time: number) {
  for (const pen of pens) {
    const progress = Math.max(0, Math.min(1, (time - pen.start) / pen.duration))
    pen.element.setAttribute('visibility', progress > 0 ? 'visible' : 'hidden')
    pen.element.setAttribute('stroke-dashoffset', String(1 - progress))
  }
  // Release each completed mask to keep tiny textured edges of the source font intact.
  for (const glyph of glyphEnds) {
    if (time >= glyph.end) glyph.element.removeAttribute('mask')
  }
  if (time >= duration) finish()
}

function finish() {
  root.value?.querySelectorAll('[data-ink]').forEach(element => element.removeAttribute('mask'))
}

defineExpose({ prepare, draw, finish })
</script>

<template>
  <span ref="root" class="handwritten-line" :class="`handwritten-line--${language}`" :style="{ width: lineWidth }">
    <span class="handwritten-copy">{{ text }}</span>
    <svg :viewBox="`0 0 ${cursor} 1000`" preserveAspectRatio="none" aria-hidden="true" focusable="false">
      <defs>
        <mask v-for="glyph in glyphs" :id="glyph.id" :key="glyph.id" maskUnits="userSpaceOnUse" x="-150" y="-150" width="1500" height="1600" style="mask-type: luminance">
          <path v-for="(stroke, index) in glyph.strokes" :key="index" data-pen :d="stroke.d" fill="none" stroke="white" :stroke-width="stroke.width" stroke-linecap="round" stroke-linejoin="round" pathLength="1" stroke-dasharray="1 1" stroke-dashoffset="1" visibility="hidden" />
        </mask>
      </defs>
      <g v-for="glyph in glyphs" :key="glyph.id" :transform="`translate(${glyph.x} 0)`">
        <path v-if="glyph.path" data-ink :data-strokes="glyph.strokes.length" :d="glyph.path" fill="currentColor" :mask="`url(#${glyph.id})`" />
      </g>
    </svg>
  </span>
</template>

<style scoped>
.handwritten-line { position: relative; display: inline-block; vertical-align: top; white-space: nowrap; }
.handwritten-copy { display: block; overflow: hidden; opacity: 0; }
.handwritten-line svg { position: absolute; left: 0; top: calc(50% - .57em); width: 100%; height: 1em; overflow: visible; pointer-events: none; }
.handwritten-line--chinese svg { top: calc(50% - .564em); }
</style>
