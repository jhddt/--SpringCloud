import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/StudentLayout.vue'),
    meta: { requiresAuth: true, role: 'STUDENT' },
    children: [
      {
        path: '',
        redirect: '/dashboard'
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue')
      },
      {
        path: 'course-browse',
        name: 'CourseBrowse',
        component: () => import('@/views/CourseBrowse.vue')
      },
      {
        path: 'my-selection',
        name: 'MySelection',
        component: () => import('@/views/MySelection.vue')
      },
      {
        path: 'grade-query',
        name: 'GradeQuery',
        component: () => import('@/views/GradeQuery.vue')
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue')
      },
      {
        path: 'message-center',
        name: 'MessageCenter',
        component: () => import('@/views/MessageCenter.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  if (to.meta.requiresAuth) {
    if (!userStore.token) {
      next('/login')
    } else if (to.meta.role && userStore.role !== 'STUDENT') {
      ElMessage.error('无权限访问学生端')
      next('/login')
    } else {
      next()
    }
  } else {
    if (to.path === '/login' && userStore.token && userStore.role === 'STUDENT') {
      next('/')
    } else {
      next()
    }
  }
})

export default router

