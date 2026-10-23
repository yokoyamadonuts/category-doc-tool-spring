import { test, expect } from '@playwright/test'
import { deleteAllProjects } from './support/projects'

test('プロジェクトがないとき、一覧に空の案内が出る', async ({ page, request }) => {
  await deleteAllProjects(request)

  await page.goto('/')

  await expect(page.getByRole('heading', { name: 'CatDoc' })).toBeVisible()
  await expect(page.getByText('プロジェクトはまだありません')).toBeVisible()
})
