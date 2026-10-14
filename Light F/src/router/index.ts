import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('@/views/Register.vue') },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/Home.vue')
  },
  {
    path: '/module/:id',
    name: 'ModuleDetail',
    component: () => import('@/views/ModuleDetail.vue')
  },
  {
    path: '/entry/:id',
    name: 'EntryDetail',
    component: () => import('@/views/EntryDetail.vue')
  },
  {
    path: '/entry/edit/:moduleId',
    name: 'EntryCreate',
    component: () => import('@/views/EntryEdit.vue')
  },
  {
    path: '/entry/edit/:moduleId/:id',
    name: 'EntryEdit',
    component: () => import('@/views/EntryEdit.vue')
  },
  {
    path: '/music',
    name: 'MusicBox',
    component: () => import('@/views/MusicBox.vue')
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue')
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/views/Search.vue')
  },
  {
    path: '/coming-soon',
    name: 'ComingSoon',
    component: () => import('@/views/ComingSoon.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('light_access_token')
  if (to.name !== 'Login' && to.name !== 'Register' && !token) return '/login'
  if ((to.name === 'Login' || to.name === 'Register') && token) return '/'
})

export default router
