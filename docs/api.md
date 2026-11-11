# API 契约

Java 基础路径 `/api`。以下明确区分已实现接口与设计接口。Next.js 当前只代理模板列表与分类；详情页服务端调用 Java。

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

## 下一阶段实现

| 方法与路径 | 输入 | 输出/约束 |
| --- | --- | --- |
| POST /api/demo/session | `{ "account": "alice" }` | 设置会话 cookie，返回当前用户；仅 demo 模式 |
| GET /api/me | 会话 cookie | `{id,displayName}`；无有效身份 401 |
| DELETE /api/session | 会话 cookie | 撤销会话并清 cookie |
| POST /api/assets | multipart `file` | 201 `{id,name,mimeType,sizeBytes,previewUrl}` |
| GET /api/assets/{id}/content | 会话 cookie | 本人图片内容；非本人返回 404 |
| POST /api/tasks | `{templateId,assetIds,simulation?}` | 202 Task；已持久化且提交后台执行 |
| GET /api/tasks | 可选 cursor、limit | `{items,nextCursor}`，仅本人，按创建时间倒序 |
| GET /api/tasks/{id} | 会话 cookie | Task，非本人或不存在返回 404 |
| GET /api/tasks/{id}/result | 会话 cookie | 本人成功任务结果图片 |

simulation 仅在 demo 模式接受：`{delayMs:2000, failAt:"GENERATE"}`；delayMs 0～30000，failAt 为空或 VALIDATE/GENERATE/FINALIZE。生产配置拒绝该字段。

Task 设计：`id, templateId, templateTitle, status, currentStage, createdAt, updatedAt, finishedAt, resultUrl, failureStage, failureReason`。可增加只读 steps 供观察边界。所有时间为 ISO 8601 UTC。

错误约定：400 参数错误、401 未认证、404 不存在或非本人资源、413 文件过大、415 类型不支持、503 服务暂不可用。采用 ProblemDetail `{type,title,status,detail,instance}`；500 不返回内部路径、SQL 或堆栈。

上传完成不代表任务提交；任务受理成功不代表制作完成；查询异常不修改服务端任务状态。
