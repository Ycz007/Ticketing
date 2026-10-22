<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import AppIcon from '../components/AppIcon.vue'

const auth = useAuthStore()
const router = useRouter()
const username = ref('')
const password = ref('')
const busy = ref(false)
const error = ref('')
async function submit() {
  busy.value = true
  error.value = ''
  try { await auth.login(username.value, password.value); await router.replace('/') }
  catch (failure) { error.value = failure instanceof Error ? failure.message : '登录失败' }
  finally { busy.value = false; password.value = '' }
}
</script>

<template>
  <main class="login">
    <div class="login-brand"><span class="brand"><span class="brand-mark"><AppIcon name="ticket" :size="18" /></span>问题登记小助手</span></div>
    <form class="card" @submit.prevent="submit">
      <div class="brand-mark login-emblem"><AppIcon name="ticket" :size="24" /></div>
      <p class="eyebrow">TEAM SUPPORT WORKSPACE</p><h1>欢迎回到工作空间</h1>
      <p class="login-intro">登录团队账号，让问题的进展一目了然。</p>
      <label>账号<input v-model="username" autocomplete="username" required maxlength="64" placeholder="输入你的团队账号" /></label>
      <label>密码<input v-model="password" type="password" autocomplete="current-password" required placeholder="输入登录密码" /></label>
      <p v-if="error" role="alert" class="error">{{ error }}</p>
      <button :disabled="busy">{{ busy ? '登录中…' : '登录工作空间' }}<AppIcon name="arrow" :size="16" /></button>
    </form>
    <p class="login-footer">企业内部支持 · 使用预置账号登录</p>
  </main>
</template>
