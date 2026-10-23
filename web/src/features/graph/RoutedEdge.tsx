import { BaseEdge, EdgeLabelRenderer, getStraightPath, type EdgeProps } from '@xyflow/react'
import type { RoutedEdge as RoutedEdgeType } from './layout'

/** ELK が引いた折れ線で矢印を描く。経路がなければ両端を直線で結ぶ。 */
export function RoutedEdge(props: EdgeProps<RoutedEdgeType>) {
  const points = props.data?.points ?? []
  const [straight, straightX, straightY] = getStraightPath(props)
  const path = points.length >= 2 ? `M ${points.map((p) => `${p.x} ${p.y}`).join(' L ')}` : straight
  const label = props.data?.labelPosition ?? { x: straightX, y: straightY }

  return (
    <>
      <BaseEdge id={props.id} path={path} markerEnd={props.markerEnd} style={props.style} />
      {props.label && (
        <EdgeLabelRenderer>
          <div
            className="edge-label"
            style={{ transform: `translate(-50%, -50%) translate(${label.x}px, ${label.y}px)` }}
          >
            {props.label}
          </div>
        </EdgeLabelRenderer>
      )}
    </>
  )
}
