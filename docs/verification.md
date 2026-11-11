# 验证记录

日期：2026-11-11。业务功能未实现的部分不记作通过。

| 检查 | 当前结果 |
| --- | --- |
| 前端依赖安装及锁文件 | 通过；pnpm-lock.yaml 已生成；peer 检查无冲突 |
| ESLint / TypeScript / 构建 | 通过；最终配置执行 lint、typecheck、build |
| Java 构建 | 通过；Temurin 21.0.13 + Maven 3.9.11 执行 `mvn verify`，生成可执行 JAR；当前无测试用例，日志为 No tests to run，不能宣称业务测试通过 |
| Compose 配置 | `docker compose config --quiet` 通过 |
| Compose 启动 | 通过；重启 Docker 并配置官方镜像缓存后，`docker compose up --build --wait --wait-timeout 180` 成功；MySQL、Java、Next.js 均已运行 |
| MySQL 初始化 / 模板 API | 通过；空数据卷应用 V1/V2 迁移，6 个模板、3 个分类；真实 Java/MySQL 接口验证筛选、未知分类空数组、未知模板 404 |
| 桌面 / 小屏页面 | 通过；Chrome / Playwright 使用真实后端检查 1440px、390px，分类、详情、图片、404、无横向溢出与无页面异常 |
| 后端重启 / 重复初始化 | 通过；重启后健康状态 UP，模板仍为 6 条，成功迁移记录仍为 2 条 |
| GitHub Actions 远端运行 | 已配置 push/PR 触发；运行结果以仓库 Actions 为准，Git 建仓阶段正在核验 |
| 上传、身份、任务核心测试 | 尚未实现 |

## 浏览器检查边界

- 初次检查（Docker 修复前）：Java 不可用时模板中间层返回 503；错误提示和重新加载按钮；详情错误边界；不存在路径的 404 页面。另用 SQL 种子 fixture 检查过列表布局，未将其计为后端联调。
- Docker 修复后真实联调：浏览器 → Next.js → Java → MyBatis → MySQL。未拦截或模拟响应，验证 6 张卡片、分类筛选后 2 张、详情要求、图片加载、未知模板、390px 没有横向溢出，未出现浏览器页面异常。
- 最新临时截图位于 `tmp/docker-desktop.png`、`tmp/docker-detail.png`、`tmp/docker-mobile.png`；不计入正式提交材料。
- `pnpm build` 生成 standalone 目录并复制静态文件；`pnpm start` 使用该目录，避免本地运行与容器内容不一致。

## Docker 修复记录

1. 用户重启后，Docker Desktop 4.89.0 / Engine 29.7.2 恢复响应；此前 socket 清理被自动审批拒绝，未执行。
2. 原始 Docker Hub 连接持续 EOF，Google 官方缓存可访问。备份本机 daemon.json 后添加 `registry-mirrors: ["https://mirror.gcr.io"]`，重启引擎并确认配置生效。备份在 `tmp/docker-daemon-before-mirror-20261111-141849.json`，项目仍使用原始官方镜像名称。
3. `docker pull mysql:8.4` 与 MySQL 容器实际执行成功。参考：[Google 官方 Docker Hub 缓存说明](https://docs.cloud.google.com/artifact-registry/docs/pull-cached-dockerhub-images)。这解决了当前项目镜像拉取；未声称 Docker Hub 直连网络已恢复。
4. Maven 首次下载出现 TLS 连接中断；后端 Dockerfile 移除额外的 go-offline 预下载，使用 BuildKit Maven 缓存运行完整 `mvn verify`。网络重试复用缓存后构建成功，没有跳过编译或关闭 TLS 验证。当前 Java 仍无业务测试用例。
5. Compose 从空数据卷启动，Flyway 两次迁移成功；后端重启后无重复数据。前后端均在容器内执行，页面数据来自真实 MySQL。

待跟进的兼容提示：当前 Flyway 启动日志对 MySQL 8.4 输出“超出已测试版本”提示；本次建表、种子迁移、重启验证实际均通过。后续工程收尾需确认并对齐 Flyway 的正式支持版本。

## 本机工具与 Git

Java 21 和 Maven 3.9.11 放在忽略提交的 `tmp/tooling/` 中；JDK 使用官方 SHA-256 校验。没有修改系统 JAVA_HOME 或全局 Java 版本。

Git 使用 main 分支，远端为私有仓库 `M1kasali/xiaoying-material-studio`。用户已确认新建私有仓库。仅为本项目配置提交者 M1kasali 和 GitHub 隐私邮箱，使用已有 Git Credential Manager 登录授权；未要求用户提供密码或 Token。源题 PDF、临时工具、缓存、上传目录和真实 `.env` 均被排除，首次提交前常见凭据模式扫描无匹配。当前阶段不是最终验收版本。
