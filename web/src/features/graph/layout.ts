import ElkApi, { type ELK, type ElkNode } from 'elkjs/lib/elk-api.js'
import elkWorkerUrl from 'elkjs/lib/elk-worker.min.js?url'
import { MarkerType, type Edge, type Node } from '@xyflow/react'
import type { CategoryGraph } from '../../api/queries'

export const NODE_WIDTH = 200
export const NODE_HEIGHT = 56

export type DocumentNode = Node<{ label: string; domain: string }>

type Point = { x: number; y: number }

/** ELK が引いた矢印の経路。React Flow の既定の曲線は間の節点を突き抜けるので、この点を結んで描く。 */
export type RoutedEdge = Edge<{ points: Point[]; labelPosition?: Point }, 'routed'>

export type GraphLayout = {
  nodes: DocumentNode[]
  edges: RoutedEdge[]
  /** 描かなかった恒等射の数 */
  omittedIdentities: number
}

let workerElk: ELK | undefined

/**
 * ブラウザでは ELK を Web Worker で動かす。配置の計算で画面が固まらず、1.4MB ある ELK の本体も
 * 画面の JavaScript に含めずに済む。Worker のないテストでは、呼び出し側が同期版の ELK を渡す。
 */
function elkInWorker(): ELK {
  workerElk ??= new ElkApi({ workerFactory: () => new Worker(elkWorkerUrl) })
  return workerElk
}

/**
 * 文書を節点、関係を矢印にして、ELK の階層型の配置で左から右へ並べる。
 * 恒等射はどの文書にもあるので描かず、数だけ返す。
 * 存在しない文書を指す関係は描けないので除く。その誤りは検証の画面で見せる。
 */
export async function layoutGraph(graph: CategoryGraph, elk: ELK = elkInWorker()): Promise<GraphLayout> {
  const ids = new Set(graph.objects.map((o) => o.id))
  const arrows = graph.morphisms.filter((m) => !m.identity && ids.has(m.source) && ids.has(m.target))
  const omittedIdentities = graph.morphisms.filter((m) => m.identity).length

  if (graph.objects.length === 0) return { nodes: [], edges: [], omittedIdentities }

  const input: ElkNode = {
    id: graph.id,
    layoutOptions: {
      'elk.algorithm': 'layered',
      'elk.direction': 'RIGHT',
      'elk.spacing.nodeNode': '40',
      'elk.layered.spacing.nodeNodeBetweenLayers': '80',
      'elk.edgeRouting': 'ORTHOGONAL',
      'elk.edgeLabels.placement': 'CENTER',
    },
    children: graph.objects.map((o) => ({ id: o.id, width: NODE_WIDTH, height: NODE_HEIGHT })),
    edges: arrows.map((m) => ({
      id: m.id,
      sources: [m.source],
      targets: [m.target],
      // 関係の名前の場所を空けさせる。幅は文字数からの目安
      labels: [{ id: `${m.id}-label`, text: m.name, width: m.name.length * 7 + 8, height: 16 }],
    })),
  }
  const laidOut = await elk.layout(input)
  const positions = new Map(laidOut.children?.map((c) => [c.id, { x: c.x ?? 0, y: c.y ?? 0 }]))
  const routes = new Map(
    laidOut.edges?.map((e) => {
      const section = e.sections?.[0]
      const points = section ? [section.startPoint, ...(section.bendPoints ?? []), section.endPoint] : []
      const label = e.labels?.[0]
      const labelPosition =
        label?.x !== undefined && label.y !== undefined
          ? { x: label.x + (label.width ?? 0) / 2, y: label.y + (label.height ?? 0) / 2 }
          : undefined
      return [e.id, { points, labelPosition }]
    }),
  )

  return {
    nodes: graph.objects.map((o) => ({
      id: o.id,
      position: positions.get(o.id) ?? { x: 0, y: 0 },
      width: NODE_WIDTH,
      height: NODE_HEIGHT,
      data: { label: o.title, domain: o.domain },
      sourcePosition: 'right' as const,
      targetPosition: 'left' as const,
    })) as DocumentNode[],
    edges: arrows.map((m) => ({
      id: m.id,
      type: 'routed' as const,
      source: m.source,
      target: m.target,
      label: m.name,
      markerEnd: { type: MarkerType.ArrowClosed },
      data: routes.get(m.id) ?? { points: [] },
    })),
    omittedIdentities,
  }
}
