<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { MathUtils, PerspectiveCamera, Scene, Vector2 } from 'three'
import { CSS3DObject, CSS3DRenderer } from 'three/addons/renderers/CSS3DRenderer.js'

const surface = ref<HTMLDivElement>()
const paper = ref<HTMLDivElement>()
const target = new Vector2()
const current = new Vector2()
const shakeDuration = 520
const cameraDistance = 900
const viewportPadding = 24
let renderer: CSS3DRenderer | undefined
let scene: Scene | undefined
let camera: PerspectiveCamera | undefined
let object: CSS3DObject | undefined
let resizeObserver: ResizeObserver | undefined
let motionPreference: MediaQueryList | undefined
let frame = 0
let lastTime = 0
let enteredAt = -Infinity
let hovered = false
let lift = 0

function renderPose(wobble = 0) {
  if (!renderer || !scene || !camera || !object) return
  object.scale.setScalar(1 + lift * 0.04)
  object.position.set(wobble, lift * 4, 0)
  object.rotation.set(
    MathUtils.degToRad(current.y * 4),
    MathUtils.degToRad(current.x * 5),
    MathUtils.degToRad(-current.x * 0.65 + wobble * 1.15),
  )
  renderer.render(scene, camera)
}

function animate(now: number) {
  frame = 0
  const delta = Math.min((now - lastTime) / 1000, 0.05)
  lastTime = now
  const goal = hovered ? 1 : 0
  lift = MathUtils.damp(lift, goal, 14, delta)
  current.x = MathUtils.damp(current.x, target.x, 18, delta)
  current.y = MathUtils.damp(current.y, target.y, 18, delta)
  const phase = MathUtils.clamp((now - enteredAt) / shakeDuration, 0, 1)
  const wobble = Math.sin(phase * Math.PI * 6) * (1 - phase) ** 2
  const settled = Math.abs(lift - goal) < 0.0001 && current.distanceToSquared(target) < 0.000001
  if (settled && phase === 1) {
    lift = goal
    current.copy(target)
    renderPose()
    return
  }
  renderPose(wobble)
  frame = requestAnimationFrame(animate)
}

function startAnimation() {
  if (frame || !renderer) return
  lastTime = performance.now()
  frame = requestAnimationFrame(animate)
}

function trackPointer(event: PointerEvent) {
  if (event.pointerType !== 'mouse' || motionPreference?.matches || !surface.value || !renderer) return
  // Measure the stationary hit area, never the transformed paper.
  const bounds = surface.value.getBoundingClientRect()
  if (!bounds.width || !bounds.height) return
  // A resize or tab switch can happen while the pointer is still over the paper.
  // Resume on movement without requiring another pointerenter event.
  if (!hovered) {
    hovered = true
    enteredAt = performance.now()
  }
  target.set(
    MathUtils.clamp((event.clientX - bounds.left) / bounds.width * 2 - 1, -1, 1),
    MathUtils.clamp((event.clientY - bounds.top) / bounds.height * 2 - 1, -1, 1),
  )
  startAnimation()
}

function enter(event: PointerEvent) {
  trackPointer(event)
}

function leave() {
  if (!hovered) return
  hovered = false
  target.set(0, 0)
  startAnimation()
}

function resetMotion() {
  cancelAnimationFrame(frame)
  frame = 0
  hovered = false
  lift = 0
  enteredAt = -Infinity
  target.set(0, 0)
  current.set(0, 0)
  renderPose()
}

function resize() {
  if (!surface.value || !paper.value || !renderer || !camera) return
  const { width, height } = surface.value.getBoundingClientRect()
  if (!width || !height) return
  paper.value.style.width = `${width}px`
  paper.value.style.height = `${height}px`
  const viewWidth = width + viewportPadding * 2
  const viewHeight = height + viewportPadding * 2
  renderer.setSize(viewWidth, viewHeight)
  // One world unit equals one CSS pixel at the resting paper plane.
  camera.aspect = viewWidth / viewHeight
  camera.fov = MathUtils.radToDeg(2 * Math.atan(viewHeight / (2 * cameraDistance)))
  camera.updateProjectionMatrix()
  renderPose()
}

onMounted(() => {
  if (!surface.value || !paper.value || typeof ResizeObserver === 'undefined') return
  scene = new Scene()
  camera = new PerspectiveCamera(45, 1, 1, 2000)
  camera.position.z = cameraDistance
  renderer = new CSS3DRenderer()
  renderer.domElement.className = 'polaroid-motion-renderer'
  // Keep room for the lifted corners while clipping the camera's empty overflow.
  Object.assign(renderer.domElement.style, { position: 'absolute', left: `-${viewportPadding}px`, top: `-${viewportPadding}px`, pointerEvents: 'none' })
  object = new CSS3DObject(paper.value)
  object.element.style.pointerEvents = 'none'
  scene.add(object)
  surface.value.appendChild(renderer.domElement)
  motionPreference = window.matchMedia('(prefers-reduced-motion: reduce)')
  motionPreference.addEventListener('change', resetMotion)
  window.addEventListener('blur', resetMotion)
  document.addEventListener('visibilitychange', resetMotion)
  resizeObserver = new ResizeObserver(resize)
  resizeObserver.observe(surface.value)
  resize()
})

onBeforeUnmount(() => {
  cancelAnimationFrame(frame)
  resizeObserver?.disconnect()
  motionPreference?.removeEventListener('change', resetMotion)
  window.removeEventListener('blur', resetMotion)
  document.removeEventListener('visibilitychange', resetMotion)
  // Return Vue's slot to its original parent before disposing the renderer DOM.
  if (object) scene?.remove(object)
  if (paper.value) surface.value?.appendChild(paper.value)
  renderer?.domElement.remove()
})
</script>

<template>
  <div ref="surface" class="polaroid-motion" @pointerenter="enter" @pointermove="trackPointer" @pointerleave="leave" @pointercancel="leave">
    <div ref="paper" class="polaroid-motion-paper"><slot /></div>
  </div>
</template>

<style scoped>
.polaroid-motion { position: relative; width: 100%; aspect-ratio: 1149 / 1369; }
.polaroid-motion-paper { width: 100%; transform-origin: center; pointer-events: none; }
</style>
