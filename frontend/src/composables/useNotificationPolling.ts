import { onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { type Me, useAuthStore } from '../stores/auth'
import { api, ApiError } from '../api/http'

export function useNotificationPolling() {
  const count = ref(0)
  const error = ref('')
  const auth = useAuthStore()
  const router = useRouter()
  let timer: ReturnType<typeof setTimeout> | undefined
  let pending: AbortController | undefined
  let stopped = false
  let retryMs = 15000

  async function refresh() {
    clearTimeout(timer)
    if (stopped || pending || document.hidden || !auth.user) return
    const controller = new AbortController()
    pending = controller
    try {
      const me = await api.get<Me>('/me', controller.signal)
      if (!controller.signal.aborted && auth.user && JSON.stringify(me.authorities) !== JSON.stringify(auth.user.authorities)) auth.user = me
      const result = await api.get<{ count: number }>('/notifications/unread-count', controller.signal)
      if (!controller.signal.aborted) { count.value = result.count; error.value = ''; retryMs = 15000 }
    } catch (failure) {
      if (controller.signal.aborted) return
      if (failure instanceof ApiError && failure.status === 401) {
        auth.user = null
        count.value = 0
        await router.replace('/login')
      } else {
        error.value = '通知刷新失败，将自动重试'
        retryMs = Math.min(retryMs * 2, 60000)
      }
    } finally {
      pending = undefined
      if (!stopped && auth.user && !document.hidden) timer = setTimeout(refresh, retryMs)
    }
  }

  function visibilityChanged() {
    if (document.hidden) { clearTimeout(timer); pending?.abort() }
    else void refresh()
  }

  watch(() => auth.user, () => {
    clearTimeout(timer)
    if (!auth.user) { pending?.abort(); count.value = 0; error.value = '' }
    else void refresh()
  })

  onMounted(() => {
    document.addEventListener('visibilitychange', visibilityChanged)
    window.addEventListener('online', refresh)
    window.addEventListener('notifications-changed', refresh)
    void refresh()
  })
  onUnmounted(() => {
    stopped = true
    clearTimeout(timer)
    pending?.abort()
    document.removeEventListener('visibilitychange', visibilityChanged)
    window.removeEventListener('online', refresh)
    window.removeEventListener('notifications-changed', refresh)
  })
  return { count, error, refresh }
}
