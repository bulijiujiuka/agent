import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '工作台', icon: 'HomeFilled' }
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('@/views/knowledge/index.vue'),
        meta: { title: '知识库管理', icon: 'FolderOpened' }
      },
      {
        path: 'knowledge/category',
        name: 'KbCategory',
        component: () => import('@/views/knowledge/category.vue'),
        meta: { title: '分类管理', icon: 'Collection' }
      },
      {
        path: 'ai/chat',
        name: 'AiChat',
        component: () => import('@/views/ai/chat/index.vue'),
        meta: { title: 'AI 问答', icon: 'ChatDotRound' }
      },
      {
        path: 'ai/prompt',
        name: 'AiPrompt',
        component: () => import('@/views/ai/prompt/index.vue'),
        meta: { title: 'Prompt 配置', icon: 'EditPen' }
      },
      {
        path: 'ai/model',
        name: 'AiModel',
        component: () => import('@/views/ai/model/index.vue'),
        meta: { title: '模型管理', icon: 'Cpu' }
      },
      {
        path: 'ai/stats',
        name: 'AiStats',
        component: () => import('@/views/ai/stats/index.vue'),
        meta: { title: '评测看板', icon: 'DataAnalysis' }
      },
      {
        path: 'ai/audit',
        name: 'AiAudit',
        component: () => import('@/views/ai/audit/index.vue'),
        meta: { title: '会话审计', icon: 'View' }
      },
      {
        path: 'ai/observability',
        name: 'AiObservability',
        component: () => import('@/views/ai/observability/index.vue'),
        meta: { title: '可观测性', icon: 'Monitor' }
      },
      {
        path: 'ticket',
        name: 'Ticket',
        component: () => import('@/views/ticket/index.vue'),
        meta: { title: '工单辅助', icon: 'Tickets' }
      },
      {
        path: 'system/user',
        name: 'User',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'system/role',
        name: 'Role',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理', icon: 'UserFilled' }
      },
      {
        path: 'system/file',
        name: 'File',
        component: () => import('@/views/system/file/index.vue'),
        meta: { title: '文件管理', icon: 'FolderOpened' }
      },
      {
        path: 'system/log',
        name: 'OperLog',
        component: () => import('@/views/system/log/index.vue'),
        meta: { title: '操作日志', icon: 'Document' }
      },
      {
        path: 'system/sensitive',
        name: 'SensitiveWord',
        component: () => import('@/views/system/sensitive/index.vue'),
        meta: { title: '敏感词管理', icon: 'Warning' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', icon: 'UserFilled', hidden: true }
      },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

NProgress.configure({ showSpinner: false })

router.beforeEach((to, from, next) => {
  NProgress.start()
  const userStore = useUserStore()
  
  if (to.path === '/login') {
    if (userStore.isLoggedIn) {
      next('/')
    } else {
      next()
    }
  } else {
    if (userStore.isLoggedIn) {
      next()
    } else {
      next('/login')
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})

export default router
