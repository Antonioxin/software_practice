<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import HandwrittenLine from './HandwrittenLine.vue'

const firstLine = ref<InstanceType<typeof HandwrittenLine>>()
const secondLine = ref<InstanceType<typeof HandwrittenLine>>()
const caption = ref<InstanceType<typeof HandwrittenLine>>()
let frame = 0
let motion: MediaQueryList | undefined
const lines = () => [firstLine.value, secondLine.value, caption.value]
const finish = () => {
  cancelAnimationFrame(frame)
  frame = 0
  lines().forEach(line => line?.finish())
}
const onMotionChange = () => { if (motion?.matches) finish() }

onMounted(() => {
  motion = window.matchMedia?.('(prefers-reduced-motion: reduce)')
  motion?.addEventListener('change', onMotionChange)
  if (motion?.matches || typeof SVGPathElement === 'undefined' || !SVGPathElement.prototype.getTotalLength) {
    finish()
    return
  }

  firstLine.value?.prepare(180, 2100)
  secondLine.value?.prepare(2420, 1350)
  caption.value?.prepare(3930, 2250)
  const start = performance.now()
  const tick = (now: number) => {
    const elapsed = now - start
    lines().forEach(line => line?.draw(elapsed))
    if (elapsed < 6180) frame = requestAnimationFrame(tick)
    else finish()
  }
  frame = requestAnimationFrame(tick)
})

onBeforeUnmount(() => {
  cancelAnimationFrame(frame)
  motion?.removeEventListener('change', onMotionChange)
})
</script>

<template>
  <h1 id="home-title" class="home-title">
    <span class="home-title-line"><HandwrittenLine ref="firstLine" text="Life is better" language="english" /></span>{{ ' ' }}<span class="home-title-line"><HandwrittenLine ref="secondLine" text="at play." language="english" /></span>
  </h1>
  <div class="home-caption">
    <p class="home-chinese"><HandwrittenLine ref="caption" text="把日常，玩出新花样。" language="chinese" /></p>
  </div>
</template>

<style scoped>
.home-title { --handwriting-tracking: -3px; width: 100%; margin: 0 0 32px; color: var(--ink); font: 400 clamp(86px, 11.8vw, 168px)/.93 var(--font-hand); letter-spacing: var(--handwriting-tracking); }
.home-title-line { display: block; }
.home-title-line:last-child { color: var(--green); }
.home-caption { margin-top: 4px; }
.home-chinese { --handwriting-tracking: 2px; margin: 0; color: var(--ink); font-size: 24px; font-weight: 500; letter-spacing: var(--handwriting-tracking); line-height: 1.7; }
@media (max-width: 760px) {
  .home-title { --handwriting-tracking: -1.5px; font-size: clamp(64px, 13.8vw, 104px); line-height: 1.05; margin-bottom: 30px; }
  .home-chinese { --handwriting-tracking: 1px; font-size: 20px; }
}
</style>
