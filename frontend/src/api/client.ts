import axios, { AxiosError } from 'axios'

export const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
})

export async function ensureCsrf() {
  const response = await api.get<{ token: string; headerName: string }>('/auth/csrf')
  api.defaults.headers.common[response.data.headerName] = response.data.token
}

export function errorMessage(error: unknown): string {
  if (error instanceof AxiosError) {
    return error.response?.data?.message || '요청을 처리하지 못했습니다.'
  }
  return error instanceof Error ? error.message : '요청을 처리하지 못했습니다.'
}
