<script setup lang="ts">
import { ref } from 'vue'
import { api } from '../api/http'
import type { Attachment } from '../domain/ticket'

const props = withDefaults(defineProps<{ modelValue: Attachment[]; maximum?: number; disabled?: boolean }>(), { maximum: 5, disabled: false })
const emit = defineEmits<{ 'update:modelValue': [Attachment[]]; uploading: [boolean] }>()
const busy = ref(false)
const error = ref('')
async function select(event: Event) {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  error.value = ''
  if (files.length + props.modelValue.length > props.maximum) { error.value = `最多再选择 ${props.maximum - props.modelValue.length} 张图片`; input.value = ''; return }
  busy.value = true; emit('uploading', true)
  const uploaded = [...props.modelValue]
  try {
    for (const file of files) {
      if (file.size > 5 * 1024 * 1024) throw new Error('单张图片最大5 MiB')
      const body = new FormData(); body.append('file', file)
      const image = await api.post<Attachment>('/attachments', body)
      uploaded.push(image); emit('update:modelValue', [...uploaded])
    }
  } catch (failure) { error.value = failure instanceof Error ? failure.message : '上传失败' }
  finally { busy.value = false; emit('uploading', false); input.value = '' }
}
</script>

<template>
  <div class="upload">
    <label>相关图片（PNG / JPEG / WebP，单张≤5 MiB）
      <input type="file" accept="image/png,image/jpeg,image/webp" multiple :disabled="busy || disabled || modelValue.length >= maximum" @change="select" />
    </label>
    <p v-if="busy">上传中，请稍候…</p><p v-if="error" class="error" role="alert">{{ error }}</p>
    <div class="gallery"><figure v-for="image in modelValue" :key="image.id">
      <img :src="`/api/attachments/${image.id}/content`" :alt="image.originalFilename" />
      <figcaption>{{ image.originalFilename }}</figcaption>
      <button type="button" class="secondary" :disabled="busy || disabled" @click="emit('update:modelValue', modelValue.filter(i => i.id !== image.id))">移除</button>
    </figure></div>
  </div>
</template>
