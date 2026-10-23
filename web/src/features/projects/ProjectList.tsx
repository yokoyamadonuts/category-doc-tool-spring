import { Link } from 'react-router'
import { useProjects } from '../../api/queries'

export function ProjectList() {
  const { data, isPending, isError } = useProjects()

  return (
    <>
      <h2>プロジェクト</h2>
      {isPending ? (
        <p>読み込み中です</p>
      ) : isError ? (
        <p role="alert">プロジェクトを読み込めませんでした</p>
      ) : data.length === 0 ? (
        <p>プロジェクトはまだありません</p>
      ) : (
        <ul className="project-list">
          {data.map((project) => (
            <li key={project.id}>
              <Link to={`/projects/${project.id}/graph`}>{project.name}</Link>
              <time dateTime={project.importedAt}>{new Date(project.importedAt).toLocaleString('ja-JP')}</time>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
