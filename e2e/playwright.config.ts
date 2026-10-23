import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './tests',
  // DB を共有するので、1つずつ順に流す
  workers: 1,
  fullyParallel: false,
  use: {
    baseURL: process.env.BASE_URL ?? 'http://localhost:18080',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
})
