import { useId } from 'react'
import { useVerification, type Schemas } from '../../api/queries'
import { useProjectId } from '../projects/ProjectPage'

type Entry = Schemas['VerificationEntry']

export function VerificationView() {
  const { data: report, isPending, isError } = useVerification(useProjectId())

  if (isPending) return <p>検証しています</p>
  if (isError) return <p role="alert">検証の結果を読み込めませんでした</p>

  const entries = [...report.categories, ...report.functors, ...report.naturalTransformations]
  const errors = entries.reduce((sum, e) => sum + e.errors.length, 0)
  const warnings = entries.reduce((sum, e) => sum + e.warnings.length, 0)

  return (
    <>
      <p role="status" className={report.valid ? 'summary valid' : 'summary invalid'}>
        {report.valid ? 'すべての検証を通りました' : `誤りが ${errors} 件あります`}
        {warnings > 0 && `。警告が ${warnings} 件あります`}
      </p>
      <EntrySection title="カテゴリ" entries={report.categories} />
      <EntrySection title="関手" entries={report.functors} />
      <EntrySection title="自然変換" entries={report.naturalTransformations} />
    </>
  )
}

function EntrySection({ title, entries }: { title: string; entries: Entry[] }) {
  const headingId = useId()
  return (
    <section aria-labelledby={headingId}>
      <h3 id={headingId}>{title}</h3>
      {entries.length === 0 ? (
        <p>ありません</p>
      ) : (
        <ul className="verification-list">
          {entries.map((entry) => (
            <EntryItem key={entry.id} entry={entry} />
          ))}
        </ul>
      )}
    </section>
  )
}

function EntryItem({ entry }: { entry: Entry }) {
  const nameId = useId()
  return (
    <li aria-labelledby={nameId} className={entry.valid ? 'valid' : 'invalid'}>
      <span id={nameId} className="entry-name">
        {entry.name}
      </span>
      <span className="badge">{entry.valid ? '正しい' : '誤りあり'}</span>
      {entry.errors.length + entry.warnings.length > 0 && (
        <div className="messages">
          {entry.errors.map((message) => (
            <p key={message} className="error">
              <span className="label">誤り</span>
              {message}
            </p>
          ))}
          {entry.warnings.map((message) => (
            <p key={message} className="warning">
              <span className="label">警告</span>
              {message}
            </p>
          ))}
        </div>
      )}
    </li>
  )
}
