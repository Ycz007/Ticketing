<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '../api/http'
import { time, notificationLabels, type Notification } from '../domain/ticket'
const items = ref<Notification[]>([]), page = ref(1), error = ref(''), busy = ref(false)
async function load(delta = 0) {
  page.value += delta; busy.value = true; error.value = ''
  try { items.value = await api.get(`/notifications?page=${page.value}`) }
  catch (failure) { error.value = failure instanceof Error ? failure.message : '加载失败' }
  finally { busy.value = false }
}
onMounted(() => load())
</script>
<template><RouterLink class="back-link" to="/">← 返回工作台</RouterLink><div class="page-heading"><div><p class="eyebrow">WORKSPACE / INBOX</p><h1>通知中心</h1><p>掌握工单的最新动态。打开详情后，对应通知会自动标为已读。</p></div></div>
  <p v-if="error" class="error" role="alert">{{ error }}</p>
  <section class="card"><article v-for="item in items" :key="item.id" class="event">
    <RouterLink :to="{ path: `/tickets/${item.ticketId}`, query: { notification: item.id } }">{{ item.readAt ? '' : '● ' }}{{ notificationLabels[item.type] }} · {{ item.requestId }}</RouterLink>
    <small>{{ time(item.createdAt) }}</small>
  </article><div v-if="!items.length" class="empty-state"><h3>{{ busy ? '正在加载通知…' : error ? '通知加载失败' : '暂时没有新通知' }}</h3><p>{{ error ? '请刷新页面重试。' : '工单有新进展时，会在这里通知你。' }}</p></div></section>
  <div class="toolbar"><button class="secondary" :disabled="busy || page <= 1" @click="load(-1)">上一页</button><span>第 {{ page }} 页</span><button class="secondary" :disabled="busy || items.length < 20" @click="load(1)">下一页</button></div>
</template>
