import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

// Vitest の globals を使っていないので、Testing Library の後始末を自分で登録する
afterEach(() => {
  cleanup()
})
