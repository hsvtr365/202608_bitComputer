<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, ensureCsrf, errorMessage } from '../api/client'
import type { Employee } from '../types'

const employee = ref<Employee | null>(null)
const name = ref('')
const email = ref('')
const phone = ref('')
const error = ref('')
const success = ref('')
const saving = ref(false)

onMounted(async () => {
  try {
    employee.value = (await api.get<Employee>('/me')).data
    name.value = employee.value.name
    email.value = employee.value.email
    phone.value = employee.value.phone || ''
  } catch (e) {
    error.value = errorMessage(e)
  }
})

async function save() {
  error.value = ''
  success.value = ''
  saving.value = true
  try {
    await ensureCsrf()
    employee.value = (await api.patch<Employee>('/me', {
      name: name.value, email: email.value, phone: phone.value,
    })).data
    name.value = employee.value.name
    email.value = employee.value.email
    phone.value = employee.value.phone || ''
    success.value = '개인 정보를 저장했습니다.'
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section>
    <div class="mb-6">
      <p class="eyebrow">Employee</p>
      <h2 class="text-3xl font-bold">내 정보</h2>
    </div>
    <div v-if="error" class="error mb-4">{{ error }}</div>
    <div v-if="success" class="success mb-4">{{ success }}</div>
    <div v-if="employee" class="card max-w-3xl">
      <dl class="grid gap-5 sm:grid-cols-2">
        <div><dt class="text-sm text-slate-500">사번</dt><dd class="mt-1 font-semibold">{{ employee.employeeNumber }}</dd></div>
        <div><dt class="text-sm text-slate-500">생년월일</dt><dd class="mt-1 font-semibold">{{ employee.dateOfBirth }}</dd></div>
        <div><dt class="text-sm text-slate-500">부서</dt><dd class="mt-1 font-semibold">{{ employee.department }}</dd></div>
        <div><dt class="text-sm text-slate-500">직급</dt><dd class="mt-1 font-semibold">{{ employee.position }}</dd></div>
        <div><dt class="text-sm text-slate-500">입사일</dt><dd class="mt-1 font-semibold">{{ employee.hireDate }}</dd></div>
        <div><dt class="text-sm text-slate-500">상태</dt><dd class="mt-1"><span class="badge-active">재직</span></dd></div>
      </dl>
      <form class="mt-8 border-t border-slate-200 pt-6" @submit.prevent="save">
        <div class="grid gap-5 sm:grid-cols-2">
        <label>
          <span class="label">이름</span>
          <input v-model="name" class="field" required minlength="2" maxlength="100" pattern="[가-힣]{2,100}" title="한글 이름을 입력하세요." />
        </label>
        <label>
          <span class="label">이메일</span>
          <input v-model="email" class="field" type="email" required maxlength="200" />
        </label>
        <label>
          <span class="label">전화번호</span>
          <input v-model="phone" class="field" maxlength="30" pattern="[0-9+() -]*" title="전화번호 형식을 확인하세요." />
        </label>
        </div>
        <p class="mt-2 text-xs text-slate-500">이름, 이메일, 전화번호만 직접 수정할 수 있습니다.</p>
        <button class="btn-primary mt-4" :disabled="saving" :aria-busy="saving"><span v-if="saving" class="button-spinner" aria-hidden="true" />{{ saving ? '저장 중...' : '저장' }}</button>
      </form>
    </div>
  </section>
</template>
