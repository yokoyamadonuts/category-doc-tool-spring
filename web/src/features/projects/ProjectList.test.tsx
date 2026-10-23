import { describe, it, expect, vi, afterEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ProjectList } from './ProjectList'

function renderWithClient() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={client}>
      <ProjectList />
    </QueryClientProvider>,
  )
}

function respondWith(status: number, body: unknown) {
  vi.stubGlobal(
    'fetch',
    vi.fn(async () =>
      new Response(JSON.stringify(body), {
        status,
        headers: { 'Content-Type': 'application/json' },
      }),
    ),
  )
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('ProjectList', () => {
  it('プロジェクトがないとき、空の案内を出す', async () => {
    respondWith(200, [])
    renderWithClient()
    expect(await screen.findByText('プロジェクトはまだありません')).toBeInTheDocument()
  })

  it('プロジェクトがあるとき、名前を並べる', async () => {
    respondWith(200, [
      { id: '0f8c3b5e-1d2a-4c7b-9e6f-123456789abc', name: 'math-docs', importedAt: '2026-10-23T01:02:03Z' },
    ])
    renderWithClient()
    expect(await screen.findByRole('listitem')).toHaveTextContent('math-docs')
  })

  it('API が失敗したとき、エラーを知らせる', async () => {
    respondWith(500, { title: 'Internal Server Error', status: 500 })
    renderWithClient()
    expect(await screen.findByRole('alert')).toHaveTextContent('プロジェクトを読み込めませんでした')
  })
})
