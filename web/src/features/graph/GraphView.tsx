import { useEffect, useId } from 'react'
import { useSearchParams } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { Background, Controls, ReactFlow, useReactFlow, useStore } from '@xyflow/react'
import { useCategoryGraph } from '../../api/queries'
import { useProjectDetail, useProjectId } from '../projects/ProjectPage'
import { layoutGraph } from './layout'
import { ObjectPanel } from './ObjectPanel'
import { RoutedEdge } from './RoutedEdge'

// 描画のたびに作り直すと React Flow が警告するので、外に置く
const edgeTypes = { routed: RoutedEdge }

export function GraphView() {
  const projectId = useProjectId()
  const project = useProjectDetail()
  const [searchParams, setSearchParams] = useSearchParams()
  const category = searchParams.get('category') ?? project.categories[0]?.id
  const selected = searchParams.get('object') ?? undefined
  const selectId = useId()

  const graph = useCategoryGraph(projectId, category)
  // 配置の計算は非同期なので、取得と同じくクエリとして扱い、カテゴリごとに覚えておく
  const layout = useQuery({
    queryKey: ['projects', projectId, 'categories', category, 'layout'],
    queryFn: () => layoutGraph(graph.data!),
    enabled: graph.data !== undefined,
  })

  function select(params: { category?: string; object?: string }) {
    const next: Record<string, string> = {}
    if (params.category ?? category) next.category = (params.category ?? category)!
    if (params.object) next.object = params.object
    setSearchParams(next)
  }

  if (project.categories.length === 0) return <p>カテゴリがありません</p>

  return (
    <>
      <div className="toolbar">
        <label htmlFor={selectId}>カテゴリ</label>
        <select id={selectId} value={category} onChange={(e) => select({ category: e.target.value })}>
          {project.categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
        {layout.data !== undefined && layout.data.omittedIdentities > 0 && (
          <span className="note">恒等射 {layout.data.omittedIdentities} 本は描いていません</span>
        )}
      </div>
      {graph.isError || layout.isError ? (
        <p role="alert">グラフを読み込めませんでした</p>
      ) : (
        <div className={selected ? 'graph-area with-panel' : 'graph-area'}>
          <div className="graph-canvas">
            {layout.data && (
              <ReactFlow
                key={category}
                nodes={layout.data.nodes.map((n) => ({ ...n, selected: n.id === selected }))}
                edges={layout.data.edges}
                edgeTypes={edgeTypes}
                onNodeClick={(_, node) => select({ object: node.id })}
                nodesConnectable={false}
                // 矢印は配置したときの経路で描くので、節点を動かせないようにする
                nodesDraggable={false}
                colorMode="system"
                minZoom={0.2}
                fitView
              >
                <Background />
                <Controls showInteractive={false} />
                <FitViewOnResize />
              </ReactFlow>
            )}
          </div>
          {selected && <ObjectPanel projectId={projectId} objectId={selected} onClose={() => select({})} />}
        </div>
      )}
    </>
  )
}

/** 文書の詳細を開くと描画の領域が狭まるので、大きさが変わるたびに全体が収まるように合わせ直す。 */
function FitViewOnResize() {
  const { fitView } = useReactFlow()
  const width = useStore((state) => state.width)
  const height = useStore((state) => state.height)
  useEffect(() => {
    // 大きさを測る前は合わせようがないので待つ
    if (width > 0 && height > 0) void fitView({ padding: 0.2 })
  }, [width, height, fitView])
  return null
}
