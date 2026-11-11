import { defineConfig } from "@playwright/test";

export default defineConfig({
  testDir: "./tests/e2e", timeout: 60000, expect: { timeout: 15000 },
  fullyParallel: false, workers: 1, retries: 0,
  reporter: [["list"], ["html", { open: "never" }]],
  use: { baseURL: process.env.E2E_BASE_URL ?? "http://localhost:3000", channel: process.env.PLAYWRIGHT_CHANNEL || undefined, trace: "retain-on-failure", screenshot: "only-on-failure", viewport: { width: 1440, height: 1000 } },
});
