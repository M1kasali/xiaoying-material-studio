# 验证记录

日期：2026-11-11。业务功能未实现的部分不记作通过。

| 检查 | 当前结果 |
| --- | --- |
| 前端依赖安装及锁文件 | 通过；pnpm-lock.yaml 已生成；peer 检查无冲突 |
| ESLint / TypeScript / 构建 | 通过；本机和最终前端容器执行 lint、typecheck、test、build |
| Java 构建 | 通过；Temurin 21.0.13 + Maven 3.9.11，15 个单元测试全部通过，本机及 Linux 容器均执行完整 `mvn verify` |
| Compose 配置 | `docker compose config --quiet` 通过 |
| Compose 启动 | 通过；重启 Docker 并配置官方镜像缓存后，`docker compose up --build --wait --wait-timeout 180` 成功；MySQL、Java、Next.js 均已运行 |
| MySQL 初始化 / 模板 API | 通过；初次空库 V1/V2、后续增量 V3 均成功；6 个模板、3 个分类、2 个演示用户；查询与错误行为已验证 |
| 桌面 / 小屏页面 | 通过；Chrome / Playwright 使用真实后端检查 1440px、390px；制作、结果和移动端截图已归档 docs/screenshots |
| 后端重启 / 重复初始化 | 通过；重启后健康状态 UP，未完成任务明确中止，已完成结果及用户会话保留，新任务仍可运行 |
| GitHub Actions 远端运行 | d18f26f 和后端业务提交 9ea979f 的 Verify 均成功；后端运行链接见下文。最终前端提交新增浏览器测试，最新 commit 状态以 Actions 为准 |
| 上传、身份、任务核心测试 | 通过；真实 API 脚本覆盖账户隔离、上传校验、异步步骤、指定失败、并发任务、分页、终态稳定、切换/退出和重启 |
| 前端上传 / 任务状态 / 账户交互 | 通过；5 个状态测试及 9 个真实浏览器场景通过，覆盖完整业务闭环 |

## 前端测试与界面证据

- 用户复核录像后，补查发现真实交互缺陷：同一模板页点击「重新制作」没有重置失败模拟设置。现已改为故障注入单次生效、显式警告与按钮文案、重试链接触发表单重建，并增加端到端回归用例验证重试后默认成功。此前通过的 8 个场景未覆盖此路径，不代表没有缺陷。

