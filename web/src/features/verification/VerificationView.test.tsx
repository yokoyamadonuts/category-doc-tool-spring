import { describe, it, expect, afterEach, vi } from 'vitest'
import { screen, within } from '@testing-library/react'
import { renderRoute, stubApi } from '../../test/renderRoute'
import { BASE, PROJECT_ID, invalidReport, project, validReport } from '../../test/fixtures'

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('VerificationView', () => {
  it('すべて正しければ、そう知らせて項目ごとに印を付ける', async () => {
    stubApi({ [BASE]: project, [`${BASE}/verification`]: validReport })
    renderRoute(`/projects/${PROJECT_ID}/verification`)

    expect(await screen.findByRole('status')).toHaveTextContent('すべての検証を通りました')
    const categories = screen.getByRole('region', { name: 'カテゴリ' })
    expect(within(categories).getAllByRole('listitem')).toHaveLength(2)
    expect(within(categories).getAllByText('正しい')).toHaveLength(2)
    expect(within(screen.getByRole('region', { name: '自然変換' })).getByText('ありません')).toBeInTheDocument()
  })

  it('誤りと警告の数を数え、項目ごとに文言を並べる', async () => {
    stubApi({ [BASE]: project, [`${BASE}/verification`]: invalidReport })
    renderRoute(`/projects/${PROJECT_ID}/verification`)

    expect(await screen.findByRole('status')).toHaveTextContent('誤りが 3 件あります。警告が 1 件あります')
    const requirements = screen.getByRole('listitem', { name: 'Requirements' })
    expect(requirements).toHaveTextContent('誤りあり')
    expect(requirements).toHaveTextContent("Object 'req-auth' lacks an identity morphism")
    const specifications = screen.getByRole('listitem', { name: 'Specifications' })
    expect(specifications).toHaveTextContent('正しい')
    expect(specifications).toHaveTextContent('警告Missing composition: g∘f (a → c)')
  })
})
