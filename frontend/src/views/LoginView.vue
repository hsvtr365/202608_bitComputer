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
    <section class="terminal-panel" aria-label="비트컴퓨터 직원 포털 소개">
      <pre class="ascii-wordmark" aria-hidden="true">██████╗ ██╗████████╗
██╔══██╗██║╚══██╔══╝
██████╔╝██║   ██║
██╔══██╗██║   ██║
██████╔╝██║   ██║
╚═════╝ ╚═╝   ╚═╝</pre>
      <p class="terminal-command"><span>&gt;</span> employee_portal --secure-session</p>
      <div class="terminal-copy">
        <p>[+] role_based_access</p>
        <p>[+] active_status_check</p>
        <p>[x] unauthorized_access</p>
      </div>
      <p class="terminal-hint">tab switch_role&nbsp;&nbsp; ctrl-l login</p>
    </section>
    <form class="login-form" @submit.prevent="submit">
      <p class="eyebrow">Internal Employee Portal</p>
      <h2>로그인</h2>
      <p class="form-help">회사 계정으로 로그인하세요.</p>
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
