<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuth } from './composables/auth'

const router = useRouter()
const auth = useAuth()

async function logout() {
  await auth.logout()
  await router.push('/login')
}
</script>

<template>
  <div class="min-h-screen bg-slate-50 text-slate-900">
    <header v-if="auth.user.value" class="border-b border-slate-200 bg-white">
      <div class="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
        <div>
          <p class="text-xs font-semibold uppercase tracking-[0.18em] text-blue-600">Bit Computer</p>
          <h1 class="text-lg font-bold">직원 포털</h1>
        </div>
        <nav class="flex items-center gap-2 text-sm">
          <RouterLink v-if="auth.isAdmin.value" class="nav-link" to="/admin/employees">직원 관리</RouterLink>
          <RouterLink v-else class="nav-link" to="/employee/profile">내 정보</RouterLink>
          <span class="hidden text-slate-500 sm:inline">{{ auth.user.value.name }}</span>
          <button class="btn-secondary" @click="logout">로그아웃</button>
        </nav>
      </div>
    </header>
    <main class="mx-auto max-w-6xl px-5 py-8">
      <RouterView />
    </main>
  </div>
</template>
