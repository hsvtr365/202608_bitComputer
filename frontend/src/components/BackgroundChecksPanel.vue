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
const historyPage = ref(0)
const totalCount = ref(0)
const totalPages = ref(1)
const refreshing = ref(false)
const error = ref('')
const notice = ref('')
const running = ref(false)
let timer: number | undefined
let historyTimer: number | undefined

async function load() {
  error.value = ''
  try {
    const parts = (await api.get<{ firstName: string; lastName: string }>(
      `/admin/employees/${props.employeeId}/background-checks/name-parts`,
    )).data
    firstName.value = parts.firstName
    lastName.value = parts.lastName
  } catch (e) {
    error.value = errorMessage(e)
  }
  await refreshHistory()
}

async function refreshHistory(page = historyPage.value, attempt = 0) {
  if (historyTimer) window.clearTimeout(historyTimer)
  refreshing.value = true
  try {
    const response = (await api.get<{ checks: BackgroundCheckItem[]; totalCount: number; page: number; totalPages: number }>(
      `/admin/employees/${props.employeeId}/background-checks`, { params: { page, size: 10 } },
    )).data
    history.value = response.checks || []
    historyPage.value = Number.isInteger(response.page) ? response.page : 0
    totalCount.value = Number.isInteger(response.totalCount) ? response.totalCount : history.value.length
    totalPages.value = Number.isInteger(response.totalPages) && response.totalPages > 0 ? response.totalPages : 1
    error.value = ''
    if (notice.value.startsWith('History')) notice.value = ''
    refreshing.value = false
  } catch (e) {
    const status = axios.isAxiosError(e) ? e.response?.status : 0
    const retryAfter = axios.isAxiosError(e) ? Number(e.response?.data?.retryAfter || 0) : 0
    if ((status === 502 || status === 503) && attempt < 3) {
      const delay = retryAfter > 0 ? Math.min(retryAfter, 300) : 3
      notice.value = `History 조회 실패. ${delay}초 후 다시 시도합니다. (${attempt + 1}/3)`
      historyTimer = window.setTimeout(() => refreshHistory(page, attempt + 1), delay * 1000)
    } else {
      if (notice.value.startsWith('History')) notice.value = ''
      error.value = errorMessage(e)
      refreshing.value = false
    }
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
    await refreshHistory(0)
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
      await refreshHistory(historyPage.value)
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
onUnmounted(() => {
  if (timer) window.clearTimeout(timer)
  if (historyTimer) window.clearTimeout(historyTimer)
})
</script>

<template>
  <section class="card mt-6">
    <div class="flex flex-wrap items-start justify-between gap-3">
      <div><h3 class="text-xl font-bold">Background Check</h3><p class="mt-1 text-sm text-slate-500">외부 서비스 결과는 내부 DB에 저장하지 않습니다.</p></div>
      <button class="btn-secondary" type="button" :disabled="refreshing" :aria-busy="refreshing" @click="refreshHistory(0)"><span v-if="refreshing" class="button-spinner" aria-hidden="true" />{{ refreshing ? 'History 갱신 중...' : 'History 새로고침' }}</button>
    </div>
    <div v-if="error" class="error mt-4">{{ error }}</div>
    <div v-if="notice" class="success mt-4">{{ notice }}</div>
    <form class="mt-5 grid gap-4 sm:grid-cols-[1fr_1fr_auto] sm:items-end" @submit.prevent="run">
      <label><span class="label">First name</span><input v-model="firstName" class="field" required maxlength="100" pattern=".*\S.*" /></label>
      <label><span class="label">Last name</span><input v-model="lastName" class="field" required maxlength="100" pattern=".*\S.*" /></label>
      <button class="btn-primary" :disabled="running" :aria-busy="running"><span v-if="running" class="button-spinner" aria-hidden="true" />{{ running ? '조회 중...' : '조회 실행' }}</button>
    </form>

    <div v-if="selected" class="result-block mt-6">
      <div class="flex items-center justify-between"><h4 class="font-bold">상세 결과</h4><strong class="status-text uppercase">[{{ selected.status }}]</strong></div>
      <dl class="mt-4 grid gap-3 text-sm sm:grid-cols-2 lg:grid-cols-4">
        <div><dt class="text-slate-500">이름</dt><dd class="font-semibold">{{ selected.lastName }} {{ selected.firstName }}</dd></div>
        <div><dt class="text-slate-500">범죄 기록</dt><dd class="font-semibold">{{ selected.criminalRecord == null ? '-' : selected.criminalRecord ? '있음' : '없음' }}</dd></div>
        <div><dt class="text-slate-500">학력 확인</dt><dd class="font-semibold">{{ selected.educationVerified == null ? '-' : selected.educationVerified ? '확인' : '미확인' }}</dd></div>
        <div><dt class="text-slate-500">경력 확인</dt><dd class="font-semibold">{{ selected.employmentVerified == null ? '-' : selected.employmentVerified ? '확인' : '미확인' }}</dd></div>
        <div><dt class="text-slate-500">신용 등급</dt><dd class="font-semibold">{{ selected.creditScore || '-' }}</dd></div>
      </dl>
    </div>

    <div class="mt-6 overflow-x-auto">
      <div class="mb-3 flex items-center justify-between gap-3">
        <h4 class="font-bold">History ({{ totalCount }})</h4>
        <div class="flex items-center gap-2 text-sm">
          <button class="btn-secondary" type="button" :disabled="refreshing || historyPage === 0" @click="refreshHistory(historyPage - 1)">이전</button>
          <span>{{ historyPage + 1 }} / {{ totalPages }}</span>
          <button class="btn-secondary" type="button" :disabled="refreshing || historyPage + 1 >= totalPages" @click="refreshHistory(historyPage + 1)">다음</button>
        </div>
      </div>
      <table class="data-table w-full min-w-[620px] text-left text-sm">
        <thead><tr><th class="py-2">Check ID</th><th>상태</th><th>요청 시각</th><th>완료 시각</th></tr></thead>
        <tbody>
          <tr v-if="history.length === 0"><td class="py-4 text-slate-500" colspan="4">조회 이력이 없습니다.</td></tr>
          <tr v-for="item in history" :key="item.checkId">
            <td class="py-3"><button class="row-link" type="button" @click="fetchDetail(item.checkId)">{{ item.checkId }}</button></td>
            <td class="uppercase">{{ item.status }}</td><td>{{ new Date(item.createdAt).toLocaleString() }}</td><td>{{ item.completedAt ? new Date(item.completedAt).toLocaleString() : '-' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
