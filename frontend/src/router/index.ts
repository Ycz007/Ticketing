import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginView from '../views/LoginView.vue'
import LayoutView from '../views/LayoutView.vue'
import TicketListView from '../views/TicketListView.vue'
import TicketCreateView from '../views/TicketCreateView.vue'
import TicketDetailView from '../views/TicketDetailView.vue'
import NotificationsView from '../views/NotificationsView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView },
    { path: '/', component: LayoutView, meta: { requiresAuth: true }, children: [
      { path: '', component: TicketListView },
      { path: 'tickets/new', component: TicketCreateView, meta: { permission: 'ticket:create' } },
      { path: 'tickets/:id', component: TicketDetailView },
      { path: 'notifications', component: NotificationsView },
    ] },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.initialized || to.meta.requiresAuth) {
    // Let the login page display network failures instead of trapping navigation.
    try { await auth.restore() } catch { if (to.path !== '/login') return '/login' }
  }
  if (to.meta.requiresAuth && !auth.user) return '/login'
  if (to.meta.permission && !auth.user?.authorities.includes(String(to.meta.permission))) return '/'
})

export default router
