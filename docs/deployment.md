# 本地部署、更新和回滚

此项目只绑定宿主机 127.0.0.1，供本机验收。公网部署、HTTPS 反向代理和正式账号系统不在交付范围内。

## 首次部署

1. 安装并启动 Docker Engine / Docker Desktop，确认 `docker compose version` 可用。
2. 解压源码，进入包含 compose.yaml 的目录；可直接使用演示配置，也可复制 `.env.example` 为 `.env`。
3. 执行 `docker compose up --build --wait`。Java 等待 MySQL 健康后通过 Flyway 初始化 SQL；Next.js 等待 Java 健康后启动。
4. 访问 http://localhost:3000，并按 [演示步骤](demo.md) 验证。若修改 FRONTEND_PORT，需同步修改 ALLOWED_ORIGINS。
5. `docker compose ps` 查看状态；`docker compose logs --tail 100 backend` 查看后端启动信息。日志不应作为公网接口暴露。

## 更新前备份

记下当前验收 commit。先等待任务结束，再停止写入服务，以保证数据库和文件备份对应。以下为 Bash / Git Bash 命令；备份含用户数据，应保存在仓库之外。

```sh
docker compose stop frontend backend
docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysqldump --no-tablespaces --single-transaction -u"$MYSQL_USER" "$MYSQL_DATABASE"' > ../studio-backup.sql
docker compose cp backend:/app/data ../studio-data-backup
docker compose start backend frontend
```

mysqldump 命令在 MySQL 容器内执行，不要求宿主机安装 MySQL 客户端。备份目录应使用每次独立的新名称，避免覆盖之前的备份。上述备份/恢复操作为运维说明，未在用户现有数据上执行恢复演练。

## 更新与代码回滚

```sh
# 切换到准备验收的新 commit 后
docker compose up --build -d --wait
docker compose ps
```

运行成功/失败任务并核对历史。若仅代码不兼容，可切回上一个已验证 commit 后重新执行构建。不要回退或修改已经应用过的 Flyway SQL；历史迁移的校验和由 Flyway 管理。

如果涉及不兼容数据库迁移，代码回退不等于数据回退。应停止服务，在单独恢复环境中创建匹配旧版本的空数据库，导入备份 SQL，并恢复相同时间点的 `data` 目录后再切换；不要直接覆盖正在使用的数据卷。当前 V1～V3 的首次初始化和增量迁移已验证，跨版本灾难恢复未验证。

## 停止

`docker compose down` 停止并删除容器，保留 MySQL 和素材数据卷。下一次启动会恢复历史和文件；未完成任务会被标为服务中止，不自动续跑。`down -v` 会删除数据卷，不用于正常停止或更新。
