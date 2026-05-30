import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/portal'
  },
  {
    path: '/portal',
    name: 'Portal',
    component: () => import('@/views/PortalPage.vue'),
    meta: { title: '系统百科门户' }
  },
  {
    path: '/space/:systemId',
    component: () => import('@/views/space/SpaceLayout.vue'),
    children: [
      { path: '', redirect: { name: 'SpaceIntro' } },
      {
        path: 'intro',
        name: 'SpaceIntro',
        component: () => import('@/views/space/IntroPage.vue'),
        meta: { title: '系统简介' }
      },
      {
        path: 'arch',
        name: 'SpaceArch',
        component: () => import('@/views/space/ArchPage.vue'),
        meta: { title: '环境架构' }
      },
      {
        path: 'server',
        name: 'SpaceServer',
        component: () => import('@/views/space/ServerPage.vue'),
        meta: { title: '服务器配置' }
      },
      {
        path: 'guide',
        name: 'SpaceGuide',
        component: () => import('@/views/space/GuidePage.vue'),
        meta: { title: '接入指南' }
      },
      {
        path: 'sql-lib',
        name: 'SpaceSqlLib',
        component: () => import('@/views/space/SqlLibPage.vue'),
        meta: { title: '运维SQL库' }
      },
      {
        path: 'topology',
        name: 'SpaceTopology',
        component: () => import('@/views/space/TopologyPage.vue'),
        meta: { title: '黄金链路' }
      },
      {
        path: 'ai-chat',
        name: 'SpaceAiChat',
        component: () => import('@/views/space/AiChatPage.vue'),
        meta: { title: 'AI问答' }
      },
      {
        path: 'edit/:moduleType?',
        name: 'SpaceEdit',
        component: () => import('@/views/space/EditPage.vue'),
        meta: { title: '内容编辑' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const title = to.meta.title as string
  if (title) {
    document.title = `${title} - 系统百科`
  }
  next()
})

export default router
