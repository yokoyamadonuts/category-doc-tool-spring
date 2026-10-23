import { QueryClient } from '@tanstack/react-query'
import { ApiError } from './queries'

const MAX_RETRIES = 3

/**
 * 4xx は何度試しても同じ結果になるので、すぐに誤りとして見せる。
 * 既定のままだと、存在しないプロジェクトを開いたとき、3回の再試行の間ずっと読み込み中になる。
 */
export function shouldRetry(failureCount: number, error: unknown): boolean {
  if (error instanceof ApiError && error.status >= 400 && error.status < 500) return false
  return failureCount < MAX_RETRIES
}

export function createQueryClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: shouldRetry } } })
}
