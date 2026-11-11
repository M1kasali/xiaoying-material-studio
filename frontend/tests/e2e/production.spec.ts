import { test, expect, type Page } from "@playwright/test";

// A valid tiny PNG keeps tests independent of external assets and image libraries.
const image = { name: "测试素材.png", mimeType: "image/png", buffer: Buffer.from("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a7ioAAAAASUVORK5CYII=", "base64") };

async function setup(page: Page) {
  await page.goto("/templates/pet-portrait");
  await page.getByRole("button", { name: "使用小拾 · 账户 A" }).click();
  await expect(page.getByRole("button", { name: "开始制作", exact: true })).toBeDisabled();
}
async function upload(page: Page) {
  await page.getByLabel("选择第 1 张图片").setInputFiles(image);
  await expect(page.getByText("✓ 上传完成", { exact: true })).toBeVisible();
}
async function create(page: Page) {
  const response = page.waitForResponse(res => res.url().endsWith("/api/tasks") && res.request().method() === "POST");
  await page.getByRole("button", { name: /^(开始制作|再创建一个任务)$/ }).click();
  const accepted = await response;
  expect(accepted.status()).toBe(202);
  return (await accepted.json()).id as string;
}

test("upload validation, replacement, removal and submit gating", async ({ page }) => {
  await setup(page);
  await page.getByLabel("选择第 1 张图片").setInputFiles({ name: "bad.svg", mimeType: "image/svg+xml", buffer: Buffer.from("<svg/>") });
  await expect(page.getByText("请选择 JPG 或 PNG 图片。", { exact: true })).toBeVisible();
  await page.getByLabel("选择第 1 张图片").setInputFiles({ name: "fake.png", mimeType: "image/png", buffer: Buffer.from("not an image") });
  await expect(page.getByText(/上传失败：/)).toBeVisible();
  await upload(page);
  await expect(page.getByRole("button", { name: "开始制作", exact: true })).toBeEnabled();
  await page.getByRole("button", { name: "移除图片 1" }).click();
  await expect(page.getByRole("button", { name: "开始制作", exact: true })).toBeDisabled();
  await upload(page);
  const first = await page.locator(".upload-picker img").getAttribute("src");
  await upload(page);
  await expect(page.locator(".upload-picker img")).not.toHaveAttribute("src", first!);
});

test("accepted task survives navigation and reload, then renders a real result", async ({ page }) => {
  const errors: string[] = []; page.on("pageerror", error => errors.push(error.message));
  await setup(page); await upload(page);
  await page.getByText("演示设置 · 可调整速度与指定失败阶段").click();
  await page.getByLabel("每阶段耗时").selectOption("2500");
  const id = await create(page);
  await expect(page.locator(`[data-task-id="${id}"]`)).toHaveAttribute("data-status", /QUEUED|RUNNING/);
  await page.getByRole("link", { name: "我的作品", exact: true }).click();
  await page.reload();
  const card = page.locator(`[data-task-id="${id}"]`);
  await expect(card).toHaveAttribute("data-status", "SUCCEEDED", { timeout: 30000 });
  await expect(card.getByRole("img")).toBeVisible();
  expect(await card.getByRole("img").evaluate((element: HTMLImageElement) => element.naturalWidth)).toBeGreaterThan(0);
  const result = await page.request.get(`/api/tasks/${id}/result`);
  expect(result.status()).toBe(200); expect(await result.body()).toEqual(image.buffer);
  expect(errors).toEqual([]);
});

test("mixed concurrent tasks keep independent success and failure states", async ({ page }) => {
  await setup(page); await upload(page);
  await page.getByText("演示设置 · 可调整速度与指定失败阶段").click();
  await page.getByLabel("每阶段耗时").selectOption("2500");
  await page.getByLabel("模拟执行结果").selectOption("GENERATE");
  const failed = await create(page);
  await page.getByLabel("模拟执行结果").selectOption("");
  const succeeded = await create(page);
  expect(failed).not.toEqual(succeeded);
  const failure = page.locator(`[data-task-id="${failed}"]`);
  const success = page.locator(`[data-task-id="${succeeded}"]`);
  await expect(failure).toHaveAttribute("data-status", "FAILED", { timeout: 30000 });
  await expect(success).toHaveAttribute("data-status", "SUCCEEDED", { timeout: 30000 });
  await expect(failure.getByText("未执行", { exact: true })).toBeVisible();
  await expect(failure.getByRole("link", { name: "下载结果" })).toHaveCount(0);
  await expect(success.getByRole("link", { name: "下载结果" })).toBeVisible();
});

