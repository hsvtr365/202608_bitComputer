<script setup lang="ts">
import axios from 'axios'
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { api, ensureCsrf, errorMessage } from '../api/client'
import type { BackgroundCheckItem, BackgroundCheckResult } from '../types'

type Action = 'idle' | 'initial' | 'refresh' | 'check' | 'detail'
type Feedback = { message: string; type: 'success' | 'info' | 'warning' | 'error'; context: 'check' | 'history' | 'load' }

const props = defineProps<{ employeeId: number }>()
const firstName = ref('')
const lastName = ref('')
const allHistory = ref<BackgroundCheckItem[]>([])
const history = ref<BackgroundCheckItem[]>([])
const selected = ref<BackgroundCheckResult | null>(null)
const historyPage = ref(0)
const totalCount = ref(0)
const totalPages = ref(1)
const action = ref<Action>('idle')
const feedback = ref<Feedback | null>(null)
const busy = computed(() => action.value !== 'idle')
const refreshing = computed(() => action.value === 'refresh')
const running = computed(() => action.value === 'check' || action.value === 'detail')
let stopped = false
let waitTimer: number | undefined

function show(message: string, type: Feedback['type'], context: Feedback['context']) {
  feedback.value = { message, type, context }
}

function fail(error: unknown, context: Feedback['context']) {
  show(errorMessage(error), 'error', context)
}

function clear(context?: Feedback['context']) {
  if (!context || feedback.value?.context === context) feedback.value = null
}

function wait(milliseconds: number) {
  return new Promise<void>((resolve) => {
    waitTimer = window.setTimeout(resolve, milliseconds)
  })
}

function showHistoryPage(page: number) {
  totalCount.value = allHistory.value.length
  totalPages.value = Math.max(1, Math.ceil(totalCount.value / 10))
  historyPage.value = Math.min(Math.max(page, 0), totalPages.value - 1)
  const from = historyPage.value * 10
  history.value = allHistory.value.slice(from, from + 10)
}

async function requestHistory(page: number) {
  const response = (await api.get<{
    checks: BackgroundCheckItem[]
    totalCount: number
  }>(`/admin/employees/${props.employeeId}/background-checks`)).data

  allHistory.value = response.checks ?? []
  showHistoryPage(page)
}

async function requestHistoryWithRetry(page: number) {
  for (let attempt = 0; attempt <= 5; attempt += 1) {
    try {
      await requestHistory(page)
      return
    } catch (error) {
      const status = axios.isAxiosError(error) ? error.response?.status : 0
      if ((status !== 502 && status !== 503) || attempt === 5) throw error
      const retryAfter = axios.isAxiosError(error) ? Number(error.response?.data?.retryAfter || 0) : 0
      const delay = retryAfter > 0 ? Math.min(retryAfter, 300) : 3
      show(`History 조회 실패. ${delay}초 후 다시 시도합니다. (${attempt + 1}/5)`, 'warning', 'history')
      await wait(delay * 1000)
    }
  }
}

async function load() {
  action.value = 'initial'
  clear()
  try {
    const parts = (await api.get<{ firstName: string; lastName: string }>(
      `/admin/employees/${props.employeeId}/background-checks/name-parts`,
    )).data
    firstName.value = parts.firstName
    lastName.value = parts.lastName
  } catch (error) {
    fail(error, 'load')
  }
  try {
    await requestHistoryWithRetry(0)
    clear('history')
  } catch (error) {
    fail(error, 'history')
  } finally {
    action.value = 'idle'
  }
}

async function refreshHistory() {
  if (busy.value) return
  action.value = 'refresh'
  clear('history')
  try {
    await requestHistoryWithRetry(0)
    show('History를 새로고침했습니다.', 'success', 'history')
  } catch (error) {
    fail(error, 'history')
  } finally {
    action.value = 'idle'
  }
}

function goToHistoryPage(page: number) {
  if (busy.value || page < 0 || page >= totalPages.value || page === historyPage.value) return
  clear('history')
  showHistoryPage(page)
}

async function pollDetail(checkId: string) {
  for (let attempt = 0; attempt <= 15 && !stopped; attempt += 1) {
    try {
      const result = (await api.get<BackgroundCheckResult>(`/admin/background-checks/${checkId}`)).data
      selected.value = result
      if (result.status !== 'pending' || attempt === 15) return result
      show('결과 처리 중입니다. 자동으로 다시 확인합니다.', 'info', 'check')
      await wait(4000)
    } catch (error) {
      const retryAfter = axios.isAxiosError(error) ? Number(error.response?.data?.retryAfter || 0) : 0
      if (retryAfter <= 0 || attempt === 15) throw error
      show(`외부 서비스 응답 대기 중입니다. ${retryAfter}초 후 다시 확인합니다.`, 'warning', 'check')
      await wait(retryAfter * 1000)
    }
  }
  return selected.value
}

