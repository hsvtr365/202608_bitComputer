<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, errorMessage } from '../api/client'
import type { Employee } from '../types'

const employees = ref<Employee[]>([])
const query = ref('')
const error = ref('')
const loading = ref(false)

async function load() {
  loading.value = true
  error.value = ''
  try {
    employees.value = (await api.get<Employee[]>('/admin/employees', { params: { q: query.value || undefined } })).data
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
    <form class="card mb-5 flex gap-2 p-4" @submit.prevent="load">
      <input v-model="query" class="field" aria-label="이름 또는 사번 검색" placeholder="이름 또는 사번" maxlength="100" />
      <button class="btn-secondary shrink-0" :disabled="loading" :aria-busy="loading"><span v-if="loading" class="button-spinner" aria-hidden="true" />{{ loading ? '검색 중...' : '검색' }}</button>
    </form>
    <div v-if="error" class="error mb-4">{{ error }}</div>
    <div class="card overflow-x-auto p-0">
      <table class="data-table w-full min-w-[850px] text-left text-sm">
        <thead>
          <tr><th class="p-4">사번</th><th>이름</th><th>부서</th><th>직급</th><th>이메일</th><th>상태</th><th>입사일</th></tr>
        </thead>
        <tbody>
          <tr v-if="loading"><td class="p-5 text-slate-500" colspan="7">불러오는 중...</td></tr>
          <tr v-else-if="employees.length === 0"><td class="p-5 text-slate-500" colspan="7">직원이 없습니다.</td></tr>
          <tr v-for="employee in employees" v-else :key="employee.id">
            <td class="p-4"><RouterLink class="row-link" :to="`/admin/employees/${employee.id}`">{{ employee.employeeNumber }}</RouterLink></td>
            <td class="font-semibold">{{ employee.name }}</td><td>{{ employee.department }}</td><td>{{ employee.position }}</td>
            <td>{{ employee.email }}</td>
            <td><span :class="employee.status === 'ACTIVE' ? 'badge-active' : 'badge-terminated'">{{ employee.status === 'ACTIVE' ? '재직' : '퇴사' }}</span></td>
            <td>{{ employee.hireDate }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
