# 后端业务演示

先运行 `docker compose up --build --wait`。当前前端尚未接入账户、上传和制作任务，以下操作直接访问 Java 的 8080 端口。

## 一次验证全部关键场景

```sh
python scripts/backend_smoke.py --restart
```

仅使用 Python 标准库，不需要额外安装依赖。脚本创建两种 4×4 PNG 并上传，验证账户隔离、格式/大小/内容校验、受理后独立执行、三个阶段分别失败、多个任务互不干扰、结果鉴权、分页、会话切换与退出。--restart 还会重启本项目后端，确认旧任务明确中止且历史结果与会话保留。脚本会留下少量演示记录，不会删除已有数据。

## 手工请求（Bash / Git Bash）

1. 选择账户并保存 cookie：

```sh
curl -c /tmp/studio-cookies.txt -H 'Content-Type: application/json' \
  -d '{"account":"alice"}' http://localhost:8080/api/demo/session
curl -b /tmp/studio-cookies.txt http://localhost:8080/api/me
```

alice 对应小拾，bob 对应小光。使用另一份 cookie 文件登录 bob，可演示个人数据隔离；同一份 cookie 文件重新登录会撤销旧会话。

2. 上传真实图片，记录响应中的 id：

```sh
curl -b /tmp/studio-cookies.txt -F 'file=@/path/to/photo.png;type=image/png' \
  http://localhost:8080/api/assets
```

把路径换为本机图片。响应的 previewUrl 需要同一用户 cookie 才能读取。JPEG/PNG 每张不超过 5 MB，不接受改扩展名伪装的文件。

3. 使用一个素材提交单图模板：

```sh
curl -b /tmp/studio-cookies.txt -H 'Content-Type: application/json' \
  -d '{"templateId":"pet-portrait","assetIds":["替换为素材id"],"simulation":{"delayMs":2000}}' \
  http://localhost:8080/api/tasks
```

返回 202 和任务 ID，完整制作通常需要三个阶段各 2 秒。停止查询或关闭页面不影响后台执行。

4. 查询任务、历史和结果：

```sh
curl -b /tmp/studio-cookies.txt http://localhost:8080/api/tasks/替换为任务id
curl -b /tmp/studio-cookies.txt 'http://localhost:8080/api/tasks?limit=20'
curl -b /tmp/studio-cookies.txt -o /tmp/result.png \
  http://localhost:8080/api/tasks/替换为任务id/result
```

结果在 SUCCEEDED 后可读取。模拟器复制第一张输入图作为独立结果文件，不执行真实 AI 转换。任务响应 steps 包含每个已执行阶段的时间与状态，失败后的步骤不会出现。

## 失败、耗时与多任务

在提交请求中使用 `"simulation":{"delayMs":1000,"failAt":"GENERATE"}`。failAt 可分别为 VALIDATE、GENERATE、FINALIZE，每次都在指定阶段稳定失败；省略 failAt 则成功。delayMs 为每阶段延迟，范围 0～30000。

连续提交多个不同配置的任务，分别查询 ID 或历史列表；默认 2 个工作线程，队列容量 32。队列满会记录明确失败原因。服务端配置默认值可通过 SIMULATION_DELAY_MS / TASK_WORKERS / TASK_QUEUE_CAPACITY 修改。

数据库查询不推进任务。服务重启会将未完成任务记录为 FAILED，原因包含“中止”；不会重跑、续跑或修改已完成记录。

## 退出

```sh
curl -X DELETE -b /tmp/studio-cookies.txt -c /tmp/studio-cookies.txt \
  http://localhost:8080/api/session
```

cookie 文件是临时登录凭据，只保存在本机，不提交到仓库。
