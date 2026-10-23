import { render } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createMemoryRouter } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { vi } from 'vitest'
import { routes } from '../routes'

type Reply = { status: number; body: unknown }

/**
 * API の応答を、パスと検索パラメータの組か、パスだけで引けるように差し替える。
 * 登録していない URL は 404 を返す。
 */
export function stubApi(replies: Record<string, unknown | Reply>) {
  const requested: string[] = []
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: Request) => {
      const url = new URL(input.url)
      const key = url.pathname + url.search
      requested.push(decodeURIComponent(key))
      const found = replies[decodeURIComponent(key)] ?? replies[url.pathname]
      const reply: Reply =
        found === undefined
          ? { status: 404, body: { status: 404, title: 'Not Found' } }
          : isReply(found)
            ? found
            : { status: 200, body: found }
      return new Response(JSON.stringify(reply.body), {
        status: reply.status,
        headers: { 'Content-Type': reply.status < 400 ? 'application/json' : 'application/problem+json' },
      })
    }),
  )
  return { requested }
}

function isReply(value: unknown): value is Reply {
  return typeof value === 'object' && value !== null && 'status' in value && 'body' in value
    && Object.keys(value).length === 2
}

/** アプリの全体を、指定した URL から描く。 */
export function renderRoute(path: string) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  render(
    <QueryClientProvider client={client}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
  return { router }
}
