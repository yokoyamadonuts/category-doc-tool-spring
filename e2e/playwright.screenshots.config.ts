import { defineConfig, devices } from '@playwright/test'

/** README の画像を撮る。docker compose を立ち上げてから `npm run screenshots` で撮り直せる。 */
export default defineConfig({
  testDir: './screenshots',
  workers: 1,
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:18080',
    ...devices['Desktop Chrome'],
    viewport: { width: 1280, height: 760 },
    colorScheme: 'light',
  },
})
