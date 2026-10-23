import { join } from 'node:path'
import { test, expect } from '@playwright/test'
import { importZip, zipExample } from '../tests/support/projects'
import { strToU8, unzipSync, zipSync } from 'fflate'

const images = join(import.meta.dirname, '..', '..', 'docs', 'images')

/** 設計から内部仕様への対応を1つ抜いた dev-project。検証の誤りと追跡の表の切れ目を見せるため。 */
function devProjectWithGap(): Buffer {
  const files = unzipSync(zipExample('dev-project'))
  const yaml = new TextDecoder().decode(files['dev-project/category.yaml'])
  const broken = yaml.replace('      design-auth-service: internal-jwt-handler\n', '')
  expect(broken).not.toBe(yaml)
  files['dev-project/category.yaml'] = strToU8(broken)
  return Buffer.from(zipSync(files))
}

test('README の画像を撮る', async ({ page, request }) => {
  const response = await importZip(request, devProjectWithGap(), 'dev-project')
  const location = response.headers()['location']
  const base = location.replace('/api', '')

  try {
    await page.goto(`${base}/graph?category=infrastructure&object=infra-k8s-user`)
    await expect(page.getByText('PostgreSQLデータベース')).toBeVisible()
    await expect(page.getByRole('heading', { name: 'ユーザーサービスKubernetes設定' })).toBeVisible()
    // fitView のアニメーションが落ち着くのを待つ
    await page.waitForTimeout(500)
    await page.screenshot({ path: join(images, 'graph.png') })

    await page.goto(`${base}/verification`)
    await expect(page.getByText(/誤りが \d+ 件あります/)).toBeVisible()
    await page.screenshot({ path: join(images, 'verification.png') })

    await page.goto(`${base}/traceability?functors=specify-functor,design-functor,implement-functor,deploy-functor`)
    await expect(page.getByRole('table')).toContainText('対応なし')
    await page.screenshot({ path: join(images, 'traceability.png') })

    await page.goto(`${base}/paths?category=infrastructure&from=infra-k8s-auth&to=infra-database`)
    await expect(page.getByRole('region', { name: '全経路 2 本' })).toBeVisible()
    await page.screenshot({ path: join(images, 'paths.png') })
  } finally {
    await request.delete(location)
  }
})

