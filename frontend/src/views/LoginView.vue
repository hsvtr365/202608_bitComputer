<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { errorMessage } from '../api/client'
import { useAuth } from '../composables/auth'

const email = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)
const auth = useAuth()
const router = useRouter()

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(email.value, password.value)
    await router.push(auth.isAdmin.value ? '/admin/employees' : '/employee/profile')
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="mx-auto grid min-h-[75vh] max-w-5xl items-center gap-10 lg:grid-cols-2">
    <section class="hidden lg:block">
      <p class="mb-3 text-sm font-bold uppercase tracking-[0.2em] text-blue-600">Internal Employee Portal</p>
      <h2 class="text-4xl font-bold leading-tight">직원 정보와 권한을<br />안전하게 관리합니다.</h2>
      <p class="mt-5 max-w-md leading-7 text-slate-600">재직 상태와 역할에 따라 필요한 정보만 제공합니다.</p>
    </section>
    <form class="card mx-auto w-full max-w-md p-8" @submit.prevent="submit">
      <h2 class="text-2xl font-bold">로그인</h2>
      <p class="mt-2 text-sm text-slate-500">회사 계정으로 로그인하세요.</p>
      <div v-if="error" class="error mt-5" role="alert">{{ error }}</div>
      <label class="mt-6 block">
        <span class="label">이메일</span>
        <input v-model="email" class="field" type="email" autocomplete="username" required />
      </label>
      <label class="mt-4 block">
        <span class="label">비밀번호</span>
        <input v-model="password" class="field" type="password" autocomplete="current-password" required />
      </label>
      <button class="btn-primary mt-6 w-full" :disabled="loading">
        {{ loading ? '로그인 중...' : '로그인' }}
      </button>
    </form>
  </div>
</template>
