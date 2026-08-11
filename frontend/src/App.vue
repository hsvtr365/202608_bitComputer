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
  <div class="app-shell">
    <header v-if="auth.user.value" class="site-header">
      <div class="site-header-inner">
        <div class="brand-mark">
          <p>[ BIT::COMPUTER ]</p>
          <h1>internal_employee_portal</h1>
        </div>
        <nav class="primary-nav" aria-label="주요 메뉴">
          <RouterLink v-if="auth.isAdmin.value" class="nav-link" to="/admin/employees">직원 관리</RouterLink>
          <RouterLink v-else class="nav-link" to="/employee/profile">내 정보</RouterLink>
          <span class="user-name">{{ auth.user.value.name }}</span>
          <button class="btn-secondary" @click="logout">로그아웃</button>
        </nav>
      </div>
    </header>
    <main class="page-container">
      <RouterView />
    </main>
  </div>
</template>
