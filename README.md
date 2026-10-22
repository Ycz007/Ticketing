# 企业内部问题登记小助手

一个面向企业内部的问题反馈与工单协作系统。员工可以提交系统故障、权限申请、功能建议和使用咨询，协调员负责分派与审批，技术支持跟进处理，让问题从提交到解决都有记录可查。

项目采用前后端分离架构，支持角色权限控制、图片附件、操作时间线和站内通知，适合内部问题登记、团队协作演示及二次开发。

## 项目预览

准备两张截图，放到 `docs/images/` 目录，使用下面的文件名即可显示。建议使用桌面浏览器截图，宽度约 1440 像素，隐藏密码、真实内部信息及浏览器无关区域。

| 截图 | 保存位置 | 建议内容 |
| --- | --- | --- |
| 工单工作台 | `docs/images/workbench.png` | 使用协调员账号登录，展示状态统计、分类筛选和几条不同状态的工单 |
| 权限申请审批 | `docs/images/permission-approval.png` | 使用协调员账号打开一条待审批权限申请，展示申请权限、申请原因和通过 / 拒绝操作区 |

### 工单工作台

![工单工作台：状态统计、分类筛选与工单列表](docs/images/workbench.png)

### 权限申请审批

![权限申请详情：申请内容与协调员审批操作](docs/images/permission-approval.png)

## 一、环境要求

- Java 21、Maven 3.9+
- Node.js 22.12+、npm
- MySQL 8.0.16+、MinIO
- Navicat 或 MySQL 命令行客户端
- Docker / Docker Compose（可选，使用已有数据库和 MinIO 时不需要）

### 技术栈与目录

| 层级 | 技术 |
| --- | --- |
| 前端 | Vue 3、TypeScript、Vite、Vue Router、Pinia |
| 后端 | Java 21、Spring Boot 3.5、Spring Security、MyBatis |
| 数据库 | MySQL 8 |
| 图片存储 | MinIO |
| 部署 | Spring Boot JAR + Nginx；可选 Docker Compose 启动数据库和 MinIO |

```text
backend/               后端源码与测试
frontend/              前端源码
database/              数据库 SQL
  ticketing.sql        Navicat 导出的表结构与数据
docs/                  需求、接口、权限与架构说明
  images/              README 展示截图
scripts/               Windows 本地启动脚本
compose.yml            MySQL 与 MinIO 的本地容器配置
.env.example           环境变量示例
```

## 二、启动步骤

### 1. 配置环境变量

在项目根目录复制配置文件：

```powershell
Copy-Item .env.example .env
```

根据实际环境编辑 `.env`。以下示例适配仓库的 Compose 配置；请替换所有密码占位符，不要将真实凭据提交到仓库。

```dotenv
DB_URL=jdbc:mysql://127.0.0.1:3306/ticketing?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&characterEncoding=UTF-8
DB_USERNAME=ticketing
DB_PASSWORD=replace-with-database-password
MYSQL_ROOT_PASSWORD=replace-with-different-root-password
MINIO_ENDPOINT=http://127.0.0.1:9000
MINIO_ACCESS_KEY=replace-with-minio-user
MINIO_SECRET_KEY=replace-with-minio-password
MINIO_BUCKET=ticketing-images
DEMO_ENABLED=false
DEMO_PASSWORD=replace-with-demo-password-at-least-12-characters
SERVER_PORT=8080
SESSION_COOKIE_SECURE=false
```

配置文件使用不带引号的 `NAME=value` 格式。Compose 创建的应用数据库账号为 `ticketing`，`DB_USERNAME` 要与之对应；使用已有数据库时填写实际账号。

使用远程 MinIO 时，将 `MINIO_ENDPOINT` 改为 `http://服务器IP:9000`，同时填写该服务器有效的访问凭据。后端连接的是 **API 端口**，不是控制台端口；例如服务器将控制台映射到 9090 时，浏览器访问 9090，后端仍配置 API 的 9000。

### 2. 初始化数据库与图片存储

已有可用服务可跳过容器启动。在项目根目录执行：

```powershell
docker compose up -d
```

仓库 Compose 默认映射：

| 服务 | 地址 |
| --- | --- |
| MySQL | `127.0.0.1:3306` |
| MinIO API | `http://127.0.0.1:9000` |
| MinIO 控制台 | `http://127.0.0.1:9001` |

