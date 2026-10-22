export const categories: Record<string, string> = {
  SYSTEM_FAILURE: '系统故障', ACCESS_REQUEST: '权限申请', FEATURE_SUGGESTION: '功能建议',
  USAGE_QUESTION: '使用咨询', UNCONFIRMED: '待确认',
}
export const statuses: Record<string, string> = {
  PENDING: '待处理', IN_PROGRESS: '处理中', AWAITING_CONFIRMATION: '待确认完成', REWORK: '返工', COMPLETED: '已完成',
}
export const resolutions: Record<string, string> = {
  RESOLVED: '已解决', ANSWERED: '已解答', APPROVED: '申请通过', REJECTED: '拒绝', ACCEPTED: '建议采纳', DEFERRED: '建议暂缓',
}
export function resultOptions(category: string): string[] {
  return ({ SYSTEM_FAILURE: ['RESOLVED'], ACCESS_REQUEST: ['APPROVED', 'REJECTED'],
    FEATURE_SUGGESTION: ['ACCEPTED', 'DEFERRED', 'REJECTED'], USAGE_QUESTION: ['ANSWERED'],
    UNCONFIRMED: ['RESOLVED', 'ANSWERED', 'REJECTED'] } as Record<string, string[]>)[category] ?? []
}
export const eventLabels: Record<string, string> = {
  CREATED: '提交工单', CATEGORY_CHANGED: '纠正分类', ASSIGNED: '分派', REASSIGNED: '转派', STARTED: '开始处理',
  COMMENTED: '回复', SUPPLEMENTED: '补充信息', RESULT_SUBMITTED: '提交处理结果', REWORK_REQUESTED: '要求返工', COMPLETED: '确认完成',
}
export const notificationLabels: Record<string, string> = {
  TICKET_CREATED: '收到新工单', TICKET_ASSIGNED: '工单已分派', TICKET_REASSIGNED: '工单已转派',
  NEW_MESSAGE: '工单有新消息', RESULT_SUBMITTED: '处理结果待确认', REWORK_REQUESTED: '工单需要返工', TICKET_COMPLETED: '工单已完成',
}
export interface Ticket {
  id: string; requestId: string; requesterId: string; requesterName: string; originalText: string
  initialCategory: string; category: string; status: string; assigneeId: string | null; assigneeName: string | null
  relatedTicketId: string | null; resolutionCode: string | null; version: number; createdAt: string; updatedAt: string
}
export interface Attachment { id: string; uploadToken: string; originalFilename: string; mediaType: string; ticketEventId: string | null }
export interface TicketEvent { id: string; actorName: string; eventType: string; content: string | null; changes: string; createdAt: string }
export interface PermissionRequest { ticketId: string; userId: string; permissionName: string; permissionCode: string | null; status: string; reviewedBy: string | null; reviewedAt: string | null }
export interface Detail { permissionRequest: PermissionRequest | null; ticket: Ticket; events: TicketEvent[]; attachments: Attachment[]; actions: string[]; relatedReadable: boolean }
export interface Notification { id: string; ticketId: string; requestId: string; type: string; readAt: string | null; createdAt: string }
export function time(value: string): string { return new Date(value + 'Z').toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai' }) }
export function notificationChanged() { window.dispatchEvent(new Event('notifications-changed')) }
