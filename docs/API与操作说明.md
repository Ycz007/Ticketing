# API 与操作说明

日期：2026-10-21。实现代码已生成，尚未做真实 MySQL/MinIO 联调，本文不是测试通过报告。

## 1. 使用方式

按根目录 README 配置 .env，手动执行 MySQL 建表和角色初始化脚本，准备私有 MinIO 桶，再启动后端与 Vue。应用不自动建表。提交者创建时主动选择五种类型；协调员在详情选择处理人（可以本人）；当前处理人开始处理并提交结论；提交者确认或要求返工。

所有业务 API 要求登录 Cookie。POST 写操作先 GET /api/auth/csrf，将返回的 token 放入 headerName 对应 Header；登录、退出后重新获取。JSON 操作使用 application/json，登录使用表单，图片上传使用 multipart/form-data。

内部 ID 用字符串返回，版本号和数量为数字；前端提交 Long 字段可以使用十进制字符串。所有日期为 UTC 的 ISO 本地日期格式，前端补 UTC 标识并转北京时间显示。

## 2. 工单接口

| 方法/路径 | 功能与授权 |
| --- | --- |
| POST /api/tickets | 提交者创建；重复请求返回同一工单 |
| GET /api/tickets | 按角色数据范围分页，参数 page/size/status/category/requestId/unassigned |
| GET /api/tickets/{id} | 工单、公开事件、附件、可执行动作及关联读取资格 |
| GET /api/handlers | 协调员查询有效协调员/技术支持候选 |
| GET /api/workbench/counts | 本人数据范围内按状态统计，协调员另有未分派数量 |
| POST /api/tickets/{id}/assign | 协调员分派/转派，转派原因必填；状态保持 |
| POST /api/tickets/{id}/category | 协调员纠正，最初提交分类不变 |
| POST /api/tickets/{id}/start | 当前处理人开始处理/返工 |
| POST /api/tickets/{id}/comments | 本工单有权用户公开沟通；仅原提交者可附图 |
| POST /api/tickets/{id}/result | 当前处理人提交结论并进入待确认完成 |
| POST /api/tickets/{id}/confirm | 原提交者确认完成 |
| POST /api/tickets/{id}/rework | 原提交者填写原因返工 |

创建示例：

```json
{
  "requestId": "R001",
  "originalText": "导出报表后没有文件，截图如下。",
  "category": "SYSTEM_FAILURE",
  "relatedTicketId": null,
  "attachmentTokens": []
}
```

图片先上传，再将返回的 uploadToken 放入 attachmentTokens。创建重试必须保持编号、文字、提交分类、关联 ID 和图片集合一致；后续纠正当前分类不会影响原始请求的重试判断。

修改示例（版本从最新详情读取）：

```json
{"version": 0, "assigneeId": "3", "reason": "需要技术排查"}
```

其他动作请求体：

```text
category: {version, category, reason}
start / confirm: {version}
comments: {version, content, attachmentTokens: []}
result: {version, resolutionCode, content}
rework: {version, reason}
```

所有写入（包括公开沟通）递增工单 version。并发冲突返回409，页面刷新后由用户重新提交，不自动重放旧动作。

## 3. 类型与处理结论

| 类型 | code | 允许的处理结论 |
| --- | --- | --- |
| 系统故障 | SYSTEM_FAILURE | RESOLVED 已解决 |
| 权限申请 | ACCESS_REQUEST | APPROVED 申请通过 / REJECTED 拒绝 |
| 功能建议 | FEATURE_SUGGESTION | ACCEPTED 采纳 / DEFERRED 暂缓 / REJECTED 拒绝 |
| 使用咨询 | USAGE_QUESTION | ANSWERED 已解答 |
| 待确认 | UNCONFIRMED | RESOLVED / ANSWERED / REJECTED |

结论必须有说明，拒绝不能直接关闭工单，仍等待原提交者确认。采纳不代表功能已经上线；暂缓与拒绝是评估结论。APPROVED 是外部审批/开通结果的记录，不会调用真实授权系统。

## 4. 图片

| 方法/路径 | 用途 |
| --- | --- |
| POST /api/attachments | 提交者上传，字段 file，返回 id/uploadToken 等元数据 |
| GET /api/attachments/{id}/content | 后端按工单当前权限代理读取；本人临时图片可预览 |

支持 PNG/JPEG/WebP，单张≤5 MiB、解码≤2000万像素、每工单累计≤5张。每用户最多20个待绑定上传；临时图片24小时过期，每小时清理，启动后首次延迟1分钟。

MinIO 桶默认 ticketing-images，必须私有。前端不取得存储凭据或公开访问链接；原支持转派后新读取请求被拒绝，已经下载的图像无法撤回。临时上传失败进入待清理，不删除已绑定图片。

## 5. 通知

| 方法/路径 | 用途 |
| --- | --- |
| GET /api/notifications/unread-count | 本人有效且仍可访问的未读数 |
| GET /api/notifications?page=1 | 本人通知，每页20条 |
| POST /api/notifications/{id}/read | 本人单条通知已读；不改变工单状态 |

创建通知协调员；分派/转派通知新处理人和提交者；消息通知提交者、处理人及协调员；结果通知提交者；返工、完成通知处理人和协调员。排除操作者本人并去重。转派后旧技术支持通知失效，旧协调员仍有全局读取资格。

轮询15秒，可见页面才运行，重新可见/网络恢复立即补查。首期无全部已读、SSE 或 WebSocket。

## 6. 错误

400：非法参数、空白、未知类型或文件格式；401：未登录；403：无操作权限或 CSRF 失效；404：不存在或不在数据范围；409：版本/状态/提交编号冲突；503：图片存储不可用；500：未预期错误，正文不返回堆栈或凭据。

## 权限申请直接审批与授权

`GET /api/permissions` 返回权限选项（code/name），提交者或具备审批权限的协调员可读取。
`POST /api/tickets` 的 ACCESS_REQUEST 类型需传 `permissionName`，值为所选权限编码（例如 ticket:read:all），只允许现有权限；详情的 permissionRequest 返回 permissionCode 与 permissionName。
协调员使用 `POST /api/tickets/{id}/approve`，正文为 `{"version":0,"resolutionCode":"APPROVED","content":"同意申请"}`；拒绝用 REJECTED，说明必填。
要求 ROLE_COORDINATOR 和 ticket:access:approve；校验版本，更新 user_permission 并直接完成工单、通知提交者。同意记录立即参与系统鉴权，拒绝和待审批记录不授权；无需提交者确认。
新申请不能由协调员改成其他权限。旧自由文本申请通过时额外传 permissionCode，选择实际授予的现有权限。
现有登录会话每次请求重载角色与用户直接权限。前端轮询和导航刷新权限，无需重新登录。