端口仅绑定本机。已有 MySQL 占用 3306 时，可只启动 `docker compose up -d minio`，或调整 Compose 主机端口并同步修改 `DB_URL`。修改 `.env` 不会重置已有 MySQL 数据卷中的账号密码。

**初始化数据库：**

1. 在 Navicat 中连接 MySQL，创建字符集为 `utf8mb4` 的空数据库 `ticketing`（Compose 已自动创建时直接使用）。
2. 右键该数据库 → **运行 SQL 文件** → 选择项目中的 `database/ticketing.sql`。
3. 导入完成后，确认存在 `users`、`roles`、`permissions`、`tickets`、`user_permission` 等表。

命令行也可以在选定数据库后导入：

```sql
USE ticketing;
SOURCE F:/MyProject/github/Ticketing/database/ticketing.sql;
```

将 `SOURCE` 路径替换为你的实际路径。该文件包含建表和数据，带有 `DROP TABLE IF EXISTS`，**只用于初始化空库或明确执行完整恢复，不要直接导入有业务数据的数据库**。应用不会自动建表；普通升级需单独准备增量 SQL。

**初始化图片桶：**

1. 打开 MinIO 控制台，使用 `.env` 对应的 `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` 登录。
2. 进入 **Buckets → Create Bucket**，创建 `ticketing-images`，保持私有权限。
3. 已有其他桶时，让 `MINIO_BUCKET` 与实际桶名称一致。

SQL 只保存图片元数据，不包含图片文件。导入 SQL 后若要查看历史图片，还需从原 MinIO 迁移对应桶中的对象，并保留原对象路径；也可以先通过新工单上传一张图片验证存储。

### 3. 启动后端

在项目根目录打开 PowerShell：

```powershell
.\scripts\Start-Backend.ps1
```

脚本会读取根目录 `.env`，然后启动 Spring Boot。健康检查地址为 `http://127.0.0.1:8080/api/health`，正常返回 `{"status":"UP"}`。

直接从 IDEA 或执行 `mvn spring-boot:run` 时，需要自行设置相同环境变量；Spring Boot 不会自动加载根目录 `.env`。修改连接配置后需重启后端。健康接口只验证进程存活，不能替代数据库及图片上传检查。

### 4. 启动前端与登录

另开一个终端：

```powershell
cd frontend
npm ci
npm run dev
```

浏览器打开 `http://127.0.0.1:5173`。开发服务器将 `/api` 请求代理到本机 8080 端口。

| 演示账号 | 角色 |
| --- | --- |
| `requester1`、`requester2` | 提交者 |
| `coordinator1` | 协调员 |
| `support1`、`support2` | 技术支持 |

**从 SQL 导入的账号保留原数据库密码**，应使用导出前的登录密码。SQL 保存的是 BCrypt 哈希，不能从中读取明文密码。

`DEMO_ENABLED=true` 时，应用只为缺失的演示账号创建用户，密码取自 `DEMO_PASSWORD`（至少 12 个字符，UTF-8 不超过 72 字节）；**不会覆盖导入账号的已有密码**。正式使用应关闭演示初始化，并配置独立账号与密码。

### 5. 验证业务流程

- 提交者新建一条普通工单并上传图片；协调员分派给技术支持，处理后由提交者确认。
- 提交者申请“读取全部工单”等权限；协调员直接同意，提交者刷新工作台后检查新权限是否生效。
- 再提交一条申请并拒绝，确认工单结束且没有新增授权。

### 6. 服务器部署（可选）

以下以单台 Linux 服务器为例：MySQL、MinIO 继续按上述方式准备，后端运行 JAR，Nginx 同时提供前端静态文件和 `/api` 反向代理。示例假设源码位于 `/opt/ticketing`。

#### 构建与启动后端

在已安装构建工具的环境执行：

```bash
cd /opt/ticketing/backend
mvn -DskipTests package
cd ../frontend
npm ci
npm run build
```

后端产物为 `backend/target/ticketing-backend-0.1.0-SNAPSHOT.jar`，前端产物为 `frontend/dist/`。也可以在本地完成构建后，将产物与部署配置上传到服务器。

将实际环境变量配置到 `/opt/ticketing/.env`。在服务器上使用专用服务账号运行后端：

```bash
sudo useradd --system --home-dir /opt/ticketing --shell /usr/sbin/nologin ticketing
sudo chown root:ticketing /opt/ticketing/.env
sudo chmod 640 /opt/ticketing/.env
```

