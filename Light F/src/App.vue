<template>
  <div id="app-root">
    <header class="vl-header">
      <div class="header-inner">
        <div class="header-left">
          <button class="menu-btn" @click="drawerOpen = !drawerOpen" aria-label="菜单">
            <transition name="icon-fade" mode="out-in">
              <svg v-if="!drawerOpen" key="burger" width="22" height="22" viewBox="0 0 24 24" fill="none"
                stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                <line x1="3" y1="6" x2="21" y2="6" />
                <line x1="3" y1="12" x2="21" y2="12" />
                <line x1="3" y1="18" x2="21" y2="18" />
              </svg>
              <svg v-else key="close" width="22" height="22" viewBox="0 0 24 24" fill="none"
                stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
                <line x1="6" y1="6" x2="18" y2="18" />
                <line x1="6" y1="18" x2="18" y2="6" />
              </svg>
            </transition>
          </button>
          <div class="logo" @click="router.push('/')">
            <span class="logo-text">Light</span>
          </div>
        </div>

        <div class="search-block">
          <div class="search-wrapper" :class="{ focused: searchFocused }">
            <input
              ref="searchInput"
              v-model="searchKeyword"
              class="search-input"
              placeholder="搜索学习条目…"
              @focus="onSearchFocus"
              @blur="onSearchBlur"
              @keydown.enter="doSearch"
            />
          </div>
        </div>

        <div class="header-right">
          <button class="icon-btn wallpaper-btn" @click="goWallpaper" title="主题壁纸">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"
              stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="3" width="18" height="18" rx="2" />
              <circle cx="8.5" cy="8.5" r="1.5" />
              <polyline points="21 15 16 10 5 21" />
            </svg>
          </button>
        </div>
      </div>
    </header>

    <main class="vl-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <SidebarDrawer :open="drawerOpen" @close="drawerOpen = false" />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import SidebarDrawer from '@/components/SidebarDrawer.vue'
import { useStudyStore } from '@/stores/study'
import { applyWallpaper, wallpapers, pickWallpaper, wallIndex } from '@/config/wallpapers'

const router = useRouter()
const route = useRoute()

// ============ 学习时长记录 ============
const study = useStudyStore()

// 进入模块/条目页时记录上下文，用于按模块统计
watch(() => route.fullPath, () => {
  drawerOpen.value = false
  if (route.name === 'ModuleDetail') study.setModule(String(route.params.id))
  else if (route.name === 'EntryDetail' || route.name === 'EntryEdit' || route.name === 'EntryCreate') {
    // entry 路由里 moduleId 是第一个参数
    const mid = route.params.moduleId || route.params.id
    study.setModule(mid ? String(mid) : '')
  } else study.setModule('')
})

// ============ 顶部下拉面板 ============
const drawerOpen = ref(false)

watch(() => route.fullPath, () => {
  drawerOpen.value = false
})

const onKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') drawerOpen.value = false
}

// ============ 搜索 ============
const searchInput = ref<HTMLInputElement | null>(null)
const searchFocused = ref(false)
const searchKeyword = ref('')

const onSearchFocus = () => { searchFocused.value = true }
const onSearchBlur = () => { searchFocused.value = false }

const doSearch = () => {
  const q = searchKeyword.value.trim()
  if (!q) { searchInput.value?.focus(); return }
  router.push({ path: '/search', query: { q } })
  searchInput.value?.blur()
}

// ============ 壁纸（导航栏按钮直接循环切换） ============
const goWallpaper = () => {
  // 在 -1(默认) 和 0..n 之间循环
  const next = wallIndex.value < wallpapers.length - 1 ? wallIndex.value + 1 : -1
  pickWallpaper(next)
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
  study.start()
  applyWallpaper()
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped lang="scss">
#app-root {
  min-height: 100vh;
  transition: background 0.3s;
  background: var(--vl-wallpaper, transparent);
  background-attachment: fixed;
}

.vl-header {
  position: sticky;
  top: 0;
  z-index: 100;
  height: var(--vl-header-h, 83px);
  background: $bg-card;
  border-bottom: 1px solid $border-soft;
  transition: background-color 0.3s ease, border-color 0.3s ease;
}

