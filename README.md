# 拾光 · 素材工作台

小影科技全栈笔试项目。**当前推进到后端业务闭环阶段，前端上传与任务管理仍待实现，尚不是完整交付版本。**

已实现：Java 模板 API、演示会话、图片上传与私有预览、三阶段异步任务、失败模拟、个人历史和结果 API；Next.js 模板库和只读详情；Docker Compose、后端单元测试及真实 API 验证脚本。待完成：前端账户切换、上传交互、任务提交与跟踪、历史页面及前端核心测试。验证状态见 [验证记录](docs/verification.md)，需求映射见 [验收清单](docs/acceptance.md)。

## 1. 技术与目录

- Node.js 24；pnpm 11.25.0；Next.js 16.4.6；React 19.3.1；TypeScript 5.9.3。
- Java 21；Maven 3.9.11；Spring Boot 3.5.16；MyBatis Starter 3.0.5；MySQL 8.4。
- 前端精确版本及传递依赖见 `frontend/package.json` 与 `frontend/pnpm-lock.yaml`，Java 依赖由 `backend/pom.xml` 和 Spring Boot BOM 管理。

```text
frontend/       Next.js 页面、同源接口、本地示例封面
backend/        Java API、MyBatis、Flyway 数据库迁移
docs/           验收清单、架构、接口与验证记录
scripts/        原创 SVG 素材生成脚本
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

MySQL 健康后 Java 启动，Flyway 自动执行 `backend/src/main/resources/db/migration/` 的版本化建表与种子。Java 健康后启动前端。数据库和后续上传目录使用命名数据卷。

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
pnpm build
# 回到仓库根目录
mvn -B -ntp -f backend/pom.xml verify
docker compose config --quiet
# 已启动 Compose 后，执行真实 API 测试（Python 标准库，无需 pip）
python scripts/backend_smoke.py --restart
```

CI 在 push/PR 执行上述前后端检查，并启动 Compose 验证模板、身份、上传、任务及重启行为。Java 单元测试重点覆盖阶段顺序、每个阶段失败中断、线程中断、重复派发、提交事务边界、队列满与上传校验。前端多任务和异常区分的测试尚待实现，因此 R-12 仍未全部完成。

[API 契约](docs/api.md) 标明已实现与待实现接口；[架构设计](docs/architecture.md) 记录数据表、状态、执行方案及边界。

## 4. 演示

页面目前可演示模板列表、分类、详情、未知模板、加载异常与重试。后端业务可通过 [后端演示步骤](docs/backend-demo.md) 或 `python scripts/backend_smoke.py --restart` 验证。脚本会创建少量演示素材及任务记录，并在指定 --restart 时重启本项目后端。

后台流程是 VALIDATE（素材检查）→ GENERATE（模拟生成）→ FINALIZE（结果整理）。模拟生成选择第一张输入图片，结果整理复制成独立结果文件；不调用 AI，也不伪称实际完成图片变换。前端上传、制作任务 UI 和刷新跟踪仍待接入。

## 5. 用户身份

两套固定演示账户 alice（小拾）、bob（小光），通过 Java 登录接口选择。会话令牌随机生成，数据库只存摘要和过期时间，浏览器用 HttpOnly / SameSite=Lax cookie。更换账户会撤销当前旧会话；素材、任务、历史及结果均在 Java 校验归属。该方案用于本机题目演示，不是生产账号认证；DEMO_ENABLED=false 会关闭演示登录入口和客户端模拟参数。

## 6. 完成度与实际投入

以 [验收清单](docs/acceptance.md) 和 [验证记录](docs/verification.md) 为准。前端业务闭环尚未完成。服务重启时将上一次运行中的未完成任务标记为失败，不续跑；数据库或磁盘持续不可用时不能保证即时写入失败状态。当前仅支持单个 Java 实例，不支持集群协调、自动重试、受理幂等或素材自动清理。实际投入记录在 [开发记录](docs/worklog.md)，区分 AI 执行时间与本人投入，不编造工时。

## 7. 部署与回滚

本地验收使用 Compose，无需公网部署。更新前保存当前验收 commit，并备份数据库和素材卷；执行 `docker compose up --build -d` 后检查健康与关键流程。代码回滚可切换到已验证 commit 后重建镜像；数据库迁移不会自动降级，若新版本存在不兼容迁移，需按备份恢复后再运行旧版本。

`docker compose down` 停止并删除容器，保留数据卷。不要在需要保留历史时使用 `down -v`。完整备份恢复命令将在最终交付阶段补充并验证。

## 8. AI 使用说明

使用 Codex 辅助开发：先将题目 R-01～R-14 拆为验收清单，再确定 API、数据表和状态边界，分阶段生成代码，运行检查并修复问题。第一阶段例子：模板数据由 Java/MyBatis 查询 MySQL，Next.js 只负责界面和中间层；通过容器与 HTTP 验证链路，未运行部分如实标明。最终由提交者阅读代码、确认设计并演示关键行为。无需附私人聊天记录。

## 9. 来源与提交

框架通过官方包仓库安装；工程代码自行组织，未复用成品模板站。参考题目截图的主要布局，未复制题目品牌或图片。六张 SVG 封面由 `scripts/generate-demo-assets.py` 绘制，可离线重新生成，无第三方素材授权依赖。

源码仓库：[M1kasali/xiaoying-material-studio](https://github.com/M1kasali/xiaoying-material-studio)（私有）。默认分支 `main`；[CI 运行记录](https://github.com/M1kasali/xiaoying-material-studio/actions)。当前提交为工程骨架阶段，不是最终验收版本；最终交付时注明验收 commit 和完成度。私有仓库交付需确保评估者具有读取权限，或另行提供源码压缩包。

本地查看当前提交：`git rev-parse HEAD`。提交源码时排除 `.env`、依赖、构建缓存、用户上传文件和题目原件。
