import { describe, it, expect, afterEach, vi } from 'vitest'
import { act, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderRoute, stubApi } from '../../test/renderRoute'
import { BASE, PROJECT_ID, authToDbPaths, infrastructureGraph, project } from '../../test/fixtures'

afterEach(() => {
  vi.unstubAllGlobals()
})

const replies = {
  [BASE]: project,
  [`${BASE}/categories/infrastructure/graph`]: infrastructureGraph,
  [`${BASE}/paths?from=infra-auth&to=infra-db`]: authToDbPaths,
  [`${BASE}/paths?from=infra-db&to=infra-auth`]: { from: 'infra-db', to: 'infra-auth', categoryId: 'infrastructure', shortest: [], all: [] },
}

describe('PathsView', () => {
  it('カテゴリと始点と終点を選んで探すと、最短経路と全経路を文書の名前で出す', async () => {
    const user = userEvent.setup()
    stubApi(replies)
    const { router } = renderRoute(`/projects/${PROJECT_ID}/paths`)

    await user.selectOptions(await screen.findByLabelText('カテゴリ'), 'Infrastructure')
    await screen.findAllByRole('option', { name: '認証サービス' })
    await user.selectOptions(screen.getByRole('combobox', { name: '始点' }), '認証サービス')
    await user.selectOptions(screen.getByLabelText('終点'), 'データベース')
    await user.click(screen.getByRole('button', { name: '経路を探す' }))

    const shortest = await screen.findByRole('region', { name: '最短経路' })
    expect(shortest).toHaveTextContent('認証サービス →connects→ データベース')
    const all = screen.getByRole('region', { name: '全経路 2 本' })
    expect(within(all).getAllByRole('listitem').map((li) => li.textContent)).toEqual([
      '認証サービス →connects→ データベース',
      '認証サービス →depends→ ユーザーサービス →connects→ データベース',
    ])
    expect(router.state.location.search).toBe('?category=infrastructure&from=infra-auth&to=infra-db')
  })

  it('URL の検索パラメータから探し、経路がなければそう知らせる', async () => {
    stubApi(replies)
    renderRoute(`/projects/${PROJECT_ID}/paths?category=infrastructure&from=infra-db&to=infra-auth`)

    expect(await screen.findByText('経路はありません')).toBeInTheDocument()
    expect(screen.getByLabelText('始点')).toHaveValue('infra-db')
  })

  it('始点と終点が同じなら探せない', async () => {
    const user = userEvent.setup()
    stubApi(replies)
    renderRoute(`/projects/${PROJECT_ID}/paths?category=infrastructure`)
    // 選択肢はカテゴリのグラフを読み込んでから出る
    await screen.findAllByRole('option', { name: '認証サービス' })

    await user.selectOptions(screen.getByRole('combobox', { name: '始点' }), '認証サービス')
    await user.selectOptions(screen.getByLabelText('終点'), '認証サービス')

    expect(screen.getByRole('button', { name: '経路を探す' })).toBeDisabled()
  })
  it('戻るや進むで URL が変われば、選択の欄も URL に合わせる', async () => {
    stubApi(replies)
    const { router } = renderRoute(`/projects/${PROJECT_ID}/paths?category=infrastructure&from=infra-auth&to=infra-db`)
    await screen.findByRole('region', { name: '最短経路' })

    await act(() => router.navigate(`/projects/${PROJECT_ID}/paths?category=infrastructure&from=infra-db&to=infra-auth`))

    await waitFor(() => expect(screen.getByLabelText('始点')).toHaveValue('infra-db'))
    expect(screen.getByLabelText('終点')).toHaveValue('infra-auth')
    expect(await screen.findByText('経路はありません')).toBeInTheDocument()
  })
})
