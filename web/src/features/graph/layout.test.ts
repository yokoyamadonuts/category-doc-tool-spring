import { describe, it, expect } from 'vitest'
import ELK from 'elkjs/lib/elk.bundled.js'
import { layoutGraph as layoutWith } from './layout'
import { infrastructureGraph } from '../../test/fixtures'

// Worker のない環境なので、同期版の ELK を渡す
const elk = new ELK()
const layoutGraph = (graph: Parameters<typeof layoutWith>[0]) => layoutWith(graph, elk)

describe('layoutGraph', () => {
  it('文書を節点、恒等射でない関係を矢印にし、恒等射の数を数える', async () => {
    const layout = await layoutGraph(infrastructureGraph)

    expect(layout.nodes.map((n) => [n.id, n.data.label])).toEqual([
      ['infra-user', 'ユーザーサービス'],
      ['infra-auth', '認証サービス'],
      ['infra-db', 'データベース'],
    ])
    expect(layout.edges.map((e) => [e.id, e.source, e.target, e.label])).toEqual([
      ['user-db', 'infra-user', 'infra-db', 'connects'],
      ['auth-db', 'infra-auth', 'infra-db', 'connects'],
      ['auth-user', 'infra-auth', 'infra-user', 'depends'],
    ])
    expect(layout.omittedIdentities).toBe(3)
  })

  it('左から右へ、関係の向きに沿って並べる', async () => {
    const { nodes } = await layoutGraph(infrastructureGraph)
    const x = Object.fromEntries(nodes.map((n) => [n.id, n.position.x]))

    expect(x['infra-auth']).toBeLessThan(x['infra-user'])
    expect(x['infra-user']).toBeLessThan(x['infra-db'])
  })

  it('節点どうしは重ならない', async () => {
    const { nodes } = await layoutGraph(infrastructureGraph)

    for (const a of nodes) {
      for (const b of nodes) {
        if (a === b) continue
        const apart =
          Math.abs(a.position.x - b.position.x) >= a.width! || Math.abs(a.position.y - b.position.y) >= a.height!
        expect(apart, `${a.id} と ${b.id}`).toBe(true)
      }
    }
  })

  it('矢印は ELK が引いた経路をたどり、節点を突き抜けない', async () => {
    const { nodes, edges } = await layoutGraph(infrastructureGraph)
    const user = nodes.find((n) => n.id === 'infra-user')!
    const authToDb = edges.find((e) => e.id === 'auth-db')!
    const points = authToDb.data!.points

    expect(points.length).toBeGreaterThanOrEqual(2)
    // 層を1つ飛ばす矢印が、間にあるユーザーサービスの節点の中を通らない
    const inside = (p: { x: number; y: number }) =>
      p.x > user.position.x && p.x < user.position.x + user.width! &&
      p.y > user.position.y && p.y < user.position.y + user.height!
    for (let i = 0; i + 1 < points.length; i++) {
      const mid = { x: (points[i].x + points[i + 1].x) / 2, y: (points[i].y + points[i + 1].y) / 2 }
      expect(inside(mid), `区間 ${i}`).toBe(false)
    }
    expect(authToDb.data!.labelPosition).toBeDefined()
  })

  it('文書がなければ空', async () => {
    const layout = await layoutGraph({ id: 'e', name: 'E', objects: [], morphisms: [] })

    expect(layout).toEqual({ nodes: [], edges: [], omittedIdentities: 0 })
  })
})
