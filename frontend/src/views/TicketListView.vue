<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import AppIcon from '../components/AppIcon.vue'
import { useAuthStore } from '../stores/auth'
import { api } from '../api/http'
import { categories, statuses, time, type Ticket } from '../domain/ticket'
const auth = useAuthStore()
const items = ref<Ticket[]>([]), counts = ref<Record<string, number>>({})
const page = ref(1), total = ref(0), status = ref(''), category = ref(''), requestId = ref(''), unassigned = ref(false)
const busy = ref(false), error = ref('')
const summary = computed(() => [
  { key: 'PENDING', label: '待处理', icon: 'ticket', hint: '等待受理与分派' },
  { key: 'IN_PROGRESS', label: '处理中', icon: 'clock', hint: '正在推进解决' },
  { key: 'AWAITING_CONFIRMATION', label: '待确认完成', icon: 'bell', hint: '等待提交者确认' },
  { key: 'COMPLETED', label: '已完成', icon: 'check', hint: '问题已闭环' },
].map(item => ({ ...item, value: counts.value[item.key] ?? 0 })))
const has = (permission: string) => auth.user?.authorities.includes(permission)
async function load(reset = false) {
  if (reset) page.value = 1
  busy.value = true; error.value = ''
  try {
    await auth.restore()
    const query = new URLSearchParams({ page: String(page.value), size: '20', status: status.value, category: category.value,
      requestId: requestId.value, unassigned: String(unassigned.value) })
    const result = await api.get<{ items: Ticket[]; total: number }>(`/tickets?${query}`)
    items.value = result.items; total.value = result.total
    counts.value = await api.get<Record<string, number>>('/workbench/counts')
  } catch (failure) { error.value = failure instanceof Error ? failure.message : '加载失败' }
  finally { busy.value = false }
}
async function next(delta: number) { page.value += delta; await load() }
onMounted(() => load())
</script>
<template>
  <div class="page-heading"><div><p class="eyebrow">WORKSPACE / OVERVIEW</p><h1>工单工作台<span class="heading-dot">.</span></h1><p>集中管理团队反馈，跟进每一个问题的解决进度。</p></div><RouterLink v-if="has('ticket:create')" class="button" to="/tickets/new"><AppIcon name="plus" :size="16" />提交工单</RouterLink></div>
  <div class="stats-grid" :aria-busy="busy">
    <div v-for="item in summary" :key="item.key" class="stat-card"><div class="stat-label">{{ item.label }}<AppIcon :name="item.icon" :size="17" /></div><strong>{{ busy ? '—' : error ? '—' : item.value }}</strong><div class="stat-hint"><span class="status-dot" :class="item.key" />{{ item.hint }}</div></div>
  </div>
  <div class="section-heading"><h2>全部工单 <span class="total-badge">{{ total }}</span></h2><div class="overview-extra"><span v-if="counts.REWORK !== undefined">返工 <strong>{{ busy || error ? '—' : counts.REWORK }}</strong></span><span v-if="counts.UNASSIGNED !== undefined">未分派 <strong>{{ busy || error ? '—' : counts.UNASSIGNED }}</strong></span></div></div>
  <form class="filters" @submit.prevent="load(true)">
    <label class="search-field"><span class="sr-only">提交编号</span><AppIcon name="search" :size="17" /><input v-model="requestId" maxlength="64" placeholder="输入完整提交编号查询…" /></label>
    <label><span class="sr-only">状态</span><select v-model="status" aria-label="工单状态"><option value="">全部状态</option><option v-for="(label, value) in statuses" :key="value" :value="value">{{ label }}</option></select></label>
    <label><span class="sr-only">类型</span><select v-model="category" aria-label="工单类型"><option value="">全部类型</option><option v-for="(label, value) in categories" :key="value" :value="value">{{ label }}</option></select></label>
    <label v-if="has('ticket:assign')" class="check"><input v-model="unassigned" type="checkbox" />仅未分派</label>
    <button class="secondary refresh-button" :disabled="busy"><AppIcon name="refresh" :size="15" :class="{ spinning: busy }" />{{ busy ? '加载中' : '查询 / 刷新' }}</button>
  </form>
  <p v-if="error" role="alert" class="error">{{ error }}</p>
  <div class="table-wrap" :aria-busy="busy"><table><thead><tr><th>工单 / 内容</th><th>类型</th><th>状态</th><th>提交人</th><th>当前处理人</th><th>创建时间</th></tr></thead>
    <tbody><tr v-for="ticket in items" :key="ticket.id">
      <td><RouterLink class="ticket-link" :to="`/tickets/${ticket.id}`">{{ ticket.requestId }}<AppIcon name="arrow" :size="13" /></RouterLink><p class="snippet">{{ ticket.originalText.slice(0, 80) }}</p></td>
      <td><span class="category-tag">{{ categories[ticket.category] }}</span></td><td><span class="status-pill" :class="ticket.status"><span class="status-dot" />{{ statuses[ticket.status] }}</span></td><td>{{ ticket.requesterName }}</td>
      <td :class="{ muted: !ticket.assigneeName }">{{ ticket.assigneeName ?? '未分派' }}</td><td class="time-cell">{{ time(ticket.createdAt) }}</td>
    </tr><tr v-if="!items.length"><td colspan="6"><div class="empty-state"><span class="empty-icon"><AppIcon :name="busy ? 'refresh' : 'ticket'" :size="26" :class="{ spinning: busy }" /></span><h3>{{ busy ? '正在加载工单' : error ? '工单加载失败' : '暂无符合条件的工单' }}</h3><p>{{ busy ? '正在获取最新的工作进展…' : error ? '请检查连接后，点击查询 / 刷新重试。' : '试试调整筛选条件，或提交一张新工单。' }}</p><RouterLink v-if="!busy && !error && has('ticket:create')" class="empty-link" to="/tickets/new">创建第一张工单 <AppIcon name="arrow" :size="14" /></RouterLink></div></td></tr></tbody>
  </table></div>
  <div class="toolbar pagination"><span>共 {{ total }} 条记录<span class="pagination-separator">/</span>第 {{ page }} 页</span><div class="actions">
    <button class="secondary" :disabled="busy || page <= 1" @click="next(-1)">上一页</button>
    <button class="secondary" :disabled="busy || page * 20 >= total" @click="next(1)">下一页</button>
  </div></div>
</template>
