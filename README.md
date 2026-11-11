# 拾光 · 素材工作台

小影科技全栈笔试项目。实现模板浏览、素材上传、后台三阶段制作、个人历史和结果预览的完整流程。

已实现：Java 模板 API、演示会话、图片校验与私有存储、三阶段异步任务、指定阶段失败、历史分页和结果访问；Next.js 分类模板库、上传预览/替换/移除、账户切换、独立任务跟踪、查询异常恢复和刷新恢复。包含 Docker Compose、15 个 Java 单元测试、5 个前端状态测试、8 个真实浏览器场景以及 API 集成验证。验证状态见 [验证记录](docs/verification.md)，需求映射见 [验收清单](docs/acceptance.md)。

**快速体验：** `docker compose up --build --wait` → 打开 http://localhost:3000 → 选择模板和演示账户 → 上传图片 → 开始制作。完整 [五分钟演示](docs/demo.md) 包含实际截图及成功、失败、多任务、断网、刷新与用户隔离步骤。

## 1. 技术与目录

- Node.js 24；pnpm 11.25.0；Next.js 16.4.6；React 19.3.1；TypeScript 5.9.3。
- Java 21；Maven 3.9.11；Spring Boot 3.5.16；MyBatis Starter 3.0.5；MySQL 8.4。
- 前端精确版本及传递依赖见 `frontend/package.json` 与 `frontend/pnpm-lock.yaml`，Java 依赖由 `backend/pom.xml` 和 Spring Boot BOM 管理。

```text
frontend/       Next.js 页面、同源接口、本地示例封面
backend/        Java API、MyBatis、Flyway 数据库迁移
docs/           验收清单、架构、接口与验证记录
scripts/        原创素材生成、API 验证和截图脚本
.github/        CI 流水线
compose.yaml    三服务容器交付
```

## 2. 启动与配置

需要可用的 Docker Engine 和 Compose。直接启动：

```sh
docker compose up --build --wait
```

前端 `http://localhost:3000`，Java `http://localhost:8080`，数据库宿主机端口 3307。均只绑定本机回环地址。首次拉取镜像与安装依赖需要网络；运行时示例素材不依赖外部服务。

可复制 `.env.example` 为 `.env` 修改端口和本地数据库密码；示例密码仅为本地演示值，没有真实凭据。数据库修改已有数据卷的凭据时应另行管理数据库用户，修改环境变量不会自动更新旧密码。

MySQL 健康后 Java 启动，Flyway 自动执行 `backend/src/main/resources/db/migration/` 的版本化建表与种子。Java 健康后启动前端。数据库和上传目录使用命名数据卷。初始化包含 6 个模板、3 个分类、2 个演示用户；六张封面 SVG 与两张可上传的示例 PNG 均随源码提供，无需联网获取素材。

本机开发（需要 Java 21、Maven、Node.js 24、pnpm）：

```sh
docker compose up -d mysql
mvn -f backend/pom.xml spring-boot:run
# 另一个终端
cd frontend
pnpm install --frozen-lockfile
pnpm dev
```

Java 默认连接 localhost:3307；可用 DB_URL、DB_USERNAME、DB_PASSWORD 覆盖。前端 BACKEND_URL 默认 http://localhost:8080，Compose 中为 http://backend:8080。前端运行时读取后端地址，无需把内部地址交给浏览器。STORAGE_ROOT 为文件存储路径。前后端构建基础镜像锁定到本次验证的 digest，以保证重现并复用已下载镜像。

其他配置：DEMO_ENABLED 控制演示登录与模拟参数；SECURE_COOKIE 在 HTTPS 部署时设为 true；ALLOWED_ORIGINS 限制浏览器写请求来源（修改端口时同步修改）；TASK_WORKERS / TASK_QUEUE_CAPACITY 控制执行线程和队列；SIMULATION_DELAY_MS 为每个阶段的默认耗时。默认均适用于本机演示。

## 3. 构建、检查与接口

```sh
cd frontend
pnpm lint
pnpm typecheck
pnpm test
pnpm build
# 真实浏览器测试：先启动完整 Compose
pnpm exec playwright install chromium
pnpm test:e2e
# 回到仓库根目录
mvn -B -ntp -f backend/pom.xml verify
docker compose config --quiet
# 已启动 Compose 后，执行真实 API 测试（Python 标准库，无需 pip）
python scripts/backend_smoke.py --restart
```

CI 在 push/PR 执行前端 lint、typecheck、状态测试及构建，Java 单元测试及构建，再启动 Compose 验证真实 API 和浏览器流程。Java 测试覆盖阶段顺序、失败中断、事务提交边界、队列满与上传校验；前端测试覆盖不同任务隔离、终态不回退、查询失败不等于任务失败、上传替换、重复提交阻止、刷新恢复及生命周期清理。浏览器测试失败时保存报告、截图和 trace。

[API 契约](docs/api.md) 说明请求/响应字段；[架构设计](docs/architecture.md) 记录数据表、状态、执行方案及边界。主要接口为 POST /api/assets、POST /api/tasks、GET /api/tasks、GET /api/tasks/{id}、GET /api/tasks/{id}/result；浏览器通过 Next.js 同源接口访问 Java。

## 4. 演示

