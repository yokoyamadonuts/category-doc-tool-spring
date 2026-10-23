import { NavLink, Outlet, useOutletContext, useParams } from 'react-router'
import { ApiError, useProject, type ProjectDetail } from '../../api/queries'

const TABS = [
  { to: 'graph', label: 'グラフ' },
  { to: 'verification', label: '検証' },
  { to: 'paths', label: '経路' },
  { to: 'traceability', label: '追跡' },
] as const

export function ProjectPage() {
  const projectId = useProjectId()
  const { data: project, isPending, error } = useProject(projectId)

  if (isPending) return <p>読み込み中です</p>
  if (error) {
    return (
      <p role="alert">
        {error instanceof ApiError && error.status === 404
          ? 'プロジェクトが見つかりません'
          : 'プロジェクトを読み込めませんでした'}
      </p>
    )
  }

  return (
    <>
      <h2>{project.name}</h2>
      <nav aria-label="画面" className="tabs">
        {TABS.map((tab) => (
          <NavLink key={tab.to} to={tab.to}>
            {tab.label}
          </NavLink>
        ))}
      </nav>
      <Outlet context={project satisfies ProjectDetail} />
    </>
  )
}

/** 子の画面から、読み込み済みのプロジェクトを受け取る。 */
export function useProjectDetail() {
  return useOutletContext<ProjectDetail>()
}

export function useProjectId() {
  const { projectId } = useParams()
  if (projectId === undefined) throw new Error('projectId のない URL で使われました')
  return projectId
}
