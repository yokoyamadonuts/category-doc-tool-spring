import { Link, isRouteErrorResponse, useRouteError } from 'react-router'

export function NotFound() {
  return (
    <>
      <h2>ページが見つかりません</h2>
      <p>
        <Link to="/">プロジェクトの一覧へ</Link>
      </p>
    </>
  )
}

/** 画面の描画で想定外の誤りが起きたとき。React Router の既定の英語の画面の代わりに出す。 */
export function RouteError() {
  const error = useRouteError()
  if (isRouteErrorResponse(error) && error.status === 404) return <NotFound />
  return (
    <>
      <h2>画面を表示できませんでした</h2>
      <p>新しい版に更新された直後なら、再読み込みすると直ります。</p>
      <p>
        <Link to="/">プロジェクトの一覧へ</Link>
      </p>
    </>
  )
}
