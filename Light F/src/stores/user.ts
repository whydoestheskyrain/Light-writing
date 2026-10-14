import { defineStore } from 'pinia'
import { ref } from 'vue'
import { identityApi } from '@/api'
import type { User } from '@/types'

/**
 * 当前登录用户：登录/注册成功后拉取一次，刷新页面时由各页面按需 ensure。
 * 登出时清空。
 */
export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const loaded = ref(false)

  async function fetchMe(force = false) {
    if (loaded.value && !force) return user.value
    try {
      const res = await identityApi.me()
      user.value = res.data
      loaded.value = true
      return user.value
    } catch {
      user.value = null
      loaded.value = false
      return null
    }
  }

  function setUser(u: User | null) {
    user.value = u
    loaded.value = !!u
  }

  function reset() {
    user.value = null
    loaded.value = false
  }

  return { user, loaded, fetchMe, setUser, reset }
})
