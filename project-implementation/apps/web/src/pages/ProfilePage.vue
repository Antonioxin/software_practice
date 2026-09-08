<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import PublicShell from '../components/PublicShell.vue'
import SketchIcon from '../components/SketchIcon.vue'
import ProfilePlayIllustration from '../components/ProfilePlayIllustration.vue'
import { api, ApiProblem } from '../services/http'
import { useSessionStore } from '../stores/session'
import type { Actor } from '../types'

const session = useSessionStore()
const form = reactive({ nickname: '', phone: '' })
const fields = reactive<Record<string, string>>({})
const editing = ref(false)
const busy = ref(false)
const error = ref('')
const success = ref('')
const nicknameInput = ref<HTMLInputElement>()
const editButton = ref<HTMLButtonElement>()
const displayName = computed(() => session.actor?.nickname || 'WEMOVE 用户')
const initial = computed(() => Array.from(displayName.value)[0])
const isDealer = computed(() => session.actor?.derivedIdentity === 'DEALER')
const canEdit = computed(() => session.actor?.capabilities.includes('ACCOUNT_PROFILE_WRITE') ?? false)
const personalLinks = [
  { to: '/account/profile', label: '个人资料', icon: 'user' },
  { to: '/account/orders', label: '我的订单', icon: 'orders' },
  { to: '/cart', label: '我的购物车', icon: 'cart' },
  { to: '/account/tickets', label: '我的咨询', icon: 'chat' },
]

function restoreForm() {
  form.nickname = session.actor?.nickname ?? ''
  form.phone = session.actor?.phone ?? ''
}
function clearFeedback() {
  error.value = ''
  success.value = ''
  Object.keys(fields).forEach(key => delete fields[key])
}
watch(() => session.actor, () => { if (!editing.value) restoreForm() }, { immediate: true })

async function beginEditing() {
  if (!canEdit.value || busy.value) return
  restoreForm()
  clearFeedback()
  editing.value = true
  await nextTick()
  nicknameInput.value?.focus()
}
async function cancelEditing() {
  if (busy.value) return
  restoreForm()
  clearFeedback()
  editing.value = false
  await nextTick()
  editButton.value?.focus()
}
async function save() {
  if (busy.value || !editing.value || !canEdit.value) return
  busy.value = true
  clearFeedback()
  try {
    const actor = (await api<Actor>('/account/profile', { method: 'PATCH', body: JSON.stringify(form) })).data
    session.replace(actor)
    restoreForm()
    editing.value = false
    success.value = '个人资料已保存。'
    await nextTick()
    editButton.value?.focus()
  } catch (cause) {
    if (cause instanceof ApiProblem) {
      error.value = cause.problem.detail
      cause.problem.errors?.forEach(item => { fields[item.field] = item.message })
    } else error.value = '保存结果暂未确认，请刷新后核对。'
  } finally { busy.value = false }
}
</script>

