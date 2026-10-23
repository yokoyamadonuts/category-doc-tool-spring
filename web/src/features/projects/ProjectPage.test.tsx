import { describe, it, expect, afterEach, vi } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { renderRoute, stubApi } from '../../test/renderRoute'
import { BASE, PROJECT_ID, project, validReport } from '../../test/fixtures'

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('ProjectPage', () => {
  it('プロジェクト名と4つのタブを出し、開いている画面のタブに印を付ける', async () => {
    stubApi({ [BASE]: project, [`${BASE}/verification`]: validReport })
    renderRoute(`/projects/${PROJECT_ID}/verification`)

    expect(await screen.findByRole('heading', { level: 2, name: 'dev-project' })).toBeInTheDocument()
    const tabs = screen.getByRole('navigation', { name: '画面' })
    expect(tabs).toHaveTextContent('グラフ検証経路追跡')
    expect(screen.getByRole('link', { name: '検証' })).toHaveAttribute('aria-current', 'page')
  })

  it('プロジェクトの URL はグラフの画面に移す', async () => {
    stubApi({ [BASE]: project })
    const { router } = renderRoute(`/projects/${PROJECT_ID}`)

    await waitFor(() => expect(router.state.location.pathname).toBe(`/projects/${PROJECT_ID}/graph`))
  })

  it('ないプロジェクトは、見つからないと知らせる', async () => {
    stubApi({})
    renderRoute(`/projects/${PROJECT_ID}/verification`)

    expect(await screen.findByRole('alert')).toHaveTextContent('プロジェクトが見つかりません')
  })
})
