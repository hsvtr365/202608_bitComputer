import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '../composables/auth'
import LoginView from '../views/LoginView.vue'
import EmployeeProfileView from '../views/EmployeeProfileView.vue'
import AdminEmployeesView from '../views/AdminEmployeesView.vue'
import AdminEmployeeFormView from '../views/AdminEmployeeFormView.vue'
import AdminEmployeeDetailView from '../views/AdminEmployeeDetailView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', component: LoginView, meta: { public: true } },
    { path: '/employee/profile', component: EmployeeProfileView },
    { path: '/admin/employees', component: AdminEmployeesView, meta: { admin: true } },
    { path: '/admin/employees/new', component: AdminEmployeeFormView, meta: { admin: true } },
    { path: '/admin/employees/:id', component: AdminEmployeeDetailView, meta: { admin: true } },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuth()
  if (!auth.loaded.value) await auth.load()
  if (to.meta.public) {
    if (to.path === '/login' && auth.user.value) {
      return auth.isAdmin.value ? '/admin/employees' : '/employee/profile'
    }
    return true
  }
  if (!auth.user.value) return '/login'
  if (to.meta.admin && !auth.isAdmin.value) return '/employee/profile'
  return true
})

export default router
