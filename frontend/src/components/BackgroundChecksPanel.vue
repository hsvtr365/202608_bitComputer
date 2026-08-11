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
const errorContext = ref<'check' | 'history' | 'load' | null>(null)
const notice = ref('')
const noticeType = ref<'success' | 'info' | 'warning'>('info')
const running = ref(false)
let timer: number | undefined
let historyTimer: number | undefined

function showNotice(message: string, type: 'success' | 'info' | 'warning' = 'info') {
  error.value = ''
  errorContext.value = null
  notice.value = message
  noticeType.value = type
}

function showError(value: unknown, context: 'check' | 'history' | 'load') {
  notice.value = ''
  error.value = errorMessage(value)
  errorContext.value = context
}

async function load() {
  error.value = ''
  try {
    const parts = (await api.get<{ firstName: string; lastName: string }>(
      `/admin/employees/${props.employeeId}/background-checks/name-parts`,
    )).data
    firstName.value = parts.firstName
    lastName.value = parts.lastName
  } catch (e) {
    showError(e, 'load')
  }
  await refreshHistory()
}

async function refreshHistory(page = historyPage.value, attempt = 0, notify = false) {
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
    if (errorContext.value === 'history') {
      error.value = ''
      errorContext.value = null
    }
    if (notice.value.startsWith('History')) notice.value = ''
    if (notify) showNotice('History를 새로고침했습니다.', 'success')
    refreshing.value = false
  } catch (e) {
    const status = axios.isAxiosError(e) ? e.response?.status : 0
    const retryAfter = axios.isAxiosError(e) ? Number(e.response?.data?.retryAfter || 0) : 0
    if ((status === 502 || status === 503) && attempt < 3) {
      const delay = retryAfter > 0 ? Math.min(retryAfter, 300) : 3
      showNotice(`History 조회 실패. ${delay}초 후 다시 시도합니다. (${attempt + 1}/3)`, 'warning')
      historyTimer = window.setTimeout(() => refreshHistory(page, attempt + 1, notify), delay * 1000)
    } else {
      if (notice.value.startsWith('History')) notice.value = ''
      showError(e, 'history')
      refreshing.value = false
    }
  }
}

async function run() {
  error.value = ''
  errorContext.value = null
  notice.value = ''
  running.value = true
  try {
    await ensureCsrf()
    const created = (await api.post<{ checkId: string; status: string }>(
      `/admin/employees/${props.employeeId}/background-checks`,
      { firstName: firstName.value, lastName: lastName.value },
    )).data
    showNotice(created.status === 'pending'
      ? '요청 접수 완료. 결과 처리 중입니다.'
      : 'Background Check가 완료되었습니다.', created.status === 'pending' ? 'info' : 'success')
    await fetchDetail(created.checkId, 0)
    if (running.value) await refreshHistory(0)
  } catch (e) {
    showError(e, 'check')
    running.value = false
  }
}

async function fetchDetail(checkId: string, attempt = 0) {
  if (timer) window.clearTimeout(timer)
  try {
    selected.value = (await api.get<BackgroundCheckResult>(`/admin/background-checks/${checkId}`)).data
    if (selected.value.status === 'pending' && attempt < 15) {
      showNotice('결과 처리 중입니다. 자동으로 다시 확인합니다.', 'info')
      timer = window.setTimeout(() => fetchDetail(checkId, attempt + 1), 4000)
    } else {
      running.value = false
      showNotice(selected.value.status === 'pending'
        ? '아직 처리 중입니다. 잠시 후 다시 확인해 주세요.'
        : 'Background Check가 완료되었습니다.', selected.value.status === 'pending' ? 'info' : 'success')
      await refreshHistory(historyPage.value)
    }
  } catch (e) {
    const retryAfter = axios.isAxiosError(e) ? Number(e.response?.data?.retryAfter || 0) : 0
    if (retryAfter > 0 && attempt < 15) {
      showNotice(`외부 서비스 응답 대기 중입니다. ${retryAfter}초 후 다시 확인합니다.`, 'warning')
      timer = window.setTimeout(() => fetchDetail(checkId, attempt + 1), retryAfter * 1000)
    } else {
      showError(e, 'check')
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
      <button class="btn-secondary" type="button" :disabled="refreshing || running" :aria-busy="refreshing" @click="refreshHistory(0, 0, true)"><span v-if="refreshing" class="button-spinner" aria-hidden="true" />{{ refreshing ? 'History 갱신 중...' : 'History 새로고침' }}</button>
    </div>
    <div v-if="error" class="error mt-4" role="alert">{{ error }}</div>
    <div v-else-if="notice" :class="[noticeType, 'mt-4']" role="status">{{ notice }}</div>
    <form class="mt-5 grid gap-4 sm:grid-cols-[1fr_1fr_auto] sm:items-end" @submit.prevent="run">
      <label><span class="label">First name</span><input v-model="firstName" class="field" required maxlength="100" pattern="[\p{L}][\p{L} -]{0,99}" title="문자, 공백, -만 입력하세요." :disabled="running" /></label>
      <label><span class="label">Last name</span><input v-model="lastName" class="field" required maxlength="100" pattern="[\p{L}][\p{L} -]{0,99}" title="문자, 공백, -만 입력하세요." :disabled="running" /></label>
      <button class="btn-primary" :disabled="running || refreshing" :aria-busy="running"><span v-if="running" class="button-spinner" aria-hidden="true" />{{ running ? '조회 중...' : '조회 실행' }}</button>
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
          <button class="btn-secondary" type="button" :disabled="refreshing || running || historyPage === 0" @click="refreshHistory(historyPage - 1)">이전</button>
          <span>{{ historyPage + 1 }} / {{ totalPages }}</span>
          <button class="btn-secondary" type="button" :disabled="refreshing || running || historyPage + 1 >= totalPages" @click="refreshHistory(historyPage + 1)">다음</button>
        </div>
      </div>
      <table class="data-table w-full min-w-[620px] text-left text-sm">
        <thead><tr><th class="py-2">Check ID</th><th>상태</th><th>요청 시각</th><th>완료 시각</th></tr></thead>
        <tbody>
          <tr v-if="history.length === 0"><td class="py-4 text-slate-500" colspan="4">조회 이력이 없습니다.</td></tr>
          <tr v-for="item in history" :key="item.checkId">
            <td class="py-3"><button class="row-link" type="button" :disabled="running || refreshing" @click="fetchDetail(item.checkId)">{{ item.checkId }}</button></td>
            <td class="uppercase">{{ item.status }}</td><td>{{ new Date(item.createdAt).toLocaleString() }}</td><td>{{ item.completedAt ? new Date(item.completedAt).toLocaleString() : '-' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
