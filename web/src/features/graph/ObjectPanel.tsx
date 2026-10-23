import { useObject } from '../../api/queries'

type Props = {
  projectId: string
  objectId: string
  onClose: () => void
}

/** 選んだ文書のメタデータと本文。本文は Markdown のまま出す。 */
export function ObjectPanel({ projectId, objectId, onClose }: Props) {
  const { data: object, isPending, isError } = useObject(projectId, objectId)

  return (
    <aside aria-label="文書の詳細" className="object-panel">
      <button type="button" className="close" onClick={onClose}>
        閉じる
      </button>
      {isPending ? (
        <p>読み込み中です</p>
      ) : isError ? (
        <p role="alert">文書を読み込めませんでした</p>
      ) : (
        <>
          <h3>{object.title}</h3>
          <dl>
            <dt>ID</dt>
            <dd>{object.id}</dd>
            <dt>ドメイン</dt>
            <dd>{object.domain}</dd>
            {Object.entries(object.metadata).map(([key, value]) => (
              <div key={key}>
                <dt>{key}</dt>
                <dd>{typeof value === 'string' ? value : JSON.stringify(value)}</dd>
              </div>
            ))}
          </dl>
          <h4>本文</h4>
          {object.content === '' ? <p>本文はありません</p> : <pre className="content">{object.content}</pre>}
        </>
      )}
    </aside>
  )
}
