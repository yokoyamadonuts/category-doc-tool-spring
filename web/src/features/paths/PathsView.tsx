import { useId, useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router'
import { useCategoryGraph, usePaths, type MorphismView } from '../../api/queries'
import { useProjectDetail, useProjectId } from '../projects/ProjectPage'

export function PathsView() {
  const projectId = useProjectId()
  const [searchParams] = useSearchParams()
  const category = searchParams.get('category') ?? undefined
  const searchedFrom = searchParams.get('from') ?? undefined
  const searchedTo = searchParams.get('to') ?? undefined

  const graph = useCategoryGraph(projectId, category)
  const paths = usePaths(projectId, searchedFrom, searchedTo)
  const titles = new Map(graph.data?.objects.map((o) => [o.id, o.title]))

  return (
    <>
      {/* 戻るや進むで URL が変わったら、選択の欄を作り直して URL の値に合わせる */}
      <PathsForm
        key={`${category}|${searchedFrom}|${searchedTo}`}
        category={category}
        initialFrom={searchedFrom ?? ''}
        initialTo={searchedTo ?? ''}
        objects={graph.data?.objects}
      />

      {category === undefined && <p>カテゴリと始点と終点を選んで、経路を探してください。</p>}
      {paths.isError && <p role="alert">経路を探せませんでした</p>}
      {paths.data &&
        (paths.data.all.length === 0 ? (
          <p>経路はありません</p>
        ) : (
          <>
            <PathSection title="最短経路">
              <p className="path">{describe(paths.data.shortest, titles)}</p>
            </PathSection>
            <PathSection title={`全経路 ${paths.data.all.length} 本`}>
              <ol className="paths">
                {paths.data.all.map((path) => (
                  <li key={path.map((m) => m.id).join('/')} className="path">
                    {describe(path, titles)}
                  </li>
                ))}
              </ol>
            </PathSection>
          </>
        ))}
    </>
  )
}

/** 選んでいる途中の始点と終点は、探すボタンを押すまで URL に移さない。 */
function PathsForm(props: {
  category: string | undefined
  initialFrom: string
  initialTo: string
  objects: { id: string; title: string }[] | undefined
}) {
  const project = useProjectDetail()
  const [, setSearchParams] = useSearchParams()
  const [from, setFrom] = useState(props.initialFrom)
  const [to, setTo] = useState(props.initialTo)
  const ids = { category: useId(), from: useId(), to: useId() }

  function search(event: FormEvent) {
    event.preventDefault()
    setSearchParams({ category: props.category!, from, to })
  }

  return (
    <form className="toolbar" onSubmit={search}>
      <label htmlFor={ids.category}>カテゴリ</label>
      <select
        id={ids.category}
        value={props.category ?? ''}
        onChange={(e) => setSearchParams({ category: e.target.value })}
      >
        <option value="" disabled>
          選んでください
        </option>
        {project.categories.map((c) => (
          <option key={c.id} value={c.id}>
            {c.name}
          </option>
        ))}
      </select>
      <label htmlFor={ids.from}>始点</label>
      <ObjectSelect id={ids.from} value={from} onChange={setFrom} objects={props.objects} />
      <label htmlFor={ids.to}>終点</label>
      <ObjectSelect id={ids.to} value={to} onChange={setTo} objects={props.objects} />
      <button type="submit" disabled={!props.category || !from || !to || from === to}>
        経路を探す
      </button>
    </form>
  )
}

function ObjectSelect(props: {
  id: string
  value: string
  onChange: (value: string) => void
  objects: { id: string; title: string }[] | undefined
}) {
  return (
    <select id={props.id} value={props.value} onChange={(e) => props.onChange(e.target.value)} disabled={!props.objects}>
      <option value="" disabled>
        選んでください
      </option>
      {props.objects?.map((o) => (
        <option key={o.id} value={o.id}>
          {o.title}
        </option>
      ))}
    </select>
  )
}

function PathSection({ title, children }: { title: string; children: React.ReactNode }) {
  const headingId = useId()
  return (
    <section aria-labelledby={headingId}>
      <h3 id={headingId}>{title}</h3>
      {children}
    </section>
  )
}

/** 文書 A →関係→ 文書 B の形の文。 */
function describe(path: MorphismView[], titles: Map<string, string>) {
  const title = (id: string) => titles.get(id) ?? id
  return [title(path[0].source), ...path.map((m) => `→${m.name}→ ${title(m.target)}`)].join(' ')
}
