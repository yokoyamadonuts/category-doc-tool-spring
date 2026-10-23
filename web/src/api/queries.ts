import { useQuery } from '@tanstack/react-query'
import { api } from './client'
import type { components } from './schema'

export type Schemas = components['schemas']
export type ProjectDetail = Schemas['ProjectDetail']
export type CategoryGraph = Schemas['CategoryGraph']
export type FunctorSummary = Schemas['FunctorSummary']
export type MorphismView = Schemas['MorphismView']

/** API が失敗したときの状態コードを持つ。画面で 404 を見分けるため。 */
export class ApiError extends Error {
  readonly status: number
  readonly detail?: string

  constructor(status: number, detail?: string) {
    super(detail ?? `API が ${status} を返しました`)
    this.status = status
    this.detail = detail
  }
}

async function unwrap<T>(call: Promise<{ data?: T; error?: unknown; response: Response }>): Promise<T> {
  const { data, error, response } = await call
  if (error !== undefined || data === undefined) {
    const detail = typeof error === 'object' && error !== null && 'detail' in error ? String(error.detail) : undefined
    throw new ApiError(response.status, detail)
  }
  return data
}

export function useProjects() {
  return useQuery({
    queryKey: ['projects'],
    queryFn: () => unwrap(api.GET('/api/projects')),
  })
}

export function useProject(projectId: string) {
  return useQuery({
    queryKey: ['projects', projectId],
    queryFn: () => unwrap(api.GET('/api/projects/{projectId}', { params: { path: { projectId } } })),
  })
}

export function useCategoryGraph(projectId: string, categoryId: string | undefined) {
  return useQuery({
    queryKey: ['projects', projectId, 'categories', categoryId],
    queryFn: () =>
      unwrap(
        api.GET('/api/projects/{projectId}/categories/{categoryId}/graph', {
          params: { path: { projectId, categoryId: categoryId! } },
        }),
      ),
    enabled: categoryId !== undefined,
  })
}

export function useObject(projectId: string, objectId: string) {
  return useQuery({
    queryKey: ['projects', projectId, 'objects', objectId],
    queryFn: () =>
      unwrap(api.GET('/api/projects/{projectId}/objects/{objectId}', { params: { path: { projectId, objectId } } })),
  })
}

export function useVerification(projectId: string) {
  return useQuery({
    queryKey: ['projects', projectId, 'verification'],
    queryFn: () => unwrap(api.GET('/api/projects/{projectId}/verification', { params: { path: { projectId } } })),
  })
}

export function usePaths(projectId: string, from: string | undefined, to: string | undefined) {
  return useQuery({
    queryKey: ['projects', projectId, 'paths', from, to],
    queryFn: () =>
      unwrap(
        api.GET('/api/projects/{projectId}/paths', {
          params: { path: { projectId }, query: { from: from!, to: to! } },
        }),
      ),
    enabled: from !== undefined && to !== undefined,
  })
}

export function useTraceability(projectId: string, functors: string[]) {
  return useQuery({
    queryKey: ['projects', projectId, 'traceability', functors],
    queryFn: () =>
      unwrap(
        api.GET('/api/projects/{projectId}/traceability', {
          params: { path: { projectId }, query: { functors } },
        }),
      ),
    enabled: functors.length > 0,
  })
}
