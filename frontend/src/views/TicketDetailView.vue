<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { api, ApiError } from '../api/http'
import { useAuthStore } from '../stores/auth'
import { categories, statuses, resolutions, resultOptions, eventLabels, time, notificationChanged, type Attachment, type Detail } from '../domain/ticket'
import ImageUpload from '../components/ImageUpload.vue'
const route = useRoute(), auth = useAuthStore()
const detail = ref<Detail | null>(null), busy = ref(false), error = ref(''), notice = ref('')
const grantCode = ref(''), permissions = ref<{ code: string; name: string }[]>([])
const handlers = ref<{ id: string; displayName: string; role: string }[]>([])
const assigneeId = ref(''), reason = ref(''), selectedCategory = ref(''), categoryReason = ref('')
const content = ref(''), resolution = ref(''), resultText = ref(''), reworkReason = ref('')
const images = ref<Attachment[]>([]), uploading = ref(false)
const ticket = computed(() => detail.value?.ticket)
const has = (action: string) => detail.value?.actions.includes(action)
const canAttach = computed(() => ticket.value?.requesterId === auth.user?.id && auth.user?.authorities.includes('attachment:upload'))
const allowedResults = computed(() => resultOptions(ticket.value?.category ?? ''))
async function load() {
  detail.value = null
  const loaded = await api.get<Detail>(`/tickets/${route.params.id}`)
  detail.value = loaded
  if (loaded.actions.includes('approve')) {
    permissions.value = await api.get('/permissions')
    grantCode.value = loaded.permissionRequest?.permissionCode ?? ''
  }
  await auth.restore()
  selectedCategory.value = loaded.ticket.category
  assigneeId.value = loaded.ticket.assigneeId ?? ''
  if (loaded.actions.includes('assign')) handlers.value = await api.get('/handlers')
  else handlers.value = []
  resolution.value = resultOptions(loaded.ticket.category)[0] ?? ''
  if (typeof route.query.notification === 'string') {
    await api.post(`/notifications/${route.query.notification}/read`)
    notificationChanged()
  }
}
async function initial() { error.value = ''; try { await load() } catch (failure) { error.value = message(failure) } }
function message(failure: unknown) { return failure instanceof Error ? failure.message : '操作失败' }
async function act(action: string, fields: Record<string, unknown> = {}) {
  if (!ticket.value) return
  busy.value = true; error.value = ''; notice.value = ''
  try {
    await api.json(`/tickets/${ticket.value.id}/${action}`, { version: ticket.value.version, ...fields })
    content.value = ''; images.value = []; reason.value = ''; reworkReason.value = ''; categoryReason.value = ''; resultText.value = ''
    await load(); notificationChanged(); notice.value = '操作成功'
  } catch (failure) {
    error.value = message(failure)
    if (failure instanceof ApiError && [404, 403].includes(failure.status)) detail.value = null
    else if (failure instanceof ApiError && failure.status === 409) {
      try { await load() } catch { detail.value = null }
    }
  } finally { busy.value = false }
}
function changes(value: string): string {
  try {
    const parsed = JSON.parse(value) as Record<string, string>
    return Object.entries(parsed).map(([key, val]) => `${key}: ${statuses[val] ?? categories[val] ?? resolutions[val] ?? val}`).join('；')
  } catch { return '' }
}
onMounted(initial)
watch(() => route.params.id, () => { images.value = []; content.value = ''; void initial() })
</script>
<template>
  <RouterLink class="back-link" to="/">← 返回工作台</RouterLink><p v-if="error" class="error" role="alert">{{ error }}</p><p v-if="notice" class="success" role="status">{{ notice }}</p>
  <template v-if="ticket && detail">
    <div class="toolbar"><h1>{{ ticket.requestId }}</h1><button class="secondary" :disabled="busy" @click="initial">刷新</button></div>
    <section class="card"><div class="metadata">
      <span>内部 ID：{{ ticket.id }}</span><span>状态：<span class="status-pill" :class="ticket.status"><span class="status-dot" />{{ statuses[ticket.status] }}</span></span><span>分类：{{ categories[ticket.category] }}</span>
      <span>提交分类：{{ categories[ticket.initialCategory] }}</span><span>提交者：{{ ticket.requesterName }}</span>
      <span>当前处理人：{{ ticket.assigneeName ?? '未分派' }}</span><span>创建：{{ time(ticket.createdAt) }}</span>
      <span v-if="ticket.resolutionCode">处理结论：{{ resolutions[ticket.resolutionCode] }}</span>
    </div>
    <p v-if="detail.permissionRequest">申请权限：<strong>{{ detail.permissionRequest.permissionName }}</strong> · {{ ({ PENDING: '待审批', APPROVED: '已批准', REJECTED: '已拒绝' } as Record<string, string>)[detail.permissionRequest.status] }}</p>
    <h2>反馈原文</h2><p class="prewrap">{{ ticket.originalText }}</p>
    <p v-if="ticket.relatedTicketId">关联工单：<RouterLink v-if="detail.relatedReadable" :to="`/tickets/${ticket.relatedTicketId}`">{{ ticket.relatedTicketId }}</RouterLink><span v-else>当前无权查看关联工单</span></p>
    <p v-if="ticket.status === 'COMPLETED'">本工单已完成，只读。仍有问题请新建工单并关联本工单 ID。</p>
    </section>
    <fieldset :disabled="busy || uploading" class="action-grid">
      <form v-if="has('approve')" class="card" @submit.prevent="act('approve', { resolutionCode: resolution, content: resultText, permissionCode: grantCode || null })">
        <h2>审批权限申请</h2>
        <label v-if="!detail.permissionRequest?.permissionCode">授予权限（旧申请同意时必选）<select v-model="grantCode" :required="resolution === 'APPROVED'"><option disabled value="">选择实际授予的权限</option><option v-for="permission in permissions" :key="permission.code" :value="permission.code">{{ permission.name }}</option></select></label>
        <label>审批结论<select v-model="resolution" required><option value="APPROVED">通过</option><option value="REJECTED">拒绝</option></select></label>
        <label>审批说明<textarea v-model="resultText" required maxlength="10000" rows="4" /></label>
        <p class="hint">同意后立即授予申请权限并结束申请；拒绝不会授予权限。</p><button>提交审批</button>
      </form>
      <form v-if="has('assign')" class="card" @submit.prevent="act('assign', { assigneeId, reason })"><h2>分派 / 转派</h2>
        <label>处理人<select v-model="assigneeId" required><option disabled value="">选择处理人</option><option v-for="handler in handlers" :key="handler.id" :value="handler.id">{{ handler.displayName }} · {{ handler.role === 'COORDINATOR' ? '协调员' : '技术支持' }}</option></select></label>
        <label>原因（转派必填）<textarea v-model="reason" :required="!!ticket.assigneeId && assigneeId !== ticket.assigneeId" maxlength="10000" /></label><button>保存分派</button>
      </form>
      <form v-if="has('category')" class="card" @submit.prevent="act('category', { category: selectedCategory, reason: categoryReason })"><h2>纠正分类</h2>
        <select v-model="selectedCategory" aria-label="纠正后的分类"><option v-for="(label, value) in categories" :key="value" :value="value">{{ label }}</option></select>
        <label>纠正原因<textarea v-model="categoryReason" required maxlength="10000" /></label><button>保存分类</button>
      </form>
      <section v-if="has('start')" class="card"><h2>{{ ticket.status === 'REWORK' ? '开始返工' : '开始处理' }}</h2><p>开始后进入处理中，由你提交处理结果。</p><button @click="act('start')">开始处理</button></section>
      <form v-if="has('result')" class="card" @submit.prevent="act('result', { resolutionCode: resolution, content: resultText })"><h2>提交处理结果</h2>
        <select v-model="resolution" required aria-label="处理结论"><option v-for="value in allowedResults" :key="value" :value="value">{{ resolutions[value] }}</option></select>
        <label>结果说明<textarea v-model="resultText" required maxlength="10000" rows="4" /></label>
        <p class="hint">采纳或暂缓表示评估结论，不代表承诺开发或交付日期。实际审批与开通在系统外完成。</p><button>提交并等待确认</button>
      </form>
      <section v-if="has('confirm')" class="card"><h2>确认处理结果</h2><p>查看下方处理结果后，确认本次反馈已经有明确结论。</p><button @click="act('confirm')">确认完成</button></section>
      <form v-if="has('rework')" class="card" @submit.prevent="act('rework', { reason: reworkReason })"><h2>要求返工</h2><label>仍未解决的原因<textarea v-model="reworkReason" required maxlength="10000" /></label><button class="secondary">退回继续处理</button></form>
    </fieldset>
    <section class="card"><h2>公开沟通与操作历史</h2><p class="hint">以下内容对提交者、协调员和当前技术支持可见。</p>
      <article v-for="event in detail.events" :key="event.id" class="event"><strong>{{ eventLabels[event.eventType] }} · {{ event.actorName }}</strong><small>{{ time(event.createdAt) }}</small>
        <p v-if="event.content" class="prewrap">{{ event.content }}</p><p v-if="event.changes !== '{}'" class="hint">{{ changes(event.changes) }}</p>
        <div class="gallery"><figure v-for="image in detail.attachments.filter(a => a.ticketEventId === event.id)" :key="image.id">
          <a :href="`/api/attachments/${image.id}/content`" target="_blank" rel="noopener"><img :src="`/api/attachments/${image.id}/content`" :alt="image.originalFilename" loading="lazy" /></a><figcaption>{{ image.originalFilename }}</figcaption>
        </figure></div>
      </article>
    </section>
    <form v-if="has('comment')" class="card" @submit.prevent="act('comments', { content, attachmentTokens: images.map(i => i.uploadToken) })"><h2>补充信息 / 回复</h2>
      <label>公开内容<textarea v-model="content" required rows="4" maxlength="10000" :disabled="busy" /></label>
      <ImageUpload v-if="canAttach" v-model="images" :maximum="5 - detail.attachments.length" :disabled="busy" @uploading="uploading = $event" />
      <button :disabled="busy || uploading">发布</button>
    </form>
  </template>
</template>
