<template>
  <Teleport to="body">
    <transition
      @enter="onEnter"
      @after-enter="onAfterEnter"
      @leave="onLeave"
      @after-leave="onAfterLeave"
    >
      <div v-if="open" class="fullscreen-menu" :class="{ leaving: isLeaving }">
        <!-- 点击遮罩区域关闭 -->
        <div class="menu-backdrop" @click="emit('close')"></div>

        <div class="menu-panel">
          <!-- 用户卡 -->
          <div class="user-card" @click="goPath('/profile')">
            <div class="uc-avatar">{{ avatarText }}</div>
            <div class="uc-info">
              <span class="uc-name">{{ displayName }}</span>
              <span class="uc-sign">{{ signature }}</span>
            </div>
            <svg class="uc-arrow" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </div>

          <!-- 导航菜单 -->
          <nav class="menu-inner">
            <button
              v-for="(item, i) in navItems"
              :key="item.name"
              class="menu-item"
              :class="{ active: item.active, cta: item.cta, visible: itemsVisible }"
              :style="{ '--i': i }"
              @click="onNavClick(item)"
            >
              <span class="item-icon" v-html="item.icon"></span>
              <span class="item-label">{{ item.name }}</span>
              <span v-if="item.badge" class="item-badge">{{ item.badge }}</span>
            </button>
          </nav>

          <!-- 底部登出 -->
          <div class="menu-footer">
            <button class="logout-item" @click="onLogout">
              <span class="item-icon" v-html="logoutIcon"></span>
              <span class="item-label">退出登录</span>
            </button>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { identityApi } from '@/api'

defineProps<{ open: boolean }>()
const emit = defineEmits<{ (e: 'close'): void }>()

const router = useRouter()
const route = useRoute()
const appStore = useAppStore()
const userStore = useUserStore()
const itemsVisible = ref(false)
const isLeaving = ref(false)

// 线性图标（stroke=currentColor，随文字色变化）
const iconHome = `<svg viewBox="0 0 24 24" width="21" height="21" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 10.5 12 3l9 7.5"/><path d="M5 9.5V21h14V9.5"/><path d="M9 21v-6h6v6"/></svg>`
const iconMusic = `<svg viewBox="0 0 24 24" width="21" height="21" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18V6l11-2v12"/><circle cx="6" cy="18" r="3"/><circle cx="17" cy="16" r="3"/></svg>`
const iconUser = `<svg viewBox="0 0 24 24" width="21" height="21" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 3.6-6.5 8-6.5s8 2.5 8 6.5"/></svg>`
const iconPlus = `<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round"><path d="M12 5v14M5 12h14"/></svg>`
const logoutIcon = `<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3"/><polyline points="10 17 15 12 10 7"/><line x1="15" y1="12" x2="3" y2="12"/></svg>`

interface NavItem {
  name: string
  icon: string
  path?: string
  cta?: boolean
  active: boolean
  badge?: number
}

const navItems = computed<NavItem[]>(() => [
  { name: '首页', icon: iconHome, path: '/', active: route.name === 'Home' },
  { name: '音乐盒', icon: iconMusic, path: '/music', active: route.name === 'MusicBox' },
  { name: '个人中心', icon: iconUser, path: '/profile', active: route.name === 'Profile' },
  { name: '创建模块', icon: iconPlus, path: '/', cta: true, active: false }
])

const displayName = computed(() => {
  const local = localStorage.getItem('vl-profile')
  if (local) {
    try {
      const n = JSON.parse(local).name
      if (n) return n
    } catch { /* ignore */ }
  }
  return userStore.user?.nickname || userStore.user?.username || '学习者'
})
const signature = computed(() => {
  const local = localStorage.getItem('vl-profile')
  if (local) {
    try {
      const s = JSON.parse(local).signature
      if (s) return s
    } catch { /* ignore */ }
  }
  return '@' + (userStore.user?.username || 'learner')
})
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase())

const onEnter = (_el: unknown, done: () => void) => {
  isLeaving.value = false
  itemsVisible.value = false
  userStore.fetchMe()
  requestAnimationFrame(() => {
    itemsVisible.value = true
    const totalDelay = navItems.value.length * 45 + 260
    setTimeout(done, totalDelay)
  })
}
const onAfterEnter = () => { /* */ }
const onLeave = (_el: unknown, done: () => void) => {
  isLeaving.value = true
  itemsVisible.value = false
  setTimeout(done, 220)
}
const onAfterLeave = () => { /* */ }

const goPath = (path: string) => {
  emit('close')
  router.push(path)
}

const onNavClick = (item: NavItem) => {
  emit('close')
  if (item.cta) {
    router.push(item.path || '/')
    appStore.bumpCreate()
  } else if (item.path) {
    router.push(item.path)
  }
}

const onLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '退出登录', {
      type: 'warning',
      confirmButtonText: '退出',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    if (localStorage.getItem('light_refresh_token')) await identityApi.logout()
  } catch { /* 允许本地登出 */ }
  localStorage.removeItem('light_access_token')
  localStorage.removeItem('light_refresh_token')
  userStore.reset()
  emit('close')
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped lang="scss">
// ============ 容器 ============
.fullscreen-menu {
  position: fixed;
  inset: 0;
  z-index: 200;
}

.menu-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(13, 12, 34, 0.32);
  backdrop-filter: blur(2px);
}

.menu-panel {
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
  width: 320px;
  max-width: 84vw;
  background: #fff;
  border-right: 1px solid $border-soft;
  box-shadow: $shadow-modal;
  display: flex;
  flex-direction: column;
  transform: translateX(-104%);
  transition: transform 0.26s cubic-bezier(0.22, 1, 0.36, 1);
  overflow-y: auto;
}
.fullscreen-menu:not(.leaving) .menu-panel {
  transform: translateX(0);
}

// ============ 用户卡 ============
.user-card {
  display: flex;
  align-items: center;
  gap: 13px;
  padding: 22px 20px 20px;
  margin: 14px 14px 8px;
  border-radius: $radius-md;
  background: linear-gradient(135deg, rgba(234, 76, 137, 0.08), rgba(124, 92, 255, 0.08));
  cursor: pointer;
  transition: background 0.15s;

  &:hover {
    background: linear-gradient(135deg, rgba(234, 76, 137, 0.13), rgba(124, 92, 255, 0.13));
  }
}

.uc-avatar {
  flex-shrink: 0;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: linear-gradient(135deg, $primary, #7c5cff);
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.uc-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.uc-name {
  font-size: 15px;
  font-weight: 700;
  color: $ink;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.uc-sign {
  font-size: 12.5px;
  color: $muted;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.uc-arrow {
  color: $faint;
  flex-shrink: 0;
}

// ============ 菜单 ============
.menu-inner {
  flex: 1;
  padding: 8px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 13px;
  width: 100%;
  padding: 12px 14px;
  border: none;
  border-radius: $radius-md;
  background: transparent;
  font-family: inherit;
  font-size: 15px;
  font-weight: 600;
  color: $ink-soft;
  text-align: left;
  cursor: pointer;
  opacity: 0;
  transform: translateX(-12px);
  transition:
    opacity 0.24s cubic-bezier(0.22, 1, 0.36, 1),
    transform 0.24s cubic-bezier(0.22, 1, 0.36, 1),
    background 0.12s,
    color 0.12s;
  transition-delay: calc(var(--i, 0) * 45ms + 80ms);

  .item-icon {
    display: inline-flex;
    color: $faint;
    transition: color 0.12s;
  }

  &.visible {
    opacity: 1;
    transform: translateX(0);
  }

  &:hover {
    background: $bg-subtle;
    color: $ink;
    .item-icon { color: $primary; }
  }

  &.active {
    background: rgba(234, 76, 137, 0.1);
    color: $primary;
    .item-icon { color: $primary; }
  }

  &.cta {
    margin-top: 6px;
    background: $ink;
    color: #fff;
    justify-content: center;

    .item-icon { color: #fff; }
    &:hover {
      background: #3d3d4e;
      color: #fff;
      .item-icon { color: #fff; }
    }
  }
}

.item-badge {
  margin-left: auto;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: $radius-pill;
  background: $primary;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

// ============ 底部 ============
.menu-footer {
  padding: 10px 14px 20px;
  border-top: 1px solid $border-soft;
}

.logout-item {
  display: flex;
  align-items: center;
  gap: 13px;
  width: 100%;
  padding: 12px 14px;
  border: none;
  border-radius: $radius-md;
  background: transparent;
  font-family: inherit;
  font-size: 14.5px;
  font-weight: 600;
  color: $danger;
  cursor: pointer;
  text-align: left;
  transition: background 0.12s;

  .item-icon { color: $danger; opacity: 0.85; }
  &:hover { background: rgba(239, 68, 68, 0.07); }
}

// 收起时菜单项立即淡出
.fullscreen-menu.leaving .menu-item {
  opacity: 0;
  transform: translateX(-12px);
  transition-duration: 0.14s;
  transition-delay: 0s;
}

// ============ 暗色 ============
:global(.dark) .menu-panel {
  background: $dark-card;
  border-right-color: $dark-border;
}
:global(.dark) .menu-item {
  color: $dark-ink;
  &:hover { background: $dark-border; color: $dark-ink; }
}
:global(.dark) .uc-sign,
:global(.dark) .uc-arrow {
  color: $dark-muted;
}
:global(.dark) .menu-footer {
  border-top-color: $dark-border;
}
</style>