选择模板和演示账户，按要求上传图片。详情左侧「演示设置」可选择每阶段 0.8 / 2.5 / 8 秒，以及任意指定阶段失败；默认全部成功。任务受理后可以继续创建其他任务；右侧制作记录分别跟踪状态。进入「我的作品」或刷新详情，记录从 Java 恢复，未完成任务继续更新。临时阻断查询只显示请求错误，恢复网络后可以重新查询。

后台流程是 VALIDATE（素材检查）→ GENERATE（模拟生成）→ FINALIZE（结果整理），整体状态为 QUEUED / RUNNING / SUCCEEDED / FAILED。创建事务提交后才派发到有界 Java 线程池，工作线程按顺序执行并记录步骤，查询不驱动执行。模拟生成选择第一张输入图片，结果整理复制成独立文件；不调用 AI，不伪称实际完成图片变换。

后端也可通过 [API 演示步骤](docs/backend-demo.md) 或 `python scripts/backend_smoke.py --restart` 验证。脚本会创建少量记录，指定 --restart 时会重启本项目后端。未上传的浏览器草稿不会在刷新后恢复，已受理任务会恢复。

## 5. 用户身份

两套固定演示账户 alice（小拾）、bob（小光），通过页头账户选择器或制作页登录入口切换。会话令牌随机生成，数据库只存摘要和过期时间，浏览器用 HttpOnly / SameSite=Lax cookie。更换账户会撤销当前旧会话；前端清除旧账户上传与任务状态并中止请求，跨标签页通过 BroadcastChannel 通知重新读取身份。素材、任务、历史及结果均在 Java 校验归属。可先在 A 创建任务，再切换 B 观察隔离，切回 A 恢复历史。该方案用于本机演示，不是生产认证；DEMO_ENABLED=false 会关闭演示登录和客户端模拟参数，此项目没有另行实现生产登录入口。

## 6. 完成度与实际投入

正文 R-01～R-14 的核心功能已实现，具体证据见 [验收清单](docs/acceptance.md) 和 [验证记录](docs/verification.md)。以下边界未实现：真实 AI、正式身份认证、集群协调、失败自动重试、提交幂等、草稿恢复、素材自动清理和重启断点续跑。服务重启会把未完成任务标为失败；数据库或磁盘持续不可用时不能保证即时写入失败状态。提交连接中断可能无法确认是否已受理，页面提示先检查历史，避免盲目重试。

实际投入：开发与验证使用 Codex 辅助分阶段执行；本人参与需求确认、Docker 重启和仓库方案选择。未连续计量人工或 AI 操作分钟数，因此不填写虚构工时。操作、问题和修正见 [开发记录](docs/worklog.md)。在剩余交付时间内优先实现闭环与测试，未添加支付、真实生成或复杂账号系统。

## 7. 部署与回滚

本地验收使用 Compose，无需公网部署。更新前保存当前验收 commit，并备份数据库和素材卷；执行 `docker compose up --build -d` 后检查健康与关键流程。代码回滚可切换到已验证 commit 后重建镜像；数据库迁移不会自动降级，若新版本存在不兼容迁移，需按备份恢复后再运行旧版本。

`docker compose down` 停止并删除容器，保留数据卷。不要在需要保留历史时使用 `down -v`。备份命令、停止顺序及数据库回滚边界见 [部署说明](docs/deployment.md)；灾难恢复演练未执行。

## 8. AI 使用说明

使用 Codex 辅助开发：提供题目 PDF、参考截图和现有仓库上下文，将 R-01～R-14 拆为验收清单，再确定 API、数据表和状态边界，按工程骨架、后端闭环、前端闭环及交付四个阶段生成/修改代码。每阶段执行构建、测试和真实服务验证，依据失败输出修正后再提交。

具体例子：将任务状态与查询错误分开，按 taskId 独立跟踪；浏览器测试临时阻断 GET 查询，确认界面保留原状态、没有误报执行失败，恢复连接后能显示真实完成结果。又通过延迟旧上传并替换图片，验证旧请求不会覆盖新素材。参考截图复核后，将制作页改为左侧模板/上传、右侧历史。提交者应阅读代码并能够说明这些取舍；没有把 AI 执行记录描述为纯人工独立完成。

## 9. 来源与提交

框架通过官方包仓库安装；工程代码自行组织，未复用成品模板站。参考题目截图的主要布局，未复制题目品牌或图片。六张 SVG 封面由 `scripts/generate-demo-assets.py` 绘制，两张可上传 PNG 由 `scripts/generate-demo-inputs.py` 用 Python 标准库生成，可离线重新生成，无第三方素材授权依赖。测试使用 JUnit/Mockito 和 Playwright；没有复用外部业务项目源码。

源码仓库：[M1kasali/xiaoying-material-studio](https://github.com/M1kasali/xiaoying-material-studio)（私有）。默认分支 `main`；[CI 运行记录](https://github.com/M1kasali/xiaoying-material-studio/actions)。提交回复中注明验收 commit；源码压缩包另附 SOURCE_COMMIT.txt。私有仓库需要评估者读取权限，也可以直接提交不含依赖和缓存的源码压缩包。没有要求评估者在宿主机执行项目脚本：主要运行入口为 Compose，前端检查与 Java 测试也会在镜像构建内执行。

本地查看当前提交：`git rev-parse HEAD`。提交源码时排除 `.env`、依赖、构建缓存、用户上传文件和题目原件。
