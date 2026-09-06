import { defineStore } from 'pinia'
import { ref } from 'vue'
import { readCart } from './api'
import type { Cart } from './types'
export const useCartStore = defineStore('commerce-cart', () => {
  const cart = ref<Cart | null>(null)
  let generation = 0
  function clear() {
    generation++
    cart.value = null
  }
  async function load() {
    const current = generation
    const result = await readCart()
    if (current === generation) cart.value = result
    return result
  }
  window.addEventListener('wemove:account-changed', clear)
  window.addEventListener('wemove:auth-invalid', clear)
  return { cart, load, clear }
})
