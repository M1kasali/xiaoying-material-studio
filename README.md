# 拾光 · 素材工作台

小影科技全栈笔试项目。**当前为第一阶段工程骨架，尚未完成可提交的完整笔试作品。**

已建立：数据库迁移与模板种子、Java 模板/分类/详情 API、Next.js 模板库和只读详情、原创本地封面、Docker Compose、基础 CI。待完成：身份、上传、后台制作、历史记录、多任务跟踪与核心业务测试。详见 [验收清单](docs/acceptance.md)。

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

Java 默认连接 localhost:3307；可用 DB_URL、DB_USERNAME、DB_PASSWORD 覆盖。前端 BACKEND_URL 默认 http://localhost:8080，Compose 中为 http://backend:8080。前端运行时读取后端地址，无需把内部地址交给浏览器。STORAGE_ROOT 为后续文件存储路径。

## 3. 构建、检查与接口

```sh
cd frontend
pnpm lint
pnpm typecheck
pnpm build
# 回到仓库根目录
mvn -B -ntp -f backend/pom.xml verify
docker compose config --quiet
```

CI 在 push/PR 执行上述前后端检查，并启动 Compose 对数据库驱动的模板链路做冒烟验证。当前尚无任务业务自动化测试，不能视为已满足 R-12 全部要求。远端 CI 只有推送后才能验证。

[API 契约](docs/api.md) 标明已实现与待实现接口；[架构设计](docs/architecture.md) 记录数据表、状态、执行方案及边界。

## 4. 演示

当前可演示：模板列表、三个分类、不同图片数量要求、详情、未知模板、后端不可用时的错误与重新加载。成功制作、指定阶段失败、耗时调整、多任务和刷新恢复尚待实现，不能按完整作品验收。

## 5. 用户身份

计划使用两套演示账户和服务端会话，按用户隔离素材与任务。尚未实现；方案及生产边界见架构文档。

## 6. 完成度与实际投入

以 [验收清单](docs/acceptance.md) 和 [验证记录](docs/verification.md) 为准。当前不支持制作任务、用户上传、身份或历史记录；服务重启续跑不在初版范围内。实际投入记录在 [开发记录](docs/worklog.md)，交付时区分 AI 执行时间与本人审阅/调试投入，不编造工作时长。

## 7. 部署与回滚

本地验收使用 Compose，无需公网部署。更新前保存当前验收 commit，并备份数据库和素材卷；执行 `docker compose up --build -d` 后检查健康与关键流程。代码回滚可切换到已验证 commit 后重建镜像；数据库迁移不会自动降级，若新版本存在不兼容迁移，需按备份恢复后再运行旧版本。

`docker compose down` 停止并删除容器，保留数据卷。不要在需要保留历史时使用 `down -v`。完整备份恢复命令将在最终交付阶段补充并验证。

## 8. AI 使用说明

使用 Codex 辅助开发：先将题目 R-01～R-14 拆为验收清单，再确定 API、数据表和状态边界，分阶段生成代码，运行检查并修复问题。第一阶段例子：模板数据由 Java/MyBatis 查询 MySQL，Next.js 只负责界面和中间层；通过容器与 HTTP 验证链路，未运行部分如实标明。最终由提交者阅读代码、确认设计并演示关键行为。无需附私人聊天记录。

## 9. 来源与提交

框架通过官方包仓库安装；工程代码自行组织，未复用成品模板站。参考题目截图的主要布局，未复制题目品牌或图片。六张 SVG 封面由 `scripts/generate-demo-assets.py` 绘制，可离线重新生成，无第三方素材授权依赖。

源码仓库：[M1kasali/xiaoying-material-studio](https://github.com/M1kasali/xiaoying-material-studio)（私有）。默认分支 `main`；[CI 运行记录](https://github.com/M1kasali/xiaoying-material-studio/actions)。当前提交为工程骨架阶段，不是最终验收版本；最终交付时注明验收 commit 和完成度。私有仓库交付需确保评估者具有读取权限，或另行提供源码压缩包。

本地查看当前提交：`git rev-parse HEAD`。提交源码时排除 `.env`、依赖、构建缓存、用户上传文件和题目原件。
