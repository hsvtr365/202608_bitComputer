<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, errorMessage } from '../api/client'
import type { Employee } from '../types'

interface EmployeePage {
  employees: Employee[]
  page: number
  size: number
  totalCount: number
  totalPages: number
}

const employees = ref<Employee[]>([])
const query = ref('')
const page = ref(0)
const pageSize = ref(10)
const totalCount = ref(0)
const totalPages = ref(1)
const error = ref('')
const loading = ref(false)

async function load(targetPage = page.value) {
  loading.value = true
  error.value = ''
  try {
    const response = (await api.get<EmployeePage>('/admin/employees', {
      params: { q: query.value || undefined, page: targetPage, size: pageSize.value },
    })).data
    employees.value = response.employees ?? []
    page.value = Number.isInteger(response.page) ? response.page : 0
    totalCount.value = Number.isInteger(response.totalCount) ? response.totalCount : employees.value.length
    totalPages.value = Number.isInteger(response.totalPages) && response.totalPages > 0 ? response.totalPages : 1
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <section>
    <div class="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div>
        <p class="eyebrow">Admin</p>
        <h2 class="text-3xl font-bold">직원 관리</h2>
      </div>
      <RouterLink class="btn-primary" to="/admin/employees/new">신규 직원 생성</RouterLink>
    </div>
    <form class="card mb-5 flex flex-wrap gap-2 p-4" @submit.prevent="load(0)">
      <input v-model="query" class="field min-w-52 flex-1" aria-label="이름 또는 사번 검색" placeholder="이름 또는 사번" maxlength="100" pattern="[A-Za-z0-9가-힣 _-]*" title="한글, 영문, 숫자, 공백, -, _만 입력하세요." />
      <label class="flex items-center gap-2 text-sm"><span>표시</span><select v-model.number="pageSize" class="field w-20" :disabled="loading" @change="load(0)"><option :value="10">10</option><option :value="15">15</option><option :value="30">30</option><option :value="50">50</option></select></label>
      <button class="btn-secondary shrink-0" :disabled="loading" :aria-busy="loading">검색</button>
    </form>
    <div v-if="error" class="error mb-4">{{ error }}</div>
    <div class="card overflow-x-auto p-0">
      <table class="data-table w-full min-w-[850px] text-left text-sm">
        <thead><tr><th class="p-4">사번</th><th>이름</th><th>부서</th><th>직급</th><th>이메일</th><th>상태</th><th>입사일</th></tr></thead>
        <tbody>
          <tr v-if="employees.length === 0"><td class="p-5 text-slate-500" colspan="7">직원이 없습니다.</td></tr>
          <tr v-for="employee in employees" v-else :key="employee.id">
            <td class="p-4"><RouterLink class="row-link" :to="`/admin/employees/${employee.id}`">{{ employee.employeeNumber }}</RouterLink></td>
            <td class="font-semibold">{{ employee.name }}</td><td>{{ employee.department }}</td><td>{{ employee.position }}</td><td>{{ employee.email }}</td>
            <td><span :class="employee.status === 'ACTIVE' ? 'badge-active' : 'badge-terminated'">{{ employee.status === 'ACTIVE' ? '재직' : '퇴사' }}</span></td><td>{{ employee.hireDate }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <div class="mt-4 flex items-center justify-between gap-3 text-sm">
      <span>총 {{ totalCount }}명</span>
      <div class="flex items-center gap-2"><button class="btn-secondary" type="button" :disabled="loading || page === 0" @click="load(page - 1)">이전</button><span>{{ page + 1 }} / {{ totalPages }}</span><button class="btn-secondary" type="button" :disabled="loading || page + 1 >= totalPages" @click="load(page + 1)">다음</button></div>
    </div>
  </section>
</template>