test("polling errors retain execution state and recover without failing the task", async ({ page }) => {
  await setup(page); await upload(page);
  await page.getByText("演示设置 · 可调整速度与指定失败阶段").click();
  await page.getByLabel("每阶段耗时").selectOption("2500");
  let blocked = true;
  await page.route(/\/api\/tasks\/[a-f0-9-]{36}$/, route => blocked ? route.abort() : route.continue());
  const id = await create(page);
  const card = page.locator(`[data-task-id="${id}"]`);
  await expect(card.getByText(/进度查询暂时失败/)).toBeVisible();
  await expect(card).not.toHaveAttribute("data-status", "FAILED");
  blocked = false;
  await card.getByRole("button", { name: "重新查询" }).click();
  await expect(card).toHaveAttribute("data-status", "SUCCEEDED", { timeout: 30000 });
  await expect(card.getByText(/进度查询暂时失败/)).toHaveCount(0);
});

test("account switching clears old tasks and enforces resource ownership", async ({ page }) => {
  await setup(page); await upload(page);
  const id = await create(page);
  await page.getByLabel("演示账户", { exact: true }).selectOption("bob");
  await expect(page.locator(`[data-task-id="${id}"]`)).toHaveCount(0);
  await expect(page.getByRole("button", { name: "开始制作", exact: true })).toBeDisabled();
  const hidden = await page.request.get(`/api/tasks/${id}`); expect(hidden.status()).toBe(404);
  await page.goto(`/tasks/${id}`);
  await expect(page.getByText("任务不存在，或不属于当前账户。", { exact: true })).toBeVisible();
  await page.getByLabel("演示账户", { exact: true }).selectOption("alice");
  await expect(page.locator(`[data-task-id="${id}"]`)).toHaveAttribute("data-status", "SUCCEEDED", { timeout: 30000 });
  await page.getByRole("button", { name: "退出", exact: true }).click();
  await expect(page.getByRole("button", { name: "使用小拾 · 账户 A" })).toBeVisible();
  expect((await page.request.get(`/api/tasks/${id}`)).status()).toBe(401);
});

test("navigation stops obsolete polling and the production form fits mobile", async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await setup(page); await upload(page);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.getByText("演示设置 · 可调整速度与指定失败阶段").click();
  await page.getByLabel("每阶段耗时").selectOption("8000");
  const id = await create(page);
  let queries = 0;
  page.on("request", request => { if (request.url().endsWith(`/api/tasks/${id}`)) queries++; });
  await page.getByRole("link", { name: "发现模板", exact: true }).click();
  await expect(page.getByText("6 个模板", { exact: true })).toBeAttached();
  const afterNavigation = queries;
  await page.waitForTimeout(3500);
  expect(queries).toBe(afterNavigation);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});

test("replacing an in-flight upload cannot restore the obsolete image", async ({ page }) => {
  await setup(page);
  let release: () => void = () => {};
  const held = new Promise<void>(resolve => { release = resolve; });
  let first = true;
  await page.route("**/api/assets", async route => {
    if (first) {
      first = false;
      await held;
      await route.abort().catch(() => {});
    } else await route.continue();
  });
  await page.getByLabel("选择第 1 张图片").setInputFiles({ ...image, name: "旧素材.png" });
  await expect(page.getByText("上传中…", { exact: true })).toBeVisible();
  const uploaded = page.waitForResponse(response => response.url().endsWith("/api/assets") && response.status() === 201);
  await page.getByLabel("选择第 1 张图片").setInputFiles({ ...image, name: "新素材.png" });
  const asset = await (await uploaded).json();
  release();
  await expect(page.getByText("✓ 上传完成", { exact: true })).toBeVisible();
  await expect(page.getByText("新素材.png", { exact: true })).toBeVisible();
  await expect(page.getByText("旧素材.png", { exact: true })).toHaveCount(0);
  const submitted = page.waitForRequest(request => request.url().endsWith("/api/tasks") && request.method() === "POST");
  await create(page);
  expect((await submitted).postDataJSON().assetIds).toEqual([asset.id]);
});

test("two-image template requires both uploads and blocks double submit", async ({ page }) => {
  await setup(page);
  await page.goto("/templates/coastal-postcard");
  await page.getByLabel("选择第 1 张图片").setInputFiles(image);
  await expect(page.getByRole("button", { name: "开始制作", exact: true })).toBeDisabled();
  await page.getByLabel("选择第 2 张图片").setInputFiles({ ...image, name: "第二张.png" });
  await expect(page.getByText("✓ 上传完成", { exact: true })).toHaveCount(2);
  let posts = 0;
  await page.route("**/api/tasks", async route => {
    if (route.request().method() === "POST") {
      posts++;
      await new Promise(resolve => setTimeout(resolve, 500));
    }
    await route.continue();
  });
  await page.getByRole("button", { name: "开始制作", exact: true }).click();
  await expect(page.getByRole("button", { name: "正在提交…", exact: true })).toBeDisabled();
  await expect(page.getByRole("button", { name: "再创建一个任务", exact: true })).toBeEnabled();
  expect(posts).toBe(1);
});
