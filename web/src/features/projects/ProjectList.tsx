import { useQuery } from '@tanstack/react-query'
import { api } from '../../api/client'

export function ProjectList() {
  const { data, isPending, isError } = useQuery({
    queryKey: ['projects'],
    queryFn: async () => {
      const { data, error } = await api.GET('/api/projects')
      if (error !== undefined || data === undefined) {
        throw new Error('プロジェクトの一覧を取得できませんでした')
      }
      return data
    },
  })

  if (isPending) return <p>読み込み中です</p>
  if (isError) return <p role="alert">プロジェクトを読み込めませんでした</p>
  if (data.length === 0) return <p>プロジェクトはまだありません</p>

  return (
    <ul>
      {data.map((project) => (
        <li key={project.id}>{project.name}</li>
      ))}
    </ul>
  )
}
