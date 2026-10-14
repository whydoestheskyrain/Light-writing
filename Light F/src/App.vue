<template>
  <!-- 认证页使用独立品牌布局，不显示主框架 -->
  <router-view v-if="isAuthPage" />

  <div v-else id="app-root">
    <header class="vl-header">
      <div class="header-inner">
        <div class="header-left">
          <button class="icon-btn menu-btn" @click="drawerOpen = !drawerOpen" aria-label="菜单">
            <svg v-if="!drawerOpen" key="burger" width="20" height="20" viewBox="0 0 24 24" fill="none"
              stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
              <line x1="3" y1="6" x2="21" y2="6" />
              <line x1="3" y1="12" x2="21" y2="12" />
              <line x1="3" y1="18" x2="21" y2="18" />
            </svg>
            <svg v-else key="close" width="20" height="20" viewBox="0 0 24 24" fill="none"
              stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
              <line x1="6" y1="6" x2="18" y2="18" />
              <line x1="6" y1="18" x2="18" y2="6" />
            </svg>
          </button>
          <div class="logo" @click="router.push('/')">
            <span class="logo-text">Light</span>
          </div>
        </div>

        <div class="search-block">
          <div class="search-wrapper" :class="{ focused: searchFocused }">
            <svg class="search-icon" viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor"
              stroke-width="2.2" stroke-linecap="round">
              <circle cx="11" cy="11" r="7" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
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
          <button class="icon-btn wallpaper-btn" @click="goWallpaper" title="切换主题壁纸">
            <svg viewBox="0 0 24 24" width="19" height="19" fill="none" stroke="currentColor" stroke-width="2"
              stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="3" width="18" height="18" rx="3" />
              <circle cx="8.5" cy="8.5" r="1.5" />
              <polyline points="21 15 16 10 5 21" />
            </svg>
          </button>

          <!-- 用户头像 + 下拉面板 -->
          <button ref="avatarBtn" class="avatar-btn" :class="{ open: userPanelOpen }"
            @click.stop="toggleUserPanel" :title="userStore.user?.nickname || '我的'">
            {{ avatarText }}
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
    <UserMenuPanel :open="userPanelOpen" :anchor="avatarBtn" @close="userPanelOpen = false" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import SidebarDrawer from '@/components/SidebarDrawer.vue'
import UserMenuPanel from '@/components/UserMenuPanel.vue'
import { useStudyStore } from '@/stores/study'
import { useUserStore } from '@/stores/user'
import { applyWallpaper, wallpapers, pickWallpaper, wallIndex } from '@/config/wallpapers'

const router = useRouter()
const route = useRoute()

const study = useStudyStore()
const userStore = useUserStore()

// 登录/注册页使用独立布局
const isAuthPage = computed(() => route.name === 'Login' || route.name === 'Register')

// 进入模块/条目页时记录学习上下文
watch(() => route.fullPath, () => {
  drawerOpen.value = false
  userPanelOpen.value = false
  if (route.name === 'ModuleDetail') study.setModule(String(route.params.id))
  else if (route.name === 'EntryDetail' || route.name === 'EntryEdit' || route.name === 'EntryCreate') {
    const mid = route.params.moduleId || route.params.id
    study.setModule(mid ? String(mid) : '')
  } else study.setModule('')
})

// ============ 顶部下拉面板 ============
const drawerOpen = ref(false)
const userPanelOpen = ref(false)
const avatarBtn = ref<HTMLElement | null>(null)

const avatarText = computed(() => {
  const name = userStore.user?.nickname || userStore.user?.username || 'L'
  return name.slice(0, 1).toUpperCase()
})

const toggleUserPanel = () => {
  userPanelOpen.value = !userPanelOpen.value
}

const onDocClick = (e: MouseEvent) => {
  // UserMenuPanel 内部自行处理外部点击；这里只处理菜单抽屉
  if (!drawerOpen.value) return
}

const onKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') {
    drawerOpen.value = false
    userPanelOpen.value = false
  }
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

