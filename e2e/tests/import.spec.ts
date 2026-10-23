import { test, expect } from '@playwright/test'
import { importZip, zipExample, zipOf } from './support/projects'

test.describe('dev-project を取り込む', () => {
  let location: string

  test.beforeAll(async ({ request }) => {
    const response = await importZip(request, zipExample('dev-project'), 'dev-project')
    expect(response.status()).toBe(201)
    location = response.headers()['location']
    expect(location).toMatch(/\/api\/projects\/[0-9a-f-]{36}$/)
  })

  test.afterAll(async ({ request }) => {
    if (location) await request.delete(location)
  })

  test('一覧の画面に出る', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByText('dev-project')).toBeVisible()
  })

  test('カテゴリと関手の一覧を返す', async ({ request }) => {
    const project = await (await request.get(location)).json()

    expect(project.name).toBe('dev-project')
    expect(project.categories.map((c: { id: string }) => c.id)).toEqual([
      'requirements', 'specifications', 'design', 'internal-spec', 'infrastructure',
    ])
    expect(project.functors).toHaveLength(4)
  })

  test('カテゴリのグラフを返す', async ({ request }) => {
    const graph = await (await request.get(`${location}/categories/infrastructure/graph`)).json()

    expect(graph.objects.map((o: { id: string }) => o.id)).toEqual([
      'infra-k8s-user', 'infra-k8s-auth', 'infra-database',
    ])
    expect(graph.morphisms).toContainEqual(expect.objectContaining({
      id: 'infra-auth-uses-db', source: 'infra-k8s-auth', target: 'infra-database', identity: false,
    }))
  })

  test('Markdown の本文を文書に結びつける', async ({ request }) => {
    const object = await (await request.get(`${location}/objects/req-auth-system`)).json()

    expect(object).toMatchObject({ id: 'req-auth-system', title: '認証システム要件', categoryId: 'requirements' })
    expect(object.content).toContain('# 認証システム要件')
    expect(object.content).not.toContain('stakeholder:')
  })

  test('すべての検証が通る', async ({ request }) => {
    const verification = await (await request.get(`${location}/verification`)).json()

    expect(verification.valid).toBe(true)
    expect(verification.categories).toHaveLength(5)
    expect(verification.functors).toHaveLength(4)
  })

  test('インフラの中で経路を探す', async ({ request }) => {
    const paths = await (await request.get(`${location}/paths`, {
      params: { from: 'infra-k8s-auth', to: 'infra-database' },
    })).json()

    expect(paths.shortest.map((m: { id: string }) => m.id)).toEqual(['infra-auth-uses-db'])
    expect(paths.all.map((p: Array<{ id: string }>) => p.map((m) => m.id))).toEqual([
      ['infra-auth-uses-db'],
      ['infra-auth-uses-user', 'infra-user-uses-db'],
    ])
  })

  test('要件からインフラまで追跡の表を作る', async ({ request }) => {
    const table = await (await request.get(`${location}/traceability`, {
      params: { functors: 'specify-functor,design-functor,implement-functor,deploy-functor' },
    })).json()

    expect(table.rows).toEqual([
      {
        objectId: 'req-user-management',
        title: 'ユーザー管理要件',
        mappedObjectIds: ['spec-user-api', 'design-user-service', 'internal-user-repository', 'infra-k8s-user'],
        complete: true,
      },
      {
        objectId: 'req-auth-system',
        title: '認証システム要件',
        mappedObjectIds: ['spec-auth-api', 'design-auth-service', 'internal-jwt-handler', 'infra-k8s-auth'],
        complete: true,
      },
    ])
  })
})

test('参照の切れた category.yaml は 422 で誤りを並べる', async ({ request }) => {
  const zip = zipOf({
    'category.yaml': `
categories:
  - id: c
    name: C
    objects: []
    morphisms: []
functors:
  - id: F
    name: F
    sourceCategory: c
    targetCategory: missing
`,
  })

  const response = await importZip(request, zip, 'broken')

  expect(response.status()).toBe(422)
  expect(response.headers()['content-type']).toContain('application/problem+json')
  expect((await response.json()).errors).toEqual([
    "Functor 'F' refers to unknown target category 'missing'",
  ])
})

test('存在しないプロジェクトは 404', async ({ request }) => {
  const response = await request.get('/api/projects/00000000-0000-0000-0000-000000000000')

  expect(response.status()).toBe(404)
})
