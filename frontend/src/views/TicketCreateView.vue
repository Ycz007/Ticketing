<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api/http'
import { categories, notificationChanged, type Attachment, type Ticket } from '../domain/ticket'
import ImageUpload from '../components/ImageUpload.vue'
const router = useRouter()
const requestId = ref(crypto.randomUUID()), originalText = ref(''), category = ref(''), relatedTicketId = ref('')
const permissionName = ref('')
const permissions = ref<{ code: string; name: string }[]>([])
onMounted(async () => {
  try { permissions.value = await api.get('/permissions') }
  catch (failure) { error.value = failure instanceof Error ? failure.message : '权限选项加载失败' }
})
const images = ref<Attachment[]>([]), uploading = ref(false), busy = ref(false), error = ref('')
async function submit() {
  busy.value = true; error.value = ''
  try {
    const ticket = await api.json<Ticket>('/tickets', { requestId: requestId.value, originalText: originalText.value, category: category.value,
      permissionName: category.value === 'ACCESS_REQUEST' ? permissionName.value : null,
      relatedTicketId: relatedTicketId.value || null, attachmentTokens: images.value.map(image => image.uploadToken) })
    notificationChanged(); await router.push(`/tickets/${ticket.id}`)
  } catch (failure) { error.value = failure instanceof Error ? failure.message : '提交失败' }
  finally { busy.value = false }
}
</script>
<template>
  <div class="form-page">
  <RouterLink class="back-link" to="/">← 返回工作台</RouterLink>
  <div class="page-heading"><div><p class="eyebrow">WORKSPACE / NEW TICKET</p><h1>提交工单</h1><p>描述你的问题，我们一起推动解决。</p></div></div>
  <form class="card" @submit.prevent="submit">
    <div class="form-section-title"><h2>工单信息</h2><p>清晰的问题描述与相关截图，有助于更快定位和处理。</p></div>
    <label>唯一提交编号<input v-model="requestId" pattern="[A-Za-z0-9_-]{1,64}" maxlength="64" required :disabled="busy" /></label>
    <p class="hint">网络失败重试时保留编号与内容；修改内容后需使用新编号。</p>
    <label>分类<select v-model="category" required :disabled="busy"><option disabled value="">请选择，不确定时选“待确认”</option>
      <option v-for="(label, value) in categories" :key="value" :value="value">{{ label }}</option></select></label>
    <template v-if="category === 'ACCESS_REQUEST'">
      <label>申请权限<select v-model="permissionName" required :disabled="busy"><option disabled value="">请选择需要开通的权限</option><option v-for="permission in permissions" :key="permission.code" :value="permission.code">{{ permission.name }}（{{ permission.code }}）</option></select></label>
      <p class="hint">请在下方填写申请原因。协调员同意后立即授予所选权限；拒绝不授权，审批后申请结束。</p>
    </template>
    <label>{{ category === 'ACCESS_REQUEST' ? '申请原因' : '反馈原文' }}<textarea v-model="originalText" rows="6" maxlength="10000" required :disabled="busy" placeholder="描述遇到的问题、期望功能或咨询内容" /></label>
    <label>关联已完成工单的内部 ID（可选）<input v-model="relatedTicketId" inputmode="numeric" pattern="[1-9][0-9]*" :disabled="busy" /></label>
    <ImageUpload v-model="images" :disabled="busy" @uploading="uploading = $event" />
    <p v-if="error" class="error" role="alert">{{ error }}</p><div class="form-footer"><RouterLink class="back-link" to="/">取消</RouterLink><button :disabled="busy || uploading">{{ busy ? '提交中…' : '提交工单' }}</button></div>
  </form>
  </div>
</template>
