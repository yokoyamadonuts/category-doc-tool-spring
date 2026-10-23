import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './tests',
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:18080',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
})
