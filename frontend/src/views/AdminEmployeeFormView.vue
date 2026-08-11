<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, ensureCsrf, errorMessage } from '../api/client'
import type { Employee, OrganizationCode, Role } from '../types'

const router = useRouter()
const error = ref('')
const saving = ref(false)
const departments = ref<OrganizationCode[]>([])
const positions = ref<OrganizationCode[]>([])
const localNow = Date.now() - new Date().getTimezoneOffset() * 60000
const today = new Date(localNow).toISOString().slice(0, 10)
const yesterday = new Date(localNow - 86400000).toISOString().slice(0, 10)
const form = reactive({
  employeeNumber: '', name: '', email: '', password: '', phone: '', dateOfBirth: '',
  department: '', position: '', role: 'EMPLOYEE' as Role, hireDate: new Date().toISOString().slice(0, 10),
})

onMounted(async () => {
  try {
    const [departmentResponse, positionResponse] = await Promise.all([
      api.get<OrganizationCode[]>('/organization-codes/departments'),
      api.get<OrganizationCode[]>('/organization-codes/positions'),
    ])
    departments.value = departmentResponse.data
    positions.value = positionResponse.data
    form.department ||= departments.value[0]?.name || ''
    form.position ||= positions.value[0]?.name || ''
  } catch (e) {
    error.value = errorMessage(e)
  }
})

async function submit() {
  error.value = ''
  saving.value = true
  try {
    await ensureCsrf()
    const created = (await api.post<Employee>('/admin/employees', form)).data
    await router.push(`/admin/employees/${created.id}`)
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section class="max-w-4xl">
    <div class="mb-6"><p class="eyebrow">Admin</p><h2 class="text-3xl font-bold">신규 직원 생성</h2></div>
    <div v-if="error" class="error mb-4">{{ error }}</div>
    <form class="card grid gap-5 sm:grid-cols-2" @submit.prevent="submit">
      <label><span class="label">사번</span><input v-model="form.employeeNumber" class="field" required maxlength="40" pattern="[A-Za-z0-9_-]+" title="영문, 숫자, -, _만 사용할 수 있습니다." /></label>
      <label><span class="label">한글 이름</span><input v-model="form.name" class="field" required minlength="2" maxlength="100" pattern="[가-힣]{2,100}" title="한글 이름을 입력하세요." /></label>
      <label><span class="label">이메일</span><input v-model="form.email" class="field" type="email" required maxlength="200" /></label>
      <label><span class="label">초기 비밀번호</span><input v-model="form.password" class="field" type="password" minlength="8" maxlength="72" required autocomplete="new-password" /></label>
      <label><span class="label">전화번호</span><input v-model="form.phone" class="field" maxlength="30" pattern="[0-9+() -]*" title="전화번호 형식을 확인하세요." /></label>
      <label><span class="label">생년월일</span><input v-model="form.dateOfBirth" class="field" type="date" required :max="yesterday" /></label>
      <label><span class="label">부서</span><select v-model="form.department" class="field" required><option disabled value="">선택</option><option v-for="item in departments" :key="item.code" :value="item.name">{{ item.name }}</option></select></label>
      <label><span class="label">직급</span><select v-model="form.position" class="field" required><option disabled value="">선택</option><option v-for="item in positions" :key="item.code" :value="item.name">{{ item.name }}</option></select></label>
      <label><span class="label">Role</span><select v-model="form.role" class="field"><option value="EMPLOYEE">EMPLOYEE</option><option value="ADMIN">ADMIN</option></select></label>
      <label><span class="label">입사일</span><input v-model="form.hireDate" class="field" type="date" required :max="today" /></label>
      <div class="flex gap-2 sm:col-span-2">
        <button class="btn-primary" :disabled="saving" :aria-busy="saving"><span v-if="saving" class="button-spinner" aria-hidden="true" />{{ saving ? '생성 중...' : '직원 생성' }}</button>
        <RouterLink class="btn-secondary" to="/admin/employees">취소</RouterLink>
      </div>
    </form>
  </section>
</template>
