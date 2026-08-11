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
  <div class="login-shell">
    <form class="login-form" @submit.prevent="submit">
      <p class="eyebrow">Internal Employee Portal</p>
      <h2>로그인</h2>
      <p class="form-help">이메일로 로그인하세요.</p>
      <div v-if="error" class="error mt-5" role="alert">{{ error }}</div>
      <label class="mt-6 block">
        <span class="label">이메일</span>
        <input v-model="email" class="field" type="email" autocomplete="username" required maxlength="200" />
      </label>
      <label class="mt-4 block">
        <span class="label">비밀번호</span>
        <input v-model="password" class="field" type="password" autocomplete="current-password" required maxlength="72" />
      </label>
      <button class="btn-primary mt-6 w-full" :disabled="loading" :aria-busy="loading">
        <span v-if="loading" class="button-spinner" aria-hidden="true" />
        {{ loading ? '로그인 중...' : '로그인' }}
      </button>
    </form>
  </div>
</template>
