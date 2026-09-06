import { clearCommands } from '../features/commerce/commandRecovery'
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { api, resetCsrf } from '../services/http'
import type { Actor } from '../types'

export const useSessionStore = defineStore('session', () => {
  const actor = ref<Actor | null>(null)
  const ready = ref(false)
  const loading = ref(false)
  const isAdmin = computed(() => actor.value?.baseRole === 'ADMIN')

  async function load(force = false) {
    if ((ready.value && !force) || loading.value) return actor.value
    loading.value = true
    try {
      actor.value = (await api<Actor>('/auth/me')).data
    } catch {
      actor.value = null
    } finally {
      ready.value = true
      loading.value = false
    }
    return actor.value
  }

  async function login(email: string, password: string) {
    clearCommands(); window.dispatchEvent(new Event('wemove:account-changed'))
    actor.value = (await api<Actor>('/auth/login', {
      method: 'POST', body: JSON.stringify({ email, password }),
    })).data
    ready.value = true
    resetCsrf()
    return actor.value
  }

  async function logout() {
    try { await api<void>('/auth/logout', { method: 'POST' }) } finally {
      clearCommands(); window.dispatchEvent(new Event('wemove:account-changed'))
      actor.value = null
      ready.value = true
      resetCsrf()
    }
  }

  function replace(next: Actor) { actor.value = next }
  function clear() { clearCommands(); window.dispatchEvent(new Event('wemove:account-changed')); actor.value = null; ready.value = true; resetCsrf() }
  window.addEventListener('wemove:auth-invalid', clear)

  return { actor, ready, loading, isAdmin, load, login, logout, replace, clear }
})