已有同名服务账号时跳过创建。确保该账号能够读取 JAR 和访问父目录。创建 `/etc/systemd/system/ticketing.service`：

```ini
[Unit]
Description=Ticketing application
After=network.target

[Service]
Type=simple
User=ticketing
WorkingDirectory=/opt/ticketing/backend
EnvironmentFile=/opt/ticketing/.env
ExecStart=/usr/bin/java -jar /opt/ticketing/backend/target/ticketing-backend-0.1.0-SNAPSHOT.jar
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

将 Java 路径、JAR 路径和文件名替换为服务器实际值，然后执行：

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now ticketing
sudo systemctl status ticketing
# 查看日志
sudo journalctl -u ticketing -f
```

#### 配置 Nginx

安装 Nginx 后，在站点配置中加入以下内容，并替换域名与前端目录：

```nginx
server {
    listen 80;
    server_name your-domain.example;
    root /opt/ticketing/frontend/dist;
    index index.html;
    client_max_body_size 26m;

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

`proxy_pass` 保留上述写法，以保留请求中的 `/api` 路径；`try_files` 用于支持 Vue 路由直接访问与刷新。确保 Nginx 用户能读取前端目录。

```bash
sudo nginx -t
sudo systemctl reload nginx
```

浏览器通过域名或服务器 IP 访问，前端和 API 使用同一站点地址。正式对外提供服务时配置 HTTPS，并设置 `SESSION_COOKIE_SECURE=true` 后重启后端；本地 HTTP 调试保持 `false`。数据库、MinIO 和后端端口应仅允许应用所需的网络访问。

当前 Session 保存在单个后端实例中，上述方案按单实例部署。MySQL 与 MinIO 都需要持久化存储和独立备份。

### 7. 常见启动问题

| 现象 | 排查方向 |
| --- | --- |
| 登录失败 | 导入账号使用原密码；修改 `DEMO_PASSWORD` 不会重置已有密码；确认后端连接的是正确数据库 |
| 提示图片存储不可用 | 检查 MinIO API 地址、访问凭据、私有桶名称；确认服务可达且桶已创建 |
| 历史图片打不开 | SQL 不包含图片对象，检查是否迁移了原 MinIO 桶及对象路径 |
| 权限审批入口不显示 | 确认使用协调员账号，数据库包含 `ticket:access:approve` 及其角色授权，后端已更新并重启 |
| 前端 API 请求失败 | 开发时检查后端 8080 端口；部署后检查 Nginx `/api/` 代理与端口配置 |
| 刷新详情页出现 404 | 检查 Nginx 是否配置了 `try_files $uri $uri/ /index.html` |

## 三、API / 操作说明

### 接口约定

前端通过同源 `/api` 调用后端，使用 Session Cookie 维持登录。写操作需先请求 `GET /api/auth/csrf`，将响应中的 `token` 放入 `headerName` 指定的请求头；登录及退出后重新获取。登录使用表单，业务请求使用 JSON，图片上传使用 `multipart/form-data`。

内部 ID 以字符串返回，`version` 和数量为数字。工单修改携带详情中的最新 `version`，冲突返回 `409`，刷新详情后再操作。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/api/health` | 进程存活检查 |
| GET / POST | `/api/auth/csrf`（GET）、`/api/auth/login`、`/api/auth/logout`（POST） | 获取 CSRF 令牌、登录及退出 |
| GET | `/api/me` | 当前用户、角色及有效权限 |
| GET / POST | `/api/tickets` | 分页筛选工单 / 创建工单 |
| GET | `/api/tickets/{id}` | 工单详情、时间线、附件、可执行动作及权限申请 |
| GET | `/api/workbench/counts` | 当前数据范围内的状态统计 |
| GET | `/api/handlers`、`/api/permissions` | 查询处理人 / 可申请权限选项 |
| POST | `/api/tickets/{id}/assign`、`/category` | 分派 / 转派、纠正分类 |
| POST | `/api/tickets/{id}/start`、`/result` | 开始处理、提交处理结果 |
| POST | `/api/tickets/{id}/comments` | 补充信息或公开回复 |
| POST | `/api/tickets/{id}/confirm`、`/rework` | 确认完成、要求返工 |
| POST | `/api/tickets/{id}/approve` | 协调员直接审批并在通过时授予权限 |
| POST | `/api/attachments` | 上传图片，表单字段为 `file` |
| GET | `/api/attachments/{id}/content` | 按权限读取图片 |
| GET | `/api/notifications`、`/api/notifications/unread-count` | 本人通知、未读数量 |
| POST | `/api/notifications/{id}/read` | 标记本人通知已读 |

