<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BackgroundChecksPanel from '../components/BackgroundChecksPanel.vue'
import { api, ensureCsrf, errorMessage } from '../api/client'
import type { Employee, Role } from '../types'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)
const employee = ref<Employee | null>(null)
const error = ref('')
const success = ref('')
const saving = ref(false)
const form = reactive({ name: '', email: '', phone: '', dateOfBirth: '', department: '', position: '', role: 'EMPLOYEE' as Role, hireDate: '' })

function copyToForm(value: Employee) {
  Object.assign(form, {
    name: value.name, email: value.email, phone: value.phone || '', dateOfBirth: value.dateOfBirth,
    department: value.department, position: value.position, role: value.role, hireDate: value.hireDate,
  })
}

async function load() {
  try {
    employee.value = (await api.get<Employee>(`/admin/employees/${id}`)).data
    copyToForm(employee.value)
  } catch (e) {
    error.value = errorMessage(e)
  }
}

async function save() {
  error.value = ''
  success.value = ''
  saving.value = true
  try {
    await ensureCsrf()
    employee.value = (await api.patch<Employee>(`/admin/employees/${id}`, form)).data
    copyToForm(employee.value)
    success.value = '직원 정보를 저장했습니다.'
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    saving.value = false
  }
}

async function terminate() {
  if (!employee.value || !window.confirm(`${employee.value.name} 직원을 퇴사 처리합니까? 즉시 접근이 차단됩니다.`)) return
  error.value = ''
  try {
    await ensureCsrf()
    employee.value = (await api.post<Employee>(`/admin/employees/${id}/terminate`)).data
    success.value = '퇴사 처리했습니다.'
  } catch (e) {
    error.value = errorMessage(e)
  }
}

onMounted(() => {
  if (!Number.isFinite(id)) router.replace('/admin/employees')
  else load()
})
</script>

<template>
  <section v-if="employee">
    <div class="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <RouterLink class="back-link" to="/admin/employees">[-] 직원 목록</RouterLink>
        <h2 class="mt-2 text-3xl font-bold">{{ employee.name }}</h2>
        <p class="mt-1 text-slate-500">{{ employee.employeeNumber }}</p>
      </div>
      <div class="flex items-center gap-3">
        <span :class="employee.status === 'ACTIVE' ? 'badge-active' : 'badge-terminated'">{{ employee.status === 'ACTIVE' ? '재직' : '퇴사' }}</span>
        <button v-if="employee.status === 'ACTIVE'" class="btn-danger" type="button" @click="terminate">퇴사 처리</button>
      </div>
    </div>
    <div v-if="error" class="error mb-4">{{ error }}</div>
    <div v-if="success" class="success mb-4">{{ success }}</div>
    <form class="card grid gap-5 sm:grid-cols-2" @submit.prevent="save">
      <label><span class="label">이름</span><input v-model="form.name" class="field" required /></label>
      <label><span class="label">이메일</span><input v-model="form.email" class="field" type="email" required /></label>
      <label><span class="label">전화번호</span><input v-model="form.phone" class="field" /></label>
      <label><span class="label">생년월일</span><input v-model="form.dateOfBirth" class="field" type="date" required /></label>
      <label><span class="label">부서</span><input v-model="form.department" class="field" required /></label>
      <label><span class="label">직급</span><input v-model="form.position" class="field" required /></label>
      <label><span class="label">Role</span><select v-model="form.role" class="field"><option value="EMPLOYEE">EMPLOYEE</option><option value="ADMIN">ADMIN</option></select></label>
      <label><span class="label">입사일</span><input v-model="form.hireDate" class="field" type="date" required /></label>
      <div class="sm:col-span-2"><button class="btn-primary" :disabled="saving">{{ saving ? '저장 중...' : '정보 저장' }}</button></div>
    </form>
    <BackgroundChecksPanel :employee-id="id" />
  </section>
  <div v-else-if="error" class="error">{{ error }}</div>
  <div v-else class="text-slate-500">불러오는 중...</div>
</template>
