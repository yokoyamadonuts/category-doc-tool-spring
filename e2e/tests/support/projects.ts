import { readdirSync, readFileSync, statSync } from 'node:fs'
import { join, relative } from 'node:path'
import type { APIRequestContext } from '@playwright/test'
import { expect } from '@playwright/test'
import { zipSync } from 'fflate'

const examples = join(import.meta.dirname, '..', '..', '..', 'examples')

/** examples/ の下のフォルダを、フォルダごと zip にする。 */
export function zipExample(name: string): Buffer {
  const root = join(examples, name)
  const files: Record<string, Uint8Array> = {}
  const walk = (dir: string) => {
    for (const entry of readdirSync(dir)) {
      const path = join(dir, entry)
      if (statSync(path).isDirectory()) walk(path)
      else files[`${name}/${relative(root, path)}`] = readFileSync(path)
    }
  }
  walk(root)
  return Buffer.from(zipSync(files))
}

export function zipOf(files: Record<string, string>): Buffer {
  const entries = Object.fromEntries(Object.entries(files).map(([k, v]) => [k, new TextEncoder().encode(v)]))
  return Buffer.from(zipSync(entries))
}

export async function importZip(request: APIRequestContext, zip: Buffer, name: string) {
  return request.post('/api/projects', {
    multipart: { name, file: { name: `${name}.zip`, mimeType: 'application/zip', buffer: zip } },
  })
}

/** 前の実行で残ったプロジェクトを消す。 */
export async function deleteAllProjects(request: APIRequestContext) {
  const response = await request.get('/api/projects')
  expect(response.ok()).toBeTruthy()
  for (const project of (await response.json()) as Array<{ id: string }>) {
    expect((await request.delete(`/api/projects/${project.id}`)).status()).toBe(204)
  }
}