表中同一行的工单动作均使用 `/api/tickets/{id}/` 前缀。`GET /api/tickets` 支持 `page`、`size`、`status`、`category`、`requestId`、`unassigned` 查询参数。

### 操作示例：申请并授予权限

1. 提交者从 `GET /api/permissions` 返回的选项中选择权限，创建申请：

```http
POST /api/tickets
Content-Type: application/json
```

```json
{
  "requestId": "access-demo-001",
  "originalText": "需要查看全部工单，便于跨团队跟进问题。",
  "category": "ACCESS_REQUEST",
  "permissionName": "ticket:read:all",
  "relatedTicketId": null,
  "attachmentTokens": []
}
```

2. 协调员打开申请详情，在 `POST /api/tickets/{id}/approve` 提交以下正文，`version` 使用最新值：

```json
{"version": 0, "resolutionCode": "APPROVED", "content": "同意授予查看全部工单权限"}
```

`permissionName` 请求字段实际传权限编码，数据库同时保存编码与显示名称。审批通过时，`user_permission` 记录转为 `APPROVED` 并参与鉴权；工单直接完成，通知提交者。拒绝时传 `REJECTED`，工单结束但不授权。旧自由文本申请在通过时需额外传 `permissionCode`，由协调员明确选择实际权限。

普通工单在页面上依次进行分派、开始处理、提交结果、确认或返工；协调员自己处理时，先将处理人设为自己。图片先通过上传接口取得 `uploadToken`，再随创建或补充内容一并提交。

常见状态码：`400` 参数错误、`401` 未登录、`403` 权限或 CSRF 校验失败、`404` 不存在或超出数据范围、`409` 状态 / 版本 / 提交编号冲突、`503` 图片存储不可用。更多请求字段见 [API 与操作说明](docs/API与操作说明.md)。

## 四、主要数据结构

数据库结构与示例数据保存在 [database/ticketing.sql](database/ticketing.sql)，共 10 张表：

| 表名 | 作用 | 主要字段 |
| --- | --- | --- |
| `users` | 用户账号 | `id`、`username`、`display_name`、`password_hash`、`enabled` |
| `roles` | 角色定义 | `id`、`code`、`name` |
| `permissions` | 权限定义 | `id`、`code`、`name` |
| `user_roles` | 用户与角色关联 | `user_id`、`role_id` |
| `role_permissions` | 角色与权限关联 | `role_id`、`permission_id` |
| `tickets` | 工单主体 | `request_id`、`requester_id`、`assignee_id`、`original_text`、`initial_category`、`category`、`status`、`resolution_code`、`version` |
| `ticket_events` | 公开沟通及操作时间线 | `ticket_id`、`actor_id`、`event_type`、`content`、`changes`（JSON） |
| `attachments` | 图片元数据及绑定关系 | `upload_token`、`uploader_id`、`ticket_id`、`ticket_event_id`、`bucket_name`、`object_key`、`storage_state` |
| `notifications` | 按接收人保存站内通知 | `recipient_id`、`ticket_id`、`event_id`、`type`、`read_at`、`invalidated_at` |
| `user_permission` | 权限申请及直接授权 | `ticket_id`（主键）、`user_id`、`permission_code`、`permission_name`、`status`、`reviewed_by`、`reviewed_at` |

用户通过 `user_roles → roles → role_permissions → permissions` 获得角色权限，再合并 `user_permission` 中状态为 `APPROVED` 的本人权限。`PENDING` 和 `REJECTED` 记录不授予权限；`permission_code` 关联现有权限定义。

工单通过 `requester_id`、`assignee_id` 关联提交者与处理人，一条工单可关联多条事件、图片及通知。图片二进制文件保存在 MinIO，数据库只存元数据。普通工单状态为 `PENDING → IN_PROGRESS → AWAITING_CONFIRMATION → COMPLETED`；返工进入 `REWORK` 后重新处理。权限申请审批后直接进入 `COMPLETED`。

## 五、业务假设

### 角色与业务流程