<template>
  <PublicShell class="profile-site">
    <div class="account-page">
      <header class="account-heading">
        <div>
          <p class="account-eyebrow">MY WEMOVE / 属于你的小天地</p>
          <h1>我的账户<span aria-hidden="true">.</span></h1>
          <p class="account-intro">照顾好日常的小事，把时间留给喜欢的生活。</p>
        </div>
        <RouterLink class="account-back" to="/products"><SketchIcon name="arrow-left" :size="20" />继续探索商品</RouterLink>
      </header>

      <div class="account-layout">
        <aside class="account-sidebar" aria-label="个人中心">
          <section class="account-person" aria-labelledby="account-name">
            <div class="account-avatar" aria-hidden="true"><span>{{ initial }}</span><i><SketchIcon name="heart" :size="18" /></i></div>
            <h2 id="account-name">{{ displayName }}</h2>
            <p class="account-email">{{ session.actor?.email }}</p>
            <span v-if="session.actor" :class="['account-status', { 'is-disabled': session.actor.accountStatus === 'DISABLED' }]">
              <i aria-hidden="true"></i>{{ session.actor.accountStatus === 'ACTIVE' ? '账户已启用' : '账户已停用' }}
            </span>
          </section>
          <nav class="account-navigation" aria-label="个人中心导航">
            <p class="account-group-label">我的日常</p>
            <RouterLink v-for="link in personalLinks" :key="link.to" :to="link.to" :class="['account-nav-link', { 'is-current': link.to === '/account/profile' }]" :aria-current="link.to === '/account/profile' ? 'page' : undefined">
              <SketchIcon :name="link.icon" :size="23" /><span>{{ link.label }}</span><span class="account-chevron" aria-hidden="true">›</span>
            </RouterLink>
            <p class="account-group-label account-group-divider">一起成长</p>
            <RouterLink class="account-nav-link" to="/account/dealer-application"><SketchIcon name="ticket" :size="23" /><span>经销合作申请</span><span class="account-chevron" aria-hidden="true">›</span></RouterLink>
            <RouterLink class="account-nav-link" to="/account/inquiries"><SketchIcon name="wallet" :size="23" /><span>我的询价</span><span class="account-chevron" aria-hidden="true">›</span></RouterLink>
            <RouterLink v-if="isDealer" class="account-nav-link" to="/dealer/catalog"><SketchIcon name="grid" :size="23" /><span>经销专属目录</span><span class="account-chevron" aria-hidden="true">›</span></RouterLink>
          </nav>
          <RouterLink class="account-help" to="/contact"><SketchIcon name="help" :size="22" /><span>需要帮助？联系到我们</span><span aria-hidden="true">↗</span></RouterLink>
        </aside>

        <div class="account-content">
          <section class="account-information" aria-labelledby="profile-title">
            <header class="account-section-heading">
              <div><p class="account-eyebrow">PERSONAL INFORMATION</p><h2 id="profile-title">个人资料</h2><p>关于你，从一个熟悉的称呼开始。</p></div>
              <button v-if="!editing && canEdit" ref="editButton" class="account-edit-button" type="button" @click="beginEditing">
                <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m15 5 4 4M4 20l4-1L20 7a2.8 2.8 0 0 0-4-4L4 15z" /></svg>编辑资料
              </button>
              <span v-else-if="editing" class="account-editing-label"><i aria-hidden="true"></i>正在编辑</span>
            </header>
            <form :class="['account-form', { 'is-editing': editing }]" aria-label="个人资料" :aria-busy="busy" @submit.prevent="save">
              <div v-if="error" class="account-feedback account-feedback-error error-summary" role="alert"><strong>未能保存</strong><span>{{ error }}</span></div>
              <p v-if="success" class="account-feedback account-feedback-success" role="status">{{ success }}</p>
              <div class="account-fields">
                <div class="account-field">
                  <label for="profile-nickname">昵称</label>
                  <input id="profile-nickname" ref="nicknameInput" v-model="form.nickname" autocomplete="nickname" minlength="2" maxlength="30" required :readonly="!editing" :tabindex="editing ? undefined : -1" :disabled="busy" :aria-invalid="fields.nickname ? true : undefined" :aria-describedby="fields.nickname ? 'profile-nickname-error' : 'profile-nickname-hint'" />
                  <span v-if="fields.nickname" id="profile-nickname-error" class="account-field-error field-error">{{ fields.nickname }}</span>
                  <span v-else id="profile-nickname-hint" class="account-field-hint">我们该如何称呼你？2—30 个字符。</span>
                </div>
                <div class="account-field">
                  <label for="profile-phone">联系电话 <span>选填</span></label>
                  <input id="profile-phone" v-model="form.phone" type="tel" autocomplete="tel" maxlength="40" :placeholder="editing ? '+86 138 0000 0000' : '暂未填写'" :readonly="!editing" :tabindex="editing ? undefined : -1" :disabled="busy" :aria-invalid="fields.phone ? true : undefined" :aria-describedby="fields.phone ? 'profile-phone-error' : 'profile-phone-hint'" />
                  <span v-if="fields.phone" id="profile-phone-error" class="account-field-error field-error">{{ fields.phone }}</span>
                  <span v-else id="profile-phone-hint" class="account-field-hint">方便有需要时与你取得联系。</span>
                </div>
                <div class="account-field account-field-wide">
                  <label for="profile-email">登录邮箱 <span class="account-fixed-label"><svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true"><rect x="5" y="10" width="14" height="11" rx="2" /><path d="M8 10V6a4 4 0 0 1 8 0v4" /></svg>不可修改</span></label>
                  <input id="profile-email" :value="session.actor?.email" type="email" autocomplete="email" disabled aria-describedby="profile-email-hint" />
                  <span id="profile-email-hint" class="account-field-hint">这是你的登录凭证，暂不支持修改。</span>
                </div>
              </div>
              <div v-if="editing" class="account-form-actions">
                <p>修改会在保存后生效。</p>
                <button class="account-cancel-button" type="button" :disabled="busy" @click="cancelEditing">取消编辑</button>
                <button class="account-edit-button" type="submit" :disabled="busy">{{ busy ? '保存中…' : '保存修改' }}<span aria-hidden="true">✓</span></button>
              </div>
            </form>
            <div class="account-details">
              <div class="account-detail-heading"><SketchIcon name="user" :size="21" /><h3>账户身份</h3></div>
              <dl>
                <div><dt>基础角色</dt><dd>{{ session.actor?.baseRole === 'ADMIN' ? '管理员' : '普通用户' }}</dd></div>
                <div><dt>合作身份</dt><dd>{{ isDealer ? '有效经销商' : '普通账户' }}<span v-if="isDealer" class="account-verified" aria-label="已认证">✓</span></dd></div>
              </dl>
            </div>
            <div class="account-privacy"><svg width="19" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6z" /><path d="m8 12 3 3 5-6" /></svg><p>你的资料仅用于账户管理与必要的业务联系。</p></div>
          </section>
          <section class="account-partnership" aria-labelledby="account-partnership-title">
            <div class="account-partnership-copy">
              <p class="account-eyebrow">{{ isDealer ? 'BETTER TOGETHER' : 'GROW WITH WEMOVE' }}</p>
              <h2 id="account-partnership-title">{{ isDealer ? '下一次合作，从这里开始。' : '把玩乐，带到更多人身边。' }}</h2>
              <p>{{ isDealer ? '发现适合你的商品，为下一次合作准备一份新询价。' : '成为 WEMOVE 合作伙伴，一起探索运动与生活的更多可能。' }}</p>
              <RouterLink :to="isDealer ? '/dealer/catalog' : '/account/dealer-application'">{{ isDealer ? '浏览专属目录' : '了解经销合作' }}<span aria-hidden="true">↗</span></RouterLink>
            </div>
            <ProfilePlayIllustration class="account-partnership-art" />
          </section>
          <p class="account-signature">A little play, every day.<span aria-hidden="true">✳</span></p>
        </div>
      </div>
    </div>
  </PublicShell>