.header-inner {
  max-width: 1440px;
  height: 100%;
  margin: 0 auto;
  padding: 0 32px;
  display: flex;
  align-items: center;
  gap: 28px;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.menu-btn {
  width: 52px;
  height: 52px;
  border: none;
  background: transparent;
  border-radius: $radius-md;
  font-size: 26px;
  color: $ink;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color $dur-fast, color $dur-fast;

  &:hover {
    background: $bg-subtle;
    color: $primary;
  }
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}
.logo-text {
  font-size: 34px;
  font-weight: 900;
  font-style: italic;
  font-family: Georgia, 'Times New Roman', serif;
  letter-spacing: -0.03em;
  background: linear-gradient(135deg, $primary, #7c3aed);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  line-height: 1;
}

// ============ 搜索（Dribbble 风格） ============
.search-block {
  flex: 1;
  max-width: 640px;
  min-width: 0;
  position: relative;
  display: flex;
  align-items: center;
  gap: 13px;
}

.search-wrapper {
  flex: 1;
  min-width: 0;
  height: 62px;
  background: $bg-input;
  border-radius: $radius-pill;
  border: 2px solid transparent;
  display: flex;
  align-items: center;
  padding: 0 8px 0 26px;
  transition: background 0.15s ease, border-color 0.15s ease;

  &::before {
    content: '🔍';
    font-size: 20px;
    margin-right: 10px;
    opacity: 0.6;
  }

  &.focused {
    background: $bg-card;
    border-color: $primary;
  }
}

.search-input {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  outline: none;
  font-size: 17px;
  color: $ink;
  font-family: inherit;

  &::placeholder {
    color: $faint;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.icon-btn {
  width: 52px;
  height: 52px;
  border: 1px solid $border-soft;
  background: $bg-card;
  border-radius: $radius-md;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: $muted;
  transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease, transform 0.15s ease;

  &:hover {
    background: $bg-subtle;
    border-color: $primary;
    color: $primary;
    transform: scale(1.04);
  }
  &:active { transform: scale(0.96); }
}

.wallpaper-btn {
  width: 57px;
  height: 57px;
}

.vl-main {
  max-width: 1440px;
  margin: 0 auto;
  padding: 32px 32px 80px;
}

.icon-fade-enter-active,
.icon-fade-leave-active {
  transition: opacity 0.15s ease;
}
.icon-fade-enter-from,
.icon-fade-leave-to {
  opacity: 0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity $dur-base ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

:global(.dark) .vl-header {
  background: $dark-card;
  border-color: $dark-border;
}
:global(.dark) .menu-btn {
  color: $dark-ink;
  &:hover { background: $dark-border; color: $primary; }
}
:global(.dark) .search-wrapper {
  background: #2a2a2a;
  &.focused { background: $dark-card; border-color: $primary; }
}
:global(.dark) .search-input {
  color: $dark-ink;
  &::placeholder { color: $dark-faint; }
}
:global(.dark) .icon-btn {
  background: $dark-card;
  border-color: $dark-border;
  color: $dark-muted;
  &:hover { border-color: $primary; color: $primary; background: $dark-border; }
}

// ============ 响应式 ============
// 平板：收敛左右留白
@media (max-width: 1024px) {
  .header-inner { padding: 0 20px; gap: 20px; }
  .vl-main { padding: 24px 20px 72px; }
}

// 手机：导航元素整体缩小，搜索框保留并弹性收缩
@media (max-width: 768px) {
  .header-inner { padding: 0 12px; gap: 10px; }
  .header-left { gap: 10px; }
  .menu-btn { width: 40px; height: 40px; font-size: 20px; }
  .logo-text { font-size: 26px; }

  .search-wrapper {
    height: 40px;
    padding-left: 14px;
    &::before { font-size: 14px; margin-right: 6px; }
  }
  // 输入字号 16px：避免 iOS Safari 聚焦时自动放大页面
  .search-input { font-size: 16px; }

  .icon-btn,
  .wallpaper-btn { width: 40px; height: 40px; }

  .vl-main { padding: 16px 12px 64px; }
}

@media (max-width: 360px) {
  .logo-text { font-size: 23px; }
  .header-left { gap: 6px; }
}
</style>