| 角色 | 默认职责 |
| --- | --- |
| 提交者 | 提交问题或权限申请、查看本人工单、补充说明、确认普通工单结果 |
| 协调员 | 查看全部工单、纠正分类、分派 / 转派、处理工单、审批并授予申请权限 |
| 技术支持 | 查看分派给自己的工单、处理问题、提交处理结果 |

上表为默认角色权限；协调员审批授予的具体权限会加入用户的有效权限集合，不改变用户角色。

普通工单：`提交 → 协调员分派 → 处理人开始处理 → 提交结果 → 提交者确认完成 / 要求返工`。

权限申请：`选择权限并提交 → 协调员同意 / 拒绝 → 同意则授予权限 → 工单完成并通知提交者`。

权限授权针对本系统 `permissions` 表中的权限，外部业务系统的账号开通需要另行集成。当前没有自助注册及完整账号管理页面。

- 分类由提交者手动选择，不做自动识别；“待确认”用于无法明确归类的反馈。普通工单可由协调员纠正分类，权限申请不能与普通工单互改类型。
- 普通工单由当前处理人提交结果，原提交者负责验收；完成后只读，后续问题通过新工单关联本人已完成工单继续跟进。
- 功能建议的“采纳 / 暂缓 / 拒绝”是评估结论，采纳不等于已经开发上线，也不承诺交付时间。
- 权限申请由协调员直接作出决定，审批通过即获得所选系统权限，不再经过普通工单的分派和验收流程。
- 工单创建使用 `requestId` 保证幂等：重试需保持提交内容一致；工单变更使用版本号与事务保证一致性，关键通知写入失败时相关业务操作回滚。
- 沟通内容对有权访问该工单的用户公开，不区分内部备注；工单及图片读取均校验当前权限，不能只依赖前端按钮控制。
- 当前部署按内部使用、单后端实例设计；Session 存于实例内存，数据库与 MinIO 需要分别持久化和备份。

## 六、已完成与未完成内容

### 已完成

- **问题登记**：支持系统故障、权限申请、功能建议、使用咨询和待确认五种分类，可上传图片、关联本人已完成工单。
- **工单处理**：协调员分派或转派，处理人开始处理并提交结果，提交者确认完成或要求返工。
- **权限审批与授权**：提交者选择具体系统权限并说明原因；协调员同意后直接授权，拒绝不授权，审批后结束工单，无需再次确认。
- **图片附件**：支持 PNG、JPEG、WebP，单张最大 5 MiB，每个工单最多 5 张；图片保存在 MinIO 私有桶中，经后端校验权限后读取。
- **操作留痕**：保存原始反馈、公开沟通、分派、处理结果及审批记录。
- **站内通知**：通知新工单、分派、回复及处理结果，页面可见时默认每 15 秒刷新未读数量。
- **权限控制**：采用 Session 登录、BCrypt 密码哈希、CSRF 防护与数据库 RBAC；用户直接授权与角色权限合并生效，已有会话在下一次请求时刷新权限。

### 未完成内容

- 自助注册、账号管理页面、密码重置、企业 SSO 集成。
- 用户直接权限的撤销、到期和批量管理页面，外部业务系统的自动开通接口。
- 内部私密备注、通知全部已读、邮件 / 短信通知、WebSocket / SSE 实时推送。
- 上传频率限流、多实例 Session 共享、定时清理任务协调及完整运维监控。
- SLA 超时升级、业务争议仲裁、自动关闭等扩展流程。

已完成核心功能不等于完成生产验收。权限申请已做真实 MySQL 的申请、通过授权、会话权限刷新与拒绝不授权验证；完整浏览器流程、MinIO 文件迁移和生产环境部署仍需按目标环境验收。

### 验证方式与相关文档

```bash
# 后端测试（默认不运行需要真实数据库的集成测试）
cd backend
mvn test

# 前端测试与构建
cd ../frontend
npm test
npm run build
```

真实数据库授权测试需先准备完整表结构，设置 `TICKETING_DB_INTEGRATION=true` 及数据库环境变量后执行 `mvn -Dtest=PermissionGrantIntegrationTest test`；测试记录通过事务回滚。

- [需求规格说明书](docs/需求规格说明书.md)
- [系统架构设计](docs/系统架构设计.md)
- [RBAC 权限设计](docs/RBAC权限设计.md)
- [数据库设计](docs/数据库设计.md)
- [API 与操作说明](docs/API与操作说明.md)
- [架构设计与通知方案对比](docs/架构设计与通知方案对比.md)
