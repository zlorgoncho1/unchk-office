import { defineConfig, devices } from '@playwright/test';

/**
 * Configuration Playwright (tests E2E du frontend UNCHK Office).
 * Les tests s'exécutent contre le frontend (ng serve sur :4200) qui appelle
 * le gateway réel sur :8080. La pile Docker doit être démarrée (docker compose up -d).
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: 'http://localhost:4200',
    headless: true,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
  // Démarre automatiquement le serveur de dev Angular (réutilisé s'il tourne déjà).
  webServer: {
    command: 'npx ng serve --port 4200',
    url: 'http://localhost:4200',
    reuseExistingServer: true,
    timeout: 180_000,
    env: { NG_CLI_ANALYTICS: 'ci' },
  },
});