// ============ 壁纸循环切换 ============
const goWallpaper = () => {
  const next = wallIndex.value < wallpapers.length - 1 ? wallIndex.value + 1 : -1
  pickWallpaper(next)
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
  document.addEventListener('click', onDocClick)
  study.start()
  applyWallpaper()
  if (!isAuthPage.value) userStore.fetchMe()
})

// 登录成功跳转后补拉用户信息
watch(isAuthPage, (auth) => {
  if (!auth) userStore.fetchMe(true)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
  document.removeEventListener('click', onDocClick)
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
  height: var(--vl-header-h, 68px);
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: saturate(180%) blur(14px);
  border-bottom: 1px solid $border-soft;
  transition: background-color 0.3s ease, border-color 0.3s ease;
}

.header-inner {
  max-width: 1440px;
  height: 100%;
  margin: 0 auto;
  padding: 0 28px;
  display: flex;
  align-items: center;
  gap: 24px;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

// 通用方形图标按钮
.icon-btn,
.menu-btn {
  width: 40px;
  height: 40px;
  border: none;
  background: transparent;
  border-radius: $radius-md;
  color: $ink-soft;
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
  cursor: pointer;
  padding-left: 4px;
}
.logo-text {
  font-size: 26px;
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

// ============ 搜索 ============
.search-block {
  flex: 1;
  max-width: 560px;
  min-width: 0;
  display: flex;
  align-items: center;
}

.search-wrapper {
  flex: 1;
  min-width: 0;
  height: 44px;
  background: $bg-input;
  border-radius: $radius-pill;
  border: 1.5px solid transparent;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  transition: background 0.15s ease, border-color 0.15s ease, box-shadow 0.15s ease;

  &.focused {
    background: $bg-card;
    border-color: rgba(234, 76, 137, 0.55);
    box-shadow: 0 0 0 3px rgba(234, 76, 137, 0.1);
  }
}

.search-icon {
  color: $faint;
  flex-shrink: 0;
}
.search-wrapper.focused .search-icon {
  color: $primary;
}

.search-input {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  outline: none;
  font-size: 14.5px;
  color: $ink;
  font-family: inherit;

  &::placeholder {
    color: $faint;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.wallpaper-btn {
  width: 40px;
  border: 1px solid transparent;
}

// ============ 用户头像 ============
.avatar-btn {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 50%;
  background: linear-gradient(135deg, $primary, #7c5cff);
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(234, 76, 137, 0.3);
  transition: transform $dur-fast, box-shadow $dur-fast, outline-color $dur-fast;
  outline: 2px solid transparent;
  outline-offset: 2px;

  &:hover {
    transform: scale(1.06);
  }
  &.open {
    outline-color: rgba(234, 76, 137, 0.4);
  }
}

.vl-main {
  max-width: 1440px;
  margin: 0 auto;
  padding: 28px 32px 72px;
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
  background: rgba(20, 20, 20, 0.85);
  border-color: $dark-border;
}
:global(.dark) .menu-btn,
:global(.dark) .icon-btn {
  color: $dark-ink;
  &:hover { background: $dark-border; color: $primary; }
}
:global(.dark) .search-wrapper {
  background: #2a2a2a;
  &.focused { background: $dark-card; }
}
:global(.dark) .search-input {
  color: $dark-ink;
  &::placeholder { color: $dark-faint; }
}

// ============ 响应式 ============
@media (max-width: 1024px) {
  .header-inner { padding: 0 20px; gap: 16px; }
  .vl-main { padding: 22px 20px 64px; }
}

@media (max-width: 768px) {
  .header-inner { padding: 0 12px; gap: 8px; }
  .header-left { gap: 6px; }
  .icon-btn, .menu-btn, .wallpaper-btn { width: 36px; height: 36px; }
  .logo-text { font-size: 22px; }

  .search-wrapper {
    height: 38px;
    padding: 0 14px;
    gap: 8px;
  }
  .search-input { font-size: 16px; }
  .avatar-btn { width: 36px; height: 36px; font-size: 13px; }
  .vl-main { padding: 16px 12px 56px; }
}
</style>
