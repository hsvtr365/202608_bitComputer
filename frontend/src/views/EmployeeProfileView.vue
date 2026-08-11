<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, ensureCsrf, errorMessage } from '../api/client'
import type { Employee } from '../types'

const employee = ref<Employee | null>(null)
const phone = ref('')
const error = ref('')
const success = ref('')
const saving = ref(false)

onMounted(async () => {
  try {
    employee.value = (await api.get<Employee>('/me')).data
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
    employee.value = (await api.patch<Employee>('/me', { phone: phone.value })).data
    success.value = '전화번호를 저장했습니다.'
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
        <div><dt class="text-sm text-slate-500">이름</dt><dd class="mt-1 font-semibold">{{ employee.name }}</dd></div>
        <div><dt class="text-sm text-slate-500">이메일</dt><dd class="mt-1 font-semibold">{{ employee.email }}</dd></div>
        <div><dt class="text-sm text-slate-500">생년월일</dt><dd class="mt-1 font-semibold">{{ employee.dateOfBirth }}</dd></div>
        <div><dt class="text-sm text-slate-500">부서</dt><dd class="mt-1 font-semibold">{{ employee.department }}</dd></div>
        <div><dt class="text-sm text-slate-500">직급</dt><dd class="mt-1 font-semibold">{{ employee.position }}</dd></div>
        <div><dt class="text-sm text-slate-500">입사일</dt><dd class="mt-1 font-semibold">{{ employee.hireDate }}</dd></div>
        <div><dt class="text-sm text-slate-500">상태</dt><dd class="mt-1"><span class="badge-active">재직</span></dd></div>
      </dl>
      <form class="mt-8 border-t border-slate-200 pt-6" @submit.prevent="save">
        <label class="block max-w-sm">
          <span class="label">전화번호</span>
          <input v-model="phone" class="field" maxlength="30" />
        </label>
        <p class="mt-2 text-xs text-slate-500">직원이 직접 수정할 수 있는 항목입니다.</p>
        <button class="btn-primary mt-4" :disabled="saving">{{ saving ? '저장 중...' : '저장' }}</button>
      </form>
    </div>
  </section>
</template>
