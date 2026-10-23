import { describe, it, expect } from 'vitest'
import { shouldRetry } from './queryClient'
import { ApiError } from './queries'

describe('shouldRetry', () => {
  it.each([400, 404, 422])('%d は何度試しても同じなので、再試行しない', (status) => {
    expect(shouldRetry(0, new ApiError(status))).toBe(false)
  })

  it('500 や通信の失敗は、3回まで再試行する', () => {
    expect(shouldRetry(0, new ApiError(500))).toBe(true)
    expect(shouldRetry(2, new TypeError('Failed to fetch'))).toBe(true)
    expect(shouldRetry(3, new ApiError(503))).toBe(false)
  })
})
