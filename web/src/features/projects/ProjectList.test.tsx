import { describe, it, expect, afterEach, vi } from 'vitest'
import { screen } from '@testing-library/react'
import { renderRoute, stubApi } from '../../test/renderRoute'
import { PROJECT_ID } from '../../test/fixtures'

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('ProjectList', () => {
  it('プロジェクトがないとき、空の案内を出す', async () => {
    stubApi({ '/api/projects': [] })
    renderRoute('/')
    expect(await screen.findByText('プロジェクトはまだありません')).toBeInTheDocument()
  })

  it('プロジェクトがあるとき、名前をグラフの画面へのリンクにして並べる', async () => {
    stubApi({ '/api/projects': [{ id: PROJECT_ID, name: 'math-docs', importedAt: '2026-10-23T01:02:03Z' }] })
    renderRoute('/')
    expect(await screen.findByRole('link', { name: 'math-docs' })).toHaveAttribute('href', `/projects/${PROJECT_ID}/graph`)
  })

  it('API が失敗したとき、エラーを知らせる', async () => {
    stubApi({ '/api/projects': { status: 500, body: { title: 'Internal Server Error', status: 500 } } })
    renderRoute('/')
    expect(await screen.findByRole('alert')).toHaveTextContent('プロジェクトを読み込めませんでした')
  })
})
