import createClient from 'openapi-fetch'
import type { paths } from './schema'

// fetch は呼び出すたびに参照する。テストで差し替えられるようにするため。
export const api = createClient<paths>({
  baseUrl: window.location.origin,
  fetch: (request) => globalThis.fetch(request),
  // 仕様書どおり、配列は functors=a,b の形で送る
  querySerializer: { array: { style: 'form', explode: false } },
})
