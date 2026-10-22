<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useNotificationPolling } from '../composables/useNotificationPolling'
import AppIcon from '../components/AppIcon.vue'
const auth = useAuthStore()
const router = useRouter()
const { count, error } = useNotificationPolling()
const logoutError = ref('')
const roleNames: Record<string, string> = { ROLE_REQUESTER: '提交者', ROLE_COORDINATOR: '协调员', ROLE_SUPPORT: '技术支持' }
const role = computed(() => auth.user?.authorities.filter(a => a.startsWith('ROLE_')).map(a => roleNames[a] ?? a).join('、'))
async function logout() {
  try { await auth.logout(); await router.replace('/login') } catch { logoutError.value = '退出失败，请重试' }
}
</script>
<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="header-inner">
        <RouterLink class="brand" to="/"><span class="brand-mark"><AppIcon name="ticket" :size="20" /></span><span>问题登记小助手</span></RouterLink>
        <span class="workspace-divider">/</span><span class="workspace-name">团队工作空间 <span class="plan-badge">Team</span></span>
        <div class="identity">
          <RouterLink class="notification icon-button" to="/notifications" aria-label="通知"><AppIcon name="bell" /><span v-if="count > 0" class="badge">{{ count > 99 ? '99+' : count }}</span></RouterLink>
          <span class="user-avatar">{{ auth.user?.displayName?.slice(0, 1) }}</span><span class="user-info">{{ auth.user?.displayName }}<small>{{ role }}</small></span>
          <button class="icon-button logout-button" aria-label="退出登录" title="退出登录" @click="logout"><AppIcon name="logout" /></button>
        </div>
      </div>
      <nav class="main-nav" aria-label="主导航">
        <RouterLink to="/" :class="{ active: $route.path === '/' || $route.path.startsWith('/tickets/') }"><AppIcon name="grid" :size="16" />工单工作台</RouterLink>
        <RouterLink to="/notifications" :class="{ active: $route.path === '/notifications' }"><AppIcon name="bell" :size="16" />通知中心<span v-if="count > 0" class="nav-count">{{ count }}</span></RouterLink>
      </nav>
    </header>
    <main class="workspace">
      <p v-if="error || logoutError" class="error" role="alert">{{ error || logoutError }}</p>
      <RouterView />
    </main>
    <footer class="app-footer"><span class="footer-brand"><AppIcon name="ticket" :size="14" />问题登记小助手</span><span>让每一个问题，都有清晰的进展。</span></footer>
  </div>
</template>
