import { computed, reactive } from 'vue'
import { api, ensureCsrf } from '../api/client'
import type { AuthUser } from '../types'

const state = reactive<{ user: AuthUser | null; loaded: boolean }>({ user: null, loaded: false })

export function useAuth() {
  async function load() {
    try {
      const response = await api.get<AuthUser>('/auth/me')
      state.user = response.status === 204 ? null : response.data
    } catch {
      state.user = null
    } finally {
      state.loaded = true
    }
  }

  async function login(email: string, password: string) {
    await ensureCsrf()
    const body = new URLSearchParams({ email, password })
    await api.post('/auth/login', body, { headers: { 'Content-Type': 'application/x-www-form-urlencoded' } })
    await load()
  }

  async function logout() {
    await ensureCsrf()
    await api.post('/auth/logout')
    state.user = null
    state.loaded = true
  }

  return {
    user: computed(() => state.user),
    loaded: computed(() => state.loaded),
    isAdmin: computed(() => state.user?.role === 'ADMIN'),
    load,
    login,
    logout,
  }
}
