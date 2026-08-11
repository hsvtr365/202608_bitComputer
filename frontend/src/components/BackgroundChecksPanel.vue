<script setup lang="ts">
import axios from 'axios'
import { onMounted, onUnmounted, ref } from 'vue'
import { api, ensureCsrf, errorMessage } from '../api/client'
import type { BackgroundCheckItem, BackgroundCheckResult } from '../types'

const props = defineProps<{ employeeId: number }>()
const firstName = ref('')
const lastName = ref('')
const history = ref<BackgroundCheckItem[]>([])
const selected = ref<BackgroundCheckResult | null>(null)
const error = ref('')
const notice = ref('')
const running = ref(false)
let timer: number | undefined

async function load() {
  error.value = ''
  const [parts, checks] = await Promise.allSettled([
      api.get<{ firstName: string; lastName: string }>(`/admin/employees/${props.employeeId}/background-checks/name-parts`),
      api.get<{ checks: BackgroundCheckItem[] }>(`/admin/employees/${props.employeeId}/background-checks`),
  ])
  if (parts.status === 'fulfilled') {
    firstName.value = parts.value.data.firstName
    lastName.value = parts.value.data.lastName
  }
  if (checks.status === 'fulfilled') history.value = checks.value.data.checks || []
  const failed = parts.status === 'rejected' ? parts.reason : checks.status === 'rejected' ? checks.reason : null
  if (failed) error.value = errorMessage(failed)
}

async function refreshHistory() {
  try {
    history.value = (await api.get<{ checks: BackgroundCheckItem[] }>(
      `/admin/employees/${props.employeeId}/background-checks`,
    )).data.checks || []
  } catch (e) {
    error.value = errorMessage(e)
  }
}

async function run() {
  error.value = ''
  notice.value = ''
  running.value = true
  try {
    await ensureCsrf()
    const created = (await api.post<{ checkId: string; status: string }>(
      `/admin/employees/${props.employeeId}/background-checks`,
      { firstName: firstName.value, lastName: lastName.value },
    )).data
    notice.value = created.status === 'pending' ? '조회가 진행 중입니다.' : '조회가 완료되었습니다.'
    await fetchDetail(created.checkId, 0)
    await refreshHistory()
  } catch (e) {
    error.value = errorMessage(e)
    running.value = false
  }
}

async function fetchDetail(checkId: string, attempt = 15) {
  if (timer) window.clearTimeout(timer)
  try {
    selected.value = (await api.get<BackgroundCheckResult>(`/admin/background-checks/${checkId}`)).data
    if (selected.value.status === 'pending' && attempt < 15) {
      notice.value = '처리 중입니다. 자동으로 다시 확인합니다.'
      timer = window.setTimeout(() => fetchDetail(checkId, attempt + 1), 4000)
    } else {
      running.value = false
      notice.value = selected.value.status === 'pending'
        ? '아직 처리 중입니다. 잠시 후 다시 확인해 주세요.'
        : 'Background Check가 완료되었습니다.'
      await refreshHistory()
    }
  } catch (e) {
    const retryAfter = axios.isAxiosError(e) ? Number(e.response?.data?.retryAfter || 0) : 0
    if (retryAfter > 0 && attempt < 15) {
      notice.value = `${retryAfter}초 후 다시 확인합니다.`
      timer = window.setTimeout(() => fetchDetail(checkId, attempt + 1), retryAfter * 1000)
    } else {
      error.value = errorMessage(e)
      running.value = false
    }
  }
}

onMounted(load)
onUnmounted(() => { if (timer) window.clearTimeout(timer) })
</script>

<template>
  <section class="card mt-6">
    <div class="flex flex-wrap items-start justify-between gap-3">
      <div><h3 class="text-xl font-bold">Background Check</h3><p class="mt-1 text-sm text-slate-500">외부 서비스 결과는 내부 DB에 저장하지 않습니다.</p></div>
      <button class="btn-secondary" type="button" @click="refreshHistory">History 새로고침</button>
    </div>
    <div v-if="error" class="error mt-4">{{ error }}</div>
    <div v-if="notice" class="success mt-4">{{ notice }}</div>
    <form class="mt-5 grid gap-4 sm:grid-cols-[1fr_1fr_auto] sm:items-end" @submit.prevent="run">
      <label><span class="label">First name</span><input v-model="firstName" class="field" required /></label>
      <label><span class="label">Last name</span><input v-model="lastName" class="field" required /></label>
      <button class="btn-primary" :disabled="running">{{ running ? '조회 중...' : '조회 실행' }}</button>
    </form>

    <div v-if="selected" class="mt-6 rounded-xl border border-slate-200 p-5">
      <div class="flex items-center justify-between"><h4 class="font-bold">상세 결과</h4><strong class="uppercase text-blue-700">{{ selected.status }}</strong></div>
      <dl class="mt-4 grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-4">
        <div><dt class="text-slate-500">이름</dt><dd class="font-semibold">{{ selected.lastName }} {{ selected.firstName }}</dd></div>
        <div><dt class="text-slate-500">범죄 기록</dt><dd class="font-semibold">{{ selected.criminalRecord == null ? '-' : selected.criminalRecord ? '있음' : '없음' }}</dd></div>
        <div><dt class="text-slate-500">학력 확인</dt><dd class="font-semibold">{{ selected.educationVerified == null ? '-' : selected.educationVerified ? '확인' : '미확인' }}</dd></div>
        <div><dt class="text-slate-500">경력 확인</dt><dd class="font-semibold">{{ selected.employmentVerified == null ? '-' : selected.employmentVerified ? '확인' : '미확인' }}</dd></div>
        <div><dt class="text-slate-500">신용 등급</dt><dd class="font-semibold">{{ selected.creditScore || '-' }}</dd></div>
      </dl>
    </div>

    <div class="mt-6 overflow-x-auto">
      <h4 class="mb-3 font-bold">History</h4>
      <table class="w-full min-w-[620px] text-left text-sm">
        <thead class="border-b text-slate-500"><tr><th class="py-2">Check ID</th><th>상태</th><th>요청 시각</th><th>완료 시각</th></tr></thead>
        <tbody>
          <tr v-if="history.length === 0"><td class="py-4 text-slate-500" colspan="4">조회 이력이 없습니다.</td></tr>
          <tr v-for="item in history" :key="item.checkId" class="border-b border-slate-100">
            <td class="py-3"><button class="font-semibold text-blue-700 hover:underline" type="button" @click="fetchDetail(item.checkId)">{{ item.checkId }}</button></td>
            <td class="uppercase">{{ item.status }}</td><td>{{ new Date(item.createdAt).toLocaleString() }}</td><td>{{ item.completedAt ? new Date(item.completedAt).toLocaleString() : '-' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
