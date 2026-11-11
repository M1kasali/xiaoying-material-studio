# API 契约

Java 基础路径 `/api`。以下接口已实现并接入前端。Next.js 使用固定白名单代理会话、素材和任务接口，保留 cookie、Origin、内容类型及 Set-Cookie；详情页服务端调用 Java。所有个人接口均 no-store，图片直接走鉴权接口，禁止公开图像优化缓存。

## 已实现

| 方法与路径 | 返回 | 备注 |
| --- | --- | --- |
| GET /actuator/health | `{"status":"UP"}` | 包含数据库连通性；不公开详情 |
| GET /api/categories | 字符串数组 | 从 MySQL 获取 |
| GET /api/templates?category=旅行灵感 | Template[] | 省略 category 返回全部；未知分类返回空数组 |
| GET /api/templates/{id} | Template | 不存在返回 404 ProblemDetail |

```json
{
  "id": "coastal-postcard",
  "title": "寄一张海边明信片",
  "category": "旅行灵感",
  "description": "选两张旅途中的照片，留住海风与沿途风景。",
  "coverUrl": "/demo/coastal.svg",
  "requiredImageCount": 2,
  "maxFileSizeBytes": 5242880,
  "acceptedMimeTypes": ["image/jpeg", "image/png"]
}
```

coverUrl 是前端同源公开封面地址；封面随前端源码和镜像一起交付。

## 身份、素材与任务接口

| 方法与路径 | 输入 | 输出/约束 |
| --- | --- | --- |
| POST /api/demo/session | `{ "account": "alice" }` | 设置会话 cookie，返回当前用户；仅 demo 模式 |
| GET /api/me | 会话 cookie | `{id,displayName}`；无有效身份 401 |
| DELETE /api/session | 会话 cookie | 撤销会话并清 cookie |
| POST /api/assets | multipart `file` | 201 `{id,name,mimeType,sizeBytes,previewUrl}` |
| GET /api/assets/{id}/content | 会话 cookie | 本人图片内容；非本人返回 404 |
| POST /api/tasks | `{templateId,assetIds,simulation?}` | 202 Task；已持久化且提交后台执行 |
| GET /api/tasks | 可选 cursor、limit（1～50，默认 20） | `{items,nextCursor}`，仅本人，按创建时间和 ID 倒序；cursor 使用返回值原样传回 |
| GET /api/tasks/{id} | 会话 cookie | Task，非本人或不存在返回 404 |
| GET /api/tasks/{id}/result | 会话 cookie | 本人成功任务结果图片 |

simulation 仅在 demo 模式接受：`{delayMs:2000, failAt:"GENERATE"}`；delayMs 0～30000，failAt 为空或 VALIDATE/GENERATE/FINALIZE。生产配置拒绝该字段。

Task 字段：`id, templateId, templateTitle, status, currentStage, createdAt, updatedAt, finishedAt, resultUrl, failureStage, failureReason, steps`。steps 每项为 `{stage,status,startedAt,finishedAt,errorMessage}`；任务排队时 currentStage 为空，运行状态为 QUEUED/RUNNING/SUCCEEDED/FAILED。所有时间为 ISO 8601 UTC。结果存储键、用户 ID 和模拟参数不会暴露在任务响应中。

错误约定：400 参数错误、401 未认证、403 浏览器写请求来源不受信任、404 不存在或非本人资源、413 文件过大、415 类型不支持、503 服务启动/停止期间暂不受理。采用 ProblemDetail `{type,title,status,detail,instance?}`；500 不返回内部路径、SQL 或堆栈。队列满时任务已经创建，因此返回 202 和带明确失败原因的 FAILED 记录，不留下永久排队状态。

Cookie：studio_session，HttpOnly、SameSite=Lax、Path=/，有效期 24 小时；HTTPS 配置可启用 Secure。退出返回 204。个人接口响应设置 Cache-Control: no-store 和 X-Content-Type-Options: nosniff。

上传完成不代表任务提交；任务受理成功不代表制作完成；查询异常不修改服务端任务状态。
