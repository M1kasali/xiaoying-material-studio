// Run after Compose is healthy and frontend dev dependencies/browser are installed.
// Creates real demo records. No API response is mocked.
import { createRequire } from "node:module";
import { mkdir } from "node:fs/promises";
import { fileURLToPath } from "node:url";
const require = createRequire(new URL("../frontend/package.json", import.meta.url));
const { chromium, expect } = require("@playwright/test");
const root = new URL("../", import.meta.url);
const destination = new URL("docs/screenshots/", root);
await mkdir(destination, { recursive: true });
const browser = await chromium.launch({ channel: process.env.PLAYWRIGHT_CHANNEL || undefined });
try {
  const page = await browser.newPage({ viewport: { width: 1440, height: 1200 } });
  const base = process.env.E2E_BASE_URL ?? "http://localhost:3000";
  await page.goto(base);
  await expect(page.locator(".template-card")).toHaveCount(6);
  await page.evaluate(async () => { await Promise.all([...document.images].map(image => image.decode())); });
  await page.screenshot({ path: fileURLToPath(new URL("catalog.png", destination)) });
  await page.goto(`${base}/templates/coastal-postcard`);
  await page.getByRole("button", { name: "使用小光 · 账户 B" }).click();
  for (let index = 1; index <= 2; index++) {
    await page.getByLabel(`选择第 ${index} 张图片`).setInputFiles(fileURLToPath(new URL(`frontend/public/demo/input-0${index}.png`, root)));
  }
  await expect(page.getByText("✓ 上传完成", { exact: true })).toHaveCount(2);
  const submitted = page.waitForResponse(response => response.url().endsWith("/api/tasks") && response.request().method() === "POST");
  await page.getByRole("button", { name: "开始制作", exact: true }).click();
  const task = await (await submitted).json();
  await expect(page.locator(`[data-task-id="${task.id}"]`)).toHaveAttribute("data-status", "SUCCEEDED", { timeout: 30000 });
  await page.locator(`[data-task-id="${task.id}"] img`).evaluate(image => image.decode());
  await page.screenshot({ path: fileURLToPath(new URL("production.png", destination)) });
  await page.goto(`${base}/tasks/${task.id}`);
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.locator(".result-image")).toBeVisible();
  await page.locator(".result-image").evaluate(image => image.decode());
  if (!(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth))) throw new Error("Mobile overflow");
  await page.screenshot({ path: fileURLToPath(new URL("mobile-result.png", destination)), fullPage: true });
  console.log(JSON.stringify({ screenshots: 3, task: task.id, source: "live Java/MySQL" }));
} finally { await browser.close(); }
