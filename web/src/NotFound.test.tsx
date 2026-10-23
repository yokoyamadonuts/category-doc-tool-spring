import { describe, it, expect, afterEach, vi } from 'vitest'
import { screen } from '@testing-library/react'
import { renderRoute, stubApi } from './test/renderRoute'

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('存在しない URL', () => {
  it('見つからないと知らせ、一覧へのリンクを出す', async () => {
    stubApi({})
    renderRoute('/nowhere')

    expect(await screen.findByRole('heading', { name: 'ページが見つかりません' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'プロジェクトの一覧へ' })).toHaveAttribute('href', '/')
  })
})
