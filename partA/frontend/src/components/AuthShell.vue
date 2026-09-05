<script setup lang="ts">
defineProps<{ eyebrow: string; title: string; description: string }>()
</script>

<template>
  <div class="auth-page">
    <header class="public-header">
      <RouterLink class="brand" to="/login" aria-label="WEMOVE 身份中心">
        <img src="/brand-logo.png" alt="WEMOVE SPORTS" />
        <span>WeMove</span>
      </RouterLink>
      <nav aria-label="账户导航">
        <RouterLink to="/login">登录</RouterLink>
        <RouterLink to="/register">注册账户</RouterLink>
      </nav>
    </header>

    <main id="main-content" class="auth-main">
      <section class="story-panel" aria-label="WEMOVE 品牌介绍">
        <div class="track-orbit orbit-one" aria-hidden="true"></div>
        <div class="track-orbit orbit-two" aria-hidden="true"></div>
        <div class="wood-block block-one" aria-hidden="true"></div>
        <div class="wood-block block-two" aria-hidden="true"></div>
        <div class="story-content">
          <span class="story-rule"></span>
          <p class="story-kicker">WEMOVE SPORTS</p>
          <h1>让每一次动手，<br />都成为理解世界的开始。</h1>
          <p>这里是面向成年消费者与采购人员的业务门户。您的资料、订单和合作记录都会受到严格的身份边界保护。</p>
        </div>
        <div class="story-foot"><span>EST. 2026</span><i></i><span>WOOD · PLAY · LEARN</span></div>
      </section>

      <section class="form-panel">
        <div class="form-wrap">
          <p class="eyebrow">{{ eyebrow }}</p>
          <h2>{{ title }}</h2>
          <p class="lede">{{ description }}</p>
          <slot />
        </div>
      </section>
    </main>

    <footer class="public-footer">
      <span>WEMOVE SPORTS</span>
      <span>© 2026 WeMove · 本项目为课程模拟系统</span>
    </footer>
  </div>
</template>
