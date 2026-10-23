import { describe, it, expect, afterEach, vi } from 'vitest'
import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { renderRoute, stubApi } from '../../test/renderRoute'
import { BASE, PROJECT_ID, project, traceTable } from '../../test/fixtures'

afterEach(() => {
  vi.unstubAllGlobals()
})

const replies = {
  [BASE]: project,
  [`${BASE}/traceability?functors=specify,deploy`]: traceTable,
}

describe('TraceabilityView', () => {
  it('関手を順に加えると、つながる関手だけを候補に出し、表を作る', async () => {
    const user = userEvent.setup()
    stubApi(replies)
    const { router } = renderRoute(`/projects/${PROJECT_ID}/traceability`)

    const next = await screen.findByLabelText('次の関手')
    expect(within(next).getAllByRole('option').map((o) => o.textContent)).toEqual(['選んでください', 'Specify', 'Deploy', 'Self'])
    await user.selectOptions(next, 'Specify')
    await user.click(screen.getByRole('button', { name: '加える' }))
    expect(within(screen.getByLabelText('次の関手')).getAllByRole('option').map((o) => o.textContent))
      .toEqual(['選んでください', 'Deploy'])
    await user.selectOptions(screen.getByLabelText('次の関手'), 'Deploy')
    await user.click(screen.getByRole('button', { name: '加える' }))

    expect(router.state.location.search).toBe('?functors=specify%2Cdeploy')
    expect(screen.getByLabelText('次の関手')).toBeDisabled()

    const table = await screen.findByRole('table')
    expect(within(table).getAllByRole('columnheader').map((th) => th.textContent))
      .toEqual(['Requirements', 'Specifications', 'Infrastructure'])
    expect(within(table).getByRole('row', { name: /ユーザー管理要件/ })).toHaveTextContent('ユーザー管理要件spec-userinfra-user')
    const incomplete = within(table).getByRole('row', { name: /認証システム要件/ })
    expect(incomplete).toHaveTextContent('認証システム要件spec-auth対応なし')
    expect(incomplete).toHaveClass('incomplete')
  })

  it('最後の関手を外せる', async () => {
    const user = userEvent.setup()
    stubApi(replies)
    const { router } = renderRoute(`/projects/${PROJECT_ID}/traceability?functors=specify,deploy`)

    await screen.findByRole('table')
    await user.click(screen.getByRole('button', { name: '最後を外す' }))

    expect(router.state.location.search).toBe('?functors=specify')
  })
})
