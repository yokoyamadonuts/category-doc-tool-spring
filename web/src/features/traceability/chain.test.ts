import { describe, it, expect } from 'vitest'
import { nextCandidates } from './chain'
import { project } from '../../test/fixtures'

const [specify, deploy, self] = project.functors

describe('nextCandidates', () => {
  it('列が空なら、すべての関手が候補', () => {
    expect(nextCandidates(project.functors, [])).toEqual([specify, deploy, self])
  })

  it('前の関手の先から始まる関手だけが候補', () => {
    expect(nextCandidates(project.functors, ['specify'])).toEqual([deploy])
  })

  it('一度使った関手は候補にしない。自分に戻る関手で列が終わらなくなるのを防ぐ', () => {
    expect(nextCandidates(project.functors, ['self'])).toEqual([specify])
  })

  it('続く関手がなければ空', () => {
    expect(nextCandidates(project.functors, ['specify', 'deploy'])).toEqual([])
  })
})
