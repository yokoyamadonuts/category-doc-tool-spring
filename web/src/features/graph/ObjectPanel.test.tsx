import { describe, it, expect, afterEach, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ObjectPanel } from './ObjectPanel'
import { stubApi } from '../../test/renderRoute'
import { BASE, PROJECT_ID, authObject } from '../../test/fixtures'

afterEach(() => {
  vi.unstubAllGlobals()
})

function renderPanel(objectId: string, onClose = vi.fn<() => void>()) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={client}>
      <ObjectPanel projectId={PROJECT_ID} objectId={objectId} onClose={onClose} />
    </QueryClientProvider>,
  )
  return { onClose }
}

describe('ObjectPanel', () => {
  it('文書の題名、メタデータ、本文を出す', async () => {
    stubApi({ [`${BASE}/objects/req-auth`]: authObject })
    renderPanel('req-auth')

    const panel = screen.getByRole('complementary', { name: '文書の詳細' })
    expect(await screen.findByRole('heading', { name: '認証システム要件' })).toBeInTheDocument()
    expect(panel).toHaveTextContent('stakeholderセキュリティチーム')
    expect(panel).toHaveTextContent('tags["auth","security"]')
    expect(screen.getByText(/## 概要/)).toBeInTheDocument()
  })

  it('本文がなければそう知らせる', async () => {
    stubApi({ [`${BASE}/objects/req-auth`]: { ...authObject, content: '' } })
    renderPanel('req-auth')

    expect(await screen.findByText('本文はありません')).toBeInTheDocument()
  })

  it('閉じるボタンで閉じる', async () => {
    const user = userEvent.setup()
    stubApi({ [`${BASE}/objects/req-auth`]: authObject })
    const { onClose } = renderPanel('req-auth')

    await user.click(await screen.findByRole('button', { name: '閉じる' }))

    expect(onClose).toHaveBeenCalled()
  })
})
