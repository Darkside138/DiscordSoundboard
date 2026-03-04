import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react-swc'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      // Some components import pinned specifiers; map them to the real packages for tests
      'sonner@2.0.3': 'sonner',
      '@radix-ui/react-slider@1.2.3': '@radix-ui/react-slider',
    }
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./vitest.setup.ts'],
    globals: true,
    css: true,
    include: ['src/**/*.test.{ts,tsx}'],
    // Exclude accidentally nested duplicate paths that may appear due to tooling
    exclude: [
      '**/node_modules/**',
      '**/build/**',
      '**/coverage/**',
      '**/dist/**',
    ],
    coverage: {
      reporter: ['text', 'html'],
    },
  },
})
