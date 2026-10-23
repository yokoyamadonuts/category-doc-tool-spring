import { Link, Outlet } from 'react-router'

export function Layout() {
  return (
    <>
      <header className="site-header">
        <h1>
          <Link to="/">CatDoc</Link>
        </h1>
      </header>
      <main>
        <Outlet />
      </main>
    </>
  )
}
