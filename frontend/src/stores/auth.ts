import { ref } from 'vue'
import { defineStore } from 'pinia'
import { api, ApiError } from '../api/http'

export interface Me { id: string; username: string; displayName: string; authorities: string[] }

export const useAuthStore = defineStore('auth', () => {
  const user = ref<Me | null>(null)
  const initialized = ref(false)

  async function restore() {
    try { user.value = await api.get<Me>('/me') }
    catch (error) {
      if (!(error instanceof ApiError) || error.status !== 401) throw error
      user.value = null
    }
    initialized.value = true
  }

  async function login(username: string, password: string) {
    await api.post('/auth/login', new URLSearchParams({ username, password }))
    await restore()
  }

  async function logout() {
    await api.post('/auth/logout')
    user.value = null
  }

  return { user, initialized, restore, login, logout }
})
