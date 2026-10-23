import { Navigate, type RouteObject } from 'react-router'
import { Layout } from './Layout'
import { NotFound, RouteError } from './NotFound'
import { ProjectList } from './features/projects/ProjectList'
import { ProjectPage } from './features/projects/ProjectPage'
import { VerificationView } from './features/verification/VerificationView'
import { PathsView } from './features/paths/PathsView'
import { TraceabilityView } from './features/traceability/TraceabilityView'

export const routes: RouteObject[] = [
  {
    path: '/',
    element: <Layout />,
    errorElement: <RouteError />,
    children: [
      { index: true, element: <ProjectList /> },
      {
        path: 'projects/:projectId',
        element: <ProjectPage />,
        children: [
          { index: true, element: <Navigate to="graph" replace /> },
          {
            path: 'graph',
            // グラフの描画の部品は大きいので、開いたときに読み込む
            lazy: async () => ({ Component: (await import('./features/graph/GraphView')).GraphView }),
          },
          { path: 'verification', element: <VerificationView /> },
          { path: 'paths', element: <PathsView /> },
          { path: 'traceability', element: <TraceabilityView /> },
        ],
      },
      { path: '*', element: <NotFound /> },
    ],
  },
]