- 状态测试 5 个：多任务 ID 隔离、终态不回退、较旧响应不覆盖、仅活动任务轮询及退避上限、上传类型/大小/空文件校验。
- Playwright 9 个场景全部通过（重试修复版本地运行约 51 秒）：上传校验/替换/移除；离开并刷新历史恢复；并发成功和失败；查询网络错误恢复；账户切换与越权；离页清理及 390px 无溢出；替换正在上传的旧素材；两图要求及提交中防重复点击；失败模拟单次生效及同页重新制作重置后成功。
- 除故意阻断查询和延迟上传的异常注入外，浏览器访问真实 Next.js → Java → MyBatis → MySQL。成功结果字节与上传素材一致，未用前端定时器伪造阶段。
- `docs/screenshots/catalog.png`、`production.png`、`mobile-result.png` 为真实运行截图。`scripts/capture-demo.mjs` 可复现，截图使用原创 PNG 演示输入。
- 后端业务 CI：[9ea979f / Verify 成功](https://github.com/M1kasali/xiaoying-material-studio/actions/runs/42979653340)。最终版本 CI 包含浏览器场景、Compose 空库启动和重启验证；最终运行链接随交付回复提供，避免把本地通过等同于远端通过。

## 后端测试证据

- TaskRunnerTest：8 个用例（含三个阶段的参数化失败）；TaskSubmissionTest：2 个事务提交边界用例；AssetServiceTest：4 个上传与落盘异常用例；TaskDispatcherTest：1 个队列饱和用例。合计 15 个，失败/错误/跳过均为 0。
- `python scripts/backend_smoke.py --restart` 首次完整通过 110 次检查；补充分页时间范围校验后，最终本地运行通过 125 次检查。使用真实 Java/MySQL 和本地文件，未 mock 业务接口。检查次数会随已有历史数量及查询次数变化，不作为固定覆盖率指标。
- 本机依赖下载持续 TLS/EOF 中断时，从 Maven Central 备用官方入口下载并校验缺失依赖；容器构建复用本机已校验的 junit-platform-launcher 缓存，仍执行完整测试。后续远端 CI 使用干净运行器验证依赖安装和空库启动。
- 基础镜像使用本机已验证的 manifest digest，避免每次构建都依赖可变标签查询；没有关闭 TLS 验证或跳过测试。

## 浏览器检查边界

- 初次检查（Docker 修复前）：Java 不可用时模板中间层返回 503；错误提示和重新加载按钮；详情错误边界；不存在路径的 404 页面。另用 SQL 种子 fixture 检查过列表布局，未将其计为后端联调。
- Docker 修复后真实联调：浏览器 → Next.js → Java → MyBatis → MySQL。未拦截或模拟响应，验证 6 张卡片、分类筛选后 2 张、详情要求、图片加载、未知模板、390px 没有横向溢出，未出现浏览器页面异常。
- 第一阶段临时截图位于 tmp；最终截图归档在 docs/screenshots 并纳入提交。
- `pnpm build` 生成 standalone 目录并复制静态文件；`pnpm start` 使用该目录，避免本地运行与容器内容不一致。

## Docker 修复记录

1. 用户重启后，Docker Desktop 4.89.0 / Engine 29.7.2 恢复响应；此前 socket 清理被自动审批拒绝，未执行。
2. 原始 Docker Hub 连接持续 EOF，Google 官方缓存可访问。备份本机 daemon.json 后添加 `registry-mirrors: ["https://mirror.gcr.io"]`，重启引擎并确认配置生效。备份在 `tmp/docker-daemon-before-mirror-20261111-141849.json`，项目仍使用原始官方镜像名称。
3. `docker pull mysql:8.4` 与 MySQL 容器实际执行成功。参考：[Google 官方 Docker Hub 缓存说明](https://docs.cloud.google.com/artifact-registry/docs/pull-cached-dockerhub-images)。这解决了当前项目镜像拉取；未声称 Docker Hub 直连网络已恢复。
4. 第一阶段 Maven 下载出现 TLS 连接中断；后端 Dockerfile 移除额外的 go-offline 预下载，使用 BuildKit Maven 缓存运行完整 `mvn verify`。当时尚无业务测试；本轮已补齐上述 15 个测试。
5. Compose 从空数据卷启动，Flyway 两次迁移成功；后端重启后无重复数据。前后端均在容器内执行，页面数据来自真实 MySQL。

已知兼容提示：Spring Boot BOM 管理的 Flyway 对 MySQL 8.4 输出“超出已测试版本”提示；实际建表、种子迁移、增量升级、重启及空库 CI 均已通过。交付保留已验证的依赖组合，未为消除提示临时升级数据库迁移库；正式生产使用前应重新确认支持矩阵。

## 本机工具与 Git

Java 21 和 Maven 3.9.11 放在忽略提交的 `tmp/tooling/` 中；JDK 使用官方 SHA-256 校验。没有修改系统 JAVA_HOME 或全局 Java 版本。

Git 使用 main 分支，远端为私有仓库 `M1kasali/xiaoying-material-studio`。用户已确认新建私有仓库。仅为本项目配置提交者 M1kasali 和 GitHub 隐私邮箱，使用已有 Git Credential Manager 登录授权；未要求用户提供密码或 Token。源题 PDF、临时工具、缓存、上传目录和真实 `.env` 均被排除。正式源码压缩包从验收 commit 生成，附 SOURCE_COMMIT.txt；提交前应确保评估者能够读取仓库或直接提供压缩包。
