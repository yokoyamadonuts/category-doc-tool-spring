import { useId, useState } from 'react'
import { useSearchParams } from 'react-router'
import { useTraceability } from '../../api/queries'
import { useProjectDetail, useProjectId } from '../projects/ProjectPage'
import { nextCandidates } from './chain'

export function TraceabilityView() {
  const projectId = useProjectId()
  const project = useProjectDetail()
  const [searchParams, setSearchParams] = useSearchParams()
  const chain = (searchParams.get('functors') ?? '').split(',').filter((id) => id !== '')
  const [selected, setSelected] = useState('')
  const selectId = useId()

  const candidates = nextCandidates(project.functors, chain)
  const table = useTraceability(projectId, chain)
  const categoryName = (id: string) => project.categories.find((c) => c.id === id)?.name ?? id
  const functorName = (id: string) => project.functors.find((f) => f.id === id)?.name ?? id

  function setChain(next: string[]) {
    setSelected('')
    setSearchParams(next.length === 0 ? {} : { functors: next.join(',') })
  }

  return (
    <>
      <div className="toolbar">
        <label htmlFor={selectId}>次の関手</label>
        <select
          id={selectId}
          value={selected}
          onChange={(e) => setSelected(e.target.value)}
          disabled={candidates.length === 0}
        >
          <option value="" disabled>
            選んでください
          </option>
          {candidates.map((f) => (
            <option key={f.id} value={f.id}>
              {f.name}
            </option>
          ))}
        </select>
        <button type="button" disabled={selected === ''} onClick={() => setChain([...chain, selected])}>
          加える
        </button>
        {chain.length > 0 && (
          <button type="button" onClick={() => setChain(chain.slice(0, -1))}>
            最後を外す
          </button>
        )}
      </div>

      {chain.length === 0 ? (
        <p>最初の関手を選んでください。続く関手は、前の関手の先のカテゴリから始まるものだけが選べます。</p>
      ) : (
        <ol className="chain">
          {chain.map((id) => (
            <li key={id}>{functorName(id)}</li>
          ))}
        </ol>
      )}

      {table.isError && <p role="alert">追跡の表を作れませんでした</p>}
      {table.data && (
        <table className="traceability">
          <thead>
            <tr>
              <th scope="col">{categoryName(table.data.functors[0].sourceCategory)}</th>
              {table.data.functors.map((f) => (
                <th key={f.id} scope="col">
                  {categoryName(f.targetCategory)}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {table.data.rows.map((row) => (
              <tr key={row.objectId} className={row.complete ? 'complete' : 'incomplete'}>
                <th scope="row">{row.title}</th>
                {table.data.functors.map((f, i) => (
                  <td key={f.id}>{row.mappedObjectIds[i] ?? <span className="missing">対応なし</span>}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}
