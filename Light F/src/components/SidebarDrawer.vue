<template>
  <Teleport to="body">
    <transition
      @enter="onEnter"
      @after-enter="onAfterEnter"
      @leave="onLeave"
      @after-leave="onAfterLeave"
    >
      <div v-if="open" class="fullscreen-menu" :class="{ leaving: isLeaving }">
        <div class="menu-inner">
          <ul class="menu-list" :class="{ visible: itemsVisible }">
            <li v-for="(item, i) in navItems" :key="item.name" class="menu-item-wrap" :style="{ '--i': i }">
              <button
                class="menu-item"
                :class="{ cta: item.cta }"
                @click="onNavClick(item)"
              >
                <span class="item-label">{{ item.name }}</span>
              </button>
            </li>
          </ul>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'

defineProps<{ open: boolean }>()
const emit = defineEmits<{ (e: 'close'): void }>()

const router = useRouter()
const appStore = useAppStore()
const itemsVisible = ref(false)
const isLeaving = ref(false)

interface NavItem {
  name: string
  path?: string
  cta?: boolean
}

const navItems: NavItem[] = [
  { name: '首页', path: '/' },
  { name: '音乐盒', path: '/music' },
  { name: '个人中心', path: '/profile' },
  { name: '创建模块', path: '/', cta: true }
]

// 面板入场：先 transform 面板，然后触发菜单项级联出现
const onEnter = (_el: any, done: () => void) => {
  isLeaving.value = false
  itemsVisible.value = false
  requestAnimationFrame(() => {
    itemsVisible.value = true
    // 等最后一个菜单项的延迟动画完成后再结束 transition
    const totalDelay = navItems.length * 60 + 320
    setTimeout(done, totalDelay)
  })
}
const onAfterEnter = () => { /* 可选 */ }

const onLeave = (_el: any, done: () => void) => {
  isLeaving.value = true
  itemsVisible.value = false
  setTimeout(done, 260)
}
const onAfterLeave = () => { /* 可选 */ }

const onNavClick = async (item: NavItem) => {
  emit('close')
  if (item.cta) {
    // 跳首页并触发“创建模块”弹窗（Home 监听 createSignal）
    await router.push(item.path || '/')
    appStore.bumpCreate()
  } else if (item.path) {
    router.push(item.path)
  } else {
    router.push({ path: '/coming-soon', query: { name: item.name } })
  }
}
</script>

<style scoped lang="scss">
// ============ 面板容器 ============
.fullscreen-menu {
  position: fixed;
  top: var(--vl-header-h, 83px);
  left: 0;
  right: 0;
  bottom: 0;
  background: $bg-card;
  border-top: 1px solid $border-soft;
  z-index: 200;
  overflow-y: auto;

  // 初始状态：上方 12px + 半透明
  transform: translateY(-12px);
  opacity: 0;
  transition: transform 0.22s ease-out, opacity 0.22s ease-out;
}
// 展开：ease-out 先快后慢 → 响应迅速
.fullscreen-menu:not(.leaving) {
  transform: translateY(0);
  opacity: 1;
}
// 收起：ease-in 先慢后快 → 不生硬
.fullscreen-menu.leaving {
  transform: translateY(-12px);
  opacity: 0;
  transition: transform 0.22s ease-in, opacity 0.22s ease-in;
}

// ============ 菜单内 ============
.menu-inner {
  padding: 40px 24px 80px;
  max-width: 1200px;
  margin: 0 auto;
}

.menu-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

// 菜单项：初始隐藏 + 上方 16px + 模糊
.menu-item-wrap {
  margin-bottom: 32px;
  opacity: 0;
  transform: translateY(-16px);
  filter: blur(4px);
  transition:
    opacity 0.32s cubic-bezier(0.22, 1, 0.36, 1),
    transform 0.32s cubic-bezier(0.22, 1, 0.36, 1),
    filter 0.32s cubic-bezier(0.22, 1, 0.36, 1);
  transition-delay: calc(var(--i, 0) * 60ms);
}

// 展开：依次级联出现
.menu-list.visible .menu-item-wrap {
  opacity: 1;
  transform: translateY(0);
  filter: blur(0);
  transition-delay: calc(var(--i, 0) * 60ms);
}
// 收起时菜单项反向 fade-through：立即开始淡出，让面板的收起动画先跑完
.menu-list:not(.visible) .menu-item-wrap {
  transition-delay: 0s;
  transition-duration: 0.18s;
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  border: none;
  background: transparent;
  padding: 0;
  text-align: left;
  cursor: pointer;
  font-family: inherit;
  color: $ink;

  .item-label {
    font-size: 32px;
    font-weight: 700;
    letter-spacing: -0.01em;
    line-height: 1.1;
    transition: color 0.15s ease, transform 0.15s ease;
  }

  &:hover .item-label {
    color: $primary;
    transform: translateX(4px);
  }
}

.menu-item.cta {
  color: $primary;
  cursor: pointer;

  .item-label { color: inherit; }
  &:hover .item-label { color: $primary-hover; }
}

// ============ 响应式 ============
@media (max-width: 768px) {
  .menu-item .item-label { font-size: 28px; }
  .menu-item-wrap { margin-bottom: 28px; }
  .menu-inner { padding: 32px 24px 60px; }
}

// ============ 暗色模式 ============
:global(.dark) .fullscreen-menu {
  background: $dark-card;
  border-top-color: $dark-border;
}
:global(.dark) .menu-item {
  color: $dark-ink;
  &:hover .item-label { color: $primary; }
}
:global(.dark) .menu-item.cta {
  color: $primary;
  &:hover .item-label { color: $primary-hover; }
}
</style>