async function run() {
  if (busy.value) return
  action.value = 'check'
  clear()
  try {
    await ensureCsrf()
    const created = (await api.post<{ checkId: string; status: string }>(
      `/admin/employees/${props.employeeId}/background-checks`,
      { firstName: firstName.value, lastName: lastName.value },
    )).data
    show(created.status === 'pending' ? '요청 접수 완료. 결과 처리 중입니다.' : 'Background Check가 완료되었습니다.',
      created.status === 'pending' ? 'info' : 'success', 'check')

    const result = await pollDetail(created.checkId)
    if (stopped) return
    show(result?.status === 'pending' ? '아직 처리 중입니다. 잠시 후 다시 확인해 주세요.' : 'Background Check가 완료되었습니다.',
      result?.status === 'pending' ? 'info' : 'success', 'check')
  } catch (error) {
    fail(error, 'check')
  } finally {
    action.value = 'idle'
  }
}

async function viewDetail(checkId: string) {
  if (busy.value) return
  action.value = 'detail'
  clear()
  try {
    const result = await pollDetail(checkId)
    if (stopped) return
    show(result?.status === 'pending' ? '아직 처리 중입니다. 잠시 후 다시 확인해 주세요.' : 'Background Check가 완료되었습니다.',
      result?.status === 'pending' ? 'info' : 'success', 'check')
  } catch (error) {
    fail(error, 'check')
  } finally {
    action.value = 'idle'
  }
}

onMounted(load)
onUnmounted(() => {
  stopped = true
  if (waitTimer) window.clearTimeout(waitTimer)
})
</script>

<template>
  <section class="card mt-6">
    <div class="flex flex-wrap items-start justify-between gap-3">
      <div><h3 class="text-xl font-bold">Background Check</h3><p class="mt-1 text-sm text-slate-500">외부 서비스 결과는 내부 DB에 저장하지 않습니다.</p></div>
    </div>
    <div v-if="feedback" :class="[feedback.type, 'mt-4']" :role="feedback.type === 'error' ? 'alert' : 'status'">{{ feedback.message }}</div>
    <form class="mt-5 grid gap-4 sm:grid-cols-[1fr_1fr_auto] sm:items-end" @submit.prevent="run">
      <label><span class="label">First name</span><input v-model="firstName" class="field" required maxlength="100" pattern="[\p{L}][\p{L} -]{0,99}" title="문자, 공백, -만 입력하세요." :disabled="running" /></label>
      <label><span class="label">Last name</span><input v-model="lastName" class="field" required maxlength="100" pattern="[\p{L}][\p{L} -]{0,99}" title="문자, 공백, -만 입력하세요." :disabled="running" /></label>
      <button class="btn-primary" :disabled="busy" :aria-busy="action === 'check'"><span v-if="action === 'check'" class="button-spinner" aria-hidden="true" />{{ action === 'check' ? '조회 중...' : '조회 실행' }}</button>
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
        <div class="flex items-center gap-2"><h4 class="font-bold">History ({{ totalCount }})</h4><button class="history-refresh-button" type="button" :disabled="busy" :aria-busy="refreshing" aria-label="History 새로고침" title="History 새로고침" @click="refreshHistory"><span v-if="refreshing" class="button-spinner" aria-hidden="true" /><span v-else aria-hidden="true">↻</span></button></div>
        <div class="flex items-center gap-2 text-sm">
          <button class="btn-secondary" type="button" :disabled="busy || historyPage === 0" @click="goToHistoryPage(historyPage - 1)">이전</button>
          <span>{{ historyPage + 1 }} / {{ totalPages }}</span>
          <button class="btn-secondary" type="button" :disabled="busy || historyPage + 1 >= totalPages" @click="goToHistoryPage(historyPage + 1)">다음</button>
        </div>
      </div>
      <table class="data-table w-full min-w-[620px] text-left text-sm">
        <thead><tr><th class="py-2">Check ID</th><th>상태</th><th>요청 시각</th><th>완료 시각</th></tr></thead>
        <tbody>
          <tr v-if="history.length === 0"><td class="py-4 text-slate-500" colspan="4">조회 이력이 없습니다.</td></tr>
          <tr v-for="item in history" :key="item.checkId">
            <td class="py-3"><button class="row-link" type="button" :disabled="busy" @click="viewDetail(item.checkId)">{{ item.checkId }}</button></td>
            <td class="uppercase">{{ item.status }}</td><td>{{ new Date(item.createdAt).toLocaleString() }}</td><td>{{ item.completedAt ? new Date(item.completedAt).toLocaleString() : '-' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