</template>

<style scoped>
.profile-site { --account-ink: #253d34; --account-muted: #68776d; --account-green: #21865c; --account-border: #e7ece8; background: #eff5f1; color: var(--account-ink); }
.account-page { width: min(1220px, calc(100% - 72px)); margin: 0 auto; padding: 44px 0 60px; }
.account-heading { display: flex; align-items: center; justify-content: space-between; gap: 24px; margin-bottom: 30px; }
.account-eyebrow { margin: 0 0 9px; color: var(--account-muted); font: 600 12px/1.4 var(--font-display); letter-spacing: 1.7px; }
.account-heading h1 { margin: 0; color: #23392f; font-size: clamp(32px, 3.2vw, 42px); font-weight: 500; line-height: 1.4; letter-spacing: -1.2px; }
.account-heading h1 > span { color: var(--account-green); }
.account-intro { margin: 8px 0 0; color: #6b7b71; font-size: 15px; line-height: 1.8; }
.account-back { display: inline-flex; flex-shrink: 0; align-items: center; gap: 9px; padding: 10px 0; color: #53665a; font-size: 14px; text-decoration: none; }
.account-back:hover { color: var(--account-green); }
.account-layout { display: grid; grid-template-columns: 296px minmax(0, 1fr); gap: 28px; align-items: start; }
.account-sidebar { padding: 29px 22px 20px; border: 1px solid #fff; border-radius: 24px; background: #fff; box-shadow: 0 8px 32px #25403204; }
.account-person { display: flex; flex-direction: column; align-items: center; padding: 0 0 25px; text-align: center; }
.account-avatar { position: relative; display: grid; place-items: center; width: 94px; height: 94px; margin-bottom: 16px; border: 6px solid #f5f8f4; border-radius: 50%; background: #deebdf; color: #41634d; }
.account-avatar > span { font-size: 40px; font-weight: 500; line-height: 1; }
.account-avatar > i { position: absolute; right: -6px; bottom: -2px; display: grid; place-items: center; width: 31px; height: 31px; border: 3px solid #fff; border-radius: 50%; background: #f2de86; }
.account-person h2 { max-width: 100%; margin: 0; font-size: 21px; font-weight: 500; line-height: 1.5; overflow-wrap: anywhere; }
.account-email { max-width: 100%; margin: 5px 0 12px; color: var(--account-muted); font: 13px/1.6 -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; overflow-wrap: anywhere; }
.account-status { display: inline-flex; align-items: center; gap: 6px; padding: 5px 10px; border-radius: 20px; color: #427552; background: #edf6ee; font-size: 11px; line-height: 1.4; }
.account-status > i, .account-editing-label > i { width: 5px; height: 5px; border-radius: 50%; background: currentColor; }
.account-status.is-disabled { color: #9d3c32; background: #fbefec; }
.account-navigation { border-top: 1px solid var(--account-border); padding-top: 20px; }
.account-group-label { margin: 0 14px 9px; color: #68776d; font-size: 12px; }
.account-nav-link { display: flex; align-items: center; gap: 13px; min-height: 47px; margin-bottom: 4px; padding: 11px 14px; border-radius: 10px; color: #546359; font-size: 15px; line-height: 1.5; text-decoration: none; transition: background-color 160ms ease, color 160ms ease; }
.account-nav-link > span:not(.account-chevron) { min-width: 0; overflow-wrap: anywhere; }
.account-chevron { flex-shrink: 0; margin-left: auto; color: #a2aea4; font: 25px/1 Arial, sans-serif; }
.account-nav-link.is-current { color: #24734d; background: #eaf3eb; font-weight: 500; }
.account-nav-link.is-current .account-chevron { color: #43845a; }
.account-nav-link:hover, .account-nav-link:focus-visible { color: #216742; background: #edf4ec; }
.account-group-divider { margin-top: 18px; padding-top: 18px; border-top: 1px solid var(--account-border); }
.account-help { display: flex; align-items: center; gap: 8px; margin: 18px 12px 0; padding-top: 18px; border-top: 1px solid var(--account-border); color: #758078; font-size: 12px; text-decoration: none; }
.account-help > span:last-child { margin-left: auto; }
.account-help:hover { color: var(--account-green); }
.account-content { min-width: 0; }
.account-information { padding: 31px 34px 0; border: 1px solid #fff; border-radius: 24px; background: #fff; box-shadow: 0 8px 32px #25403204; }
.account-section-heading { display: flex; align-items: center; justify-content: space-between; gap: 22px; padding-bottom: 27px; border-bottom: 1px solid var(--account-border); }
.account-section-heading .account-eyebrow { font-size: 11px; letter-spacing: 1.5px; }
.account-section-heading h2 { margin: 0; color: #283e32; font-size: 25px; line-height: 1.4; font-weight: 500; }
.account-section-heading > div > p:last-child { margin: 6px 0 0; color: var(--account-muted); font-size: 14px; line-height: 1.7; }
.account-edit-button { display: inline-flex; align-items: center; justify-content: center; gap: 9px; flex-shrink: 0; min-height: 42px; padding: 10px 19px; border: 1px solid transparent; border-radius: 9px; background: var(--account-green); color: #fff; font-size: 14px; transition: background-color 160ms ease; }
.account-edit-button:hover:not(:disabled) { background: #196e49; }
.account-editing-label { display: inline-flex; flex-shrink: 0; align-items: center; gap: 7px; padding: 7px 11px; color: var(--account-green); border-radius: 20px; background: #edf6ee; font-size: 12px; }
.account-form { padding-top: 26px; }
.account-fields { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 23px 22px; }
.account-field { display: grid; gap: 9px; min-width: 0; align-content: start; }
.account-field-wide { grid-column: 1 / -1; }
.account-field > label { display: flex; align-items: center; gap: 9px; font-size: 14px; }
.account-field > label > span { color: #68776d; font-size: 11px; }
.account-field .account-fixed-label { display: inline-flex; align-items: center; gap: 4px; margin-left: auto; }
.account-field > input { display: block; width: 100%; min-width: 0; min-height: 53px; padding: 13px 16px; border: 1px solid #e7ece8; border-radius: 10px; color: #35473a; background: #f8faf8; font-size: 16px; line-height: 1.6; transition: border-color 160ms ease, box-shadow 160ms ease; }
.account-field > input::placeholder { color: #6b776e; opacity: 1; }
.account-field > input:disabled { color: #8a948d; -webkit-text-fill-color: #8a948d; opacity: 1; }
.account-form.is-editing .account-field > input:not(:disabled) { background: #fff; border-color: #bacfc0; }
.account-field > input:focus { outline: 0; }
.account-form.is-editing .account-field > input:focus { border-color: var(--account-green); box-shadow: 0 0 0 3px #21865c12; }
.account-form .account-field > input[aria-invalid="true"] { border-color: #b54d40; }
.account-field-hint { color: #68776d; font-size: 12px; line-height: 1.6; }
.account-field-error { color: #a54335; font-size: 12px; line-height: 1.6; }
.account-form-actions { display: flex; align-items: center; justify-content: flex-end; flex-wrap: wrap; gap: 12px; margin-top: 26px; }
.account-form-actions > p { flex: 1; margin: 0; color: var(--account-muted); font-size: 12px; line-height: 1.7; }
.account-cancel-button { min-height: 42px; padding: 10px 16px; border: 1px solid #dce5de; border-radius: 9px; color: #68776c; background: #fff; font-size: 14px; }
.account-cancel-button:hover:not(:disabled) { background: #f4f7f3; }
.account-feedback { margin: 0 0 22px; padding: 13px 15px; border-radius: 8px; font-size: 13px; line-height: 1.7; }
.account-feedback-error { color: #924335; background: #fff2ef; }
.account-feedback-success { color: #24734d; background: #edf6ee; }
.account-details { margin-top: 28px; padding-top: 23px; border-top: 1px solid var(--account-border); }
.account-detail-heading { display: flex; align-items: center; gap: 8px; }
.account-detail-heading h3 { margin: 0; font-size: 15px; font-weight: 500; }
.account-details dl { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 22px; margin: 16px 0 25px; }
.account-details dt { margin-bottom: 6px; color: #68776d; font-size: 12px; }
.account-details dd { display: flex; align-items: center; gap: 8px; margin: 0; color: #526658; font-size: 14px; }
.account-verified { color: var(--account-green); }
.account-privacy { display: flex; align-items: center; gap: 9px; margin: 0 -34px; padding: 17px 34px; border-top: 1px solid #eef2ee; border-radius: 0 0 24px 24px; background: #fafcf9; color: #627164; }
.account-privacy > svg { flex-shrink: 0; }
.account-privacy p { margin: 0; font-size: 12px; line-height: 1.7; }
.account-partnership { position: relative; display: flex; align-items: center; gap: 12px; margin-top: 22px; padding: 23px 27px 23px 32px; overflow: hidden; border: 1px solid #dee8dc; border-radius: 21px; background: #e5eee1; }
.account-partnership-copy { position: relative; z-index: 1; flex: 1; min-width: 0; }
.account-partnership .account-eyebrow { margin-bottom: 7px; color: #5e7058; font-size: 10px; letter-spacing: 1.5px; }
.account-partnership h2 { margin: 0; color: #355438; font-size: 22px; font-weight: 500; line-height: 1.5; }
.account-partnership-copy > p:not(.account-eyebrow) { max-width: 420px; margin: 8px 0 15px; color: #60725a; font-size: 13px; line-height: 1.8; }
.account-partnership a { display: inline-flex; align-items: center; gap: 13px; padding: 2px 0; color: #355f36; font-size: 13px; text-decoration: none; }
.account-partnership a:hover { text-decoration: underline; text-underline-offset: 5px; }
.account-partnership-art { flex: 0 0 172px; width: 172px; }
.account-signature { display: flex; justify-content: flex-end; align-items: center; gap: 10px; margin: 18px 4px 0; color: #9cad9c; font: 400 18px/1.5 var(--font-hand); }
.account-signature > span { color: #80a484; font: 24px/1 sans-serif; }
.account-page :is(a, button):focus-visible { outline: 2px solid var(--account-green); outline-offset: 4px; }
@media (max-width: 1080px) {
  .account-page { width: calc(100% - 48px); padding-top: 34px; }
  .account-layout { grid-template-columns: 260px minmax(0, 1fr); gap: 22px; }
  .account-sidebar { padding-inline: 16px; }
  .account-information { padding: 27px 26px 0; }
  .account-privacy { margin-inline: -26px; padding-inline: 26px; }
  .account-partnership { padding: 23px 26px; }
  .account-partnership-art { flex-basis: 125px; width: 125px; }
  .account-section-heading { align-items: flex-start; }
}
@media (max-width: 800px) {
  .account-page { width: calc(100% - 32px); padding: 28px 0 40px; }
  .account-heading { align-items: flex-start; margin-bottom: 24px; }
  .account-back { font-size: 0; gap: 0; padding: 11px; border: 1px solid #dce6dc; border-radius: 50%; }
  .account-layout { display: flex; flex-direction: column; gap: 22px; }
  .account-sidebar, .account-content { width: 100%; }
  .account-sidebar { padding: 24px; border-radius: 20px; }
  .account-person { display: grid; grid-template-columns: 76px minmax(0, 1fr); justify-items: start; column-gap: 18px; padding-bottom: 20px; text-align: left; }
  .account-avatar { grid-row: 1 / 4; width: 76px; height: 76px; margin: 0; }
  .account-avatar > span { font-size: 30px; }
  .account-person h2 { font-size: 20px; }
  .account-email { margin: 3px 0 6px; }
  .account-status { padding: 3px 8px; font-size: 10px; }
  .account-navigation { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 4px 10px; padding-top: 16px; }
  .account-group-label { grid-column: 1 / -1; margin: 0 10px 5px; }
  .account-group-divider { margin-top: 10px; padding-top: 15px; }
  .account-nav-link { min-height: 44px; margin: 0; padding: 9px 10px; gap: 8px; font-size: 14px; }
  .account-help { margin-inline: 10px; }
  .account-information { border-radius: 20px; }
  .account-partnership-art { flex-basis: 150px; width: 150px; }
}
@media (max-width: 520px) {
  .account-heading h1 { font-size: 31px; }
  .account-heading .account-eyebrow { font-size: 10px; letter-spacing: 1px; }
  .account-intro { max-width: 250px; font-size: 13px; }
  .account-sidebar { padding: 22px 16px 18px; }
  .account-person { column-gap: 14px; }
  .account-email { font-size: 11px; }
  .account-navigation { column-gap: 3px; }
  .account-nav-link { font-size: 13px; gap: 7px; padding-inline: 8px; }
  .account-nav-link :deep(.sketch-icon) { width: 21px; height: 21px; }
  .account-information { padding: 25px 20px 0; }
  .account-section-heading { flex-wrap: wrap; gap: 16px; padding-bottom: 22px; }
  .account-section-heading h2 { font-size: 23px; }
  .account-section-heading > div > p:last-child { font-size: 13px; }
  .account-fields { grid-template-columns: minmax(0, 1fr); gap: 21px; }
  .account-form-actions > p { flex-basis: 100%; }
  .account-details dl { gap: 12px; }
  .account-privacy { margin-inline: -20px; padding: 15px 20px; border-radius: 0 0 20px 20px; }
  .account-privacy p { font-size: 11px; }
  .account-partnership { gap: 0; padding: 24px 20px; }
  .account-partnership h2 { font-size: 20px; }
  .account-partnership-copy > p:not(.account-eyebrow) { font-size: 12px; }
  .account-partnership-art { flex-basis: 92px; width: 92px; margin-right: -10px; }
  .account-partnership .account-eyebrow { font-size: 9px; letter-spacing: 1px; }
}
</style>
