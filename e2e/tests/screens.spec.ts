import { test, expect } from '@playwright/test'
import { importZip, zipExample, zipOf } from './support/projects'

test.describe('dev-project の画面', () => {
  let location: string

  test.beforeAll(async ({ request }) => {
    const response = await importZip(request, zipExample('dev-project'), 'dev-project')
    expect(response.status()).toBe(201)
    location = response.headers()['location']
  })

  test.afterAll(async ({ request }) => {
    if (location) await request.delete(location)
  })

  test('一覧から開くとグラフが出て、文書を押すと本文が出る', async ({ page }) => {
    await page.goto('/')
    await page.getByRole('link', { name: 'dev-project' }).click()

    await expect(page.getByRole('heading', { level: 2, name: 'dev-project' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'グラフ' })).toHaveAttribute('aria-current', 'page')

    await page.getByLabel('カテゴリ').selectOption({ label: 'Infrastructure' })
    await expect(page.getByText('PostgreSQLデータベース')).toBeVisible()
    await expect(page.getByText('恒等射 3 本は描いていません')).toBeVisible()

    await page.getByLabel('カテゴリ').selectOption({ label: 'Requirements' })
    await page.getByText('認証システム要件', { exact: true }).click()

    const detail = page.getByRole('complementary', { name: '文書の詳細' })
    await expect(detail.getByRole('heading', { name: '認証システム要件' })).toBeVisible()
    await expect(detail.getByText('セキュリティチーム')).toBeVisible()
    await expect(detail.getByText('ユーザーはメールアドレスとパスワードでログインできること')).toBeVisible()
    await expect(page).toHaveURL(/object=req-auth-system/)
  })

  test('検証の結果を出す', async ({ page }) => {
    await page.goto(`${location.replace('/api', '')}/verification`)

    await expect(page.getByText('すべての検証を通りました')).toBeVisible()
    await expect(page.getByRole('region', { name: 'カテゴリ' }).getByRole('listitem')).toHaveCount(5)
    await expect(page.getByRole('region', { name: '関手' }).getByRole('listitem')).toHaveCount(4)
  })

  test('経路を探す', async ({ page }) => {
    await page.goto(`${location.replace('/api', '')}/paths`)

    await page.getByLabel('カテゴリ').selectOption({ label: 'Infrastructure' })
    await page.getByLabel('始点').selectOption({ label: '認証サービスKubernetes設定' })
    await page.getByLabel('終点').selectOption({ label: 'PostgreSQLデータベース' })
    await page.getByRole('button', { name: '経路を探す' }).click()

    const shortest = page.getByRole('region', { name: '最短経路' })
    await expect(shortest).toContainText('認証サービスKubernetes設定')
    await expect(shortest).toContainText('connects')
    await expect(page.getByRole('region', { name: '全経路 2 本' }).getByRole('listitem')).toHaveCount(2)
  })

  test('関手をつないで追跡の表を作る', async ({ page }) => {
    await page.goto(`${location.replace('/api', '')}/traceability`)

    for (const functor of [
      'SpecifyFunctor (要件→仕様)',
      'DesignFunctor (仕様→設計)',
      'ImplementFunctor (設計→内部仕様)',
      'DeployFunctor (内部仕様→インフラ)',
    ]) {
      await page.getByLabel('次の関手').selectOption({ label: functor })
      await page.getByRole('button', { name: '加える' }).click()
    }

    const table = page.getByRole('table')
    await expect(table.getByRole('columnheader')).toHaveText([
      'Requirements', 'Specifications', 'Design', 'InternalSpec', 'Infrastructure',
    ])
    await expect(table.getByRole('row', { name: /認証システム要件/ })).toContainText('infra-k8s-auth')
    await expect(page.getByLabel('次の関手')).toBeDisabled()
  })
})

test('恒等射の欠けたプロジェクトは、検証で誤りを出す', async ({ page, request }) => {
  const zip = zipOf({
    'category.yaml': `
categories:
  - id: c
    name: C
    objects:
      - id: a
        title: A
        domain: d
    morphisms: []
`,
  })
  const response = await importZip(request, zip, 'missing-identity')
  const location = response.headers()['location']

  try {
    await page.goto(`${location.replace('/api', '')}/verification`)

    await expect(page.getByText('誤りが 1 件あります')).toBeVisible()
    await expect(page.getByText("Object 'a' lacks an identity morphism (id: a → a)")).toBeVisible()
  } finally {
    await request.delete(location)
  }
})
