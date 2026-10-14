<template>
  <div
    class="module-card"
    @click="goDetail"
  >
    <!-- 有封面图且 blob 地址就绪后显示图片，否则用主题色纯色 -->
    <img
      v-if="coverUrl"
      class="card-cover"
      :src="coverUrl"
      :alt="module.name"
      @error="onCoverError"
    />
    <div v-else class="card-fallback" :style="{ background: module.color || '#ea4c89' }">
      <span class="fallback-icon">{{ module.icon || '📁' }}</span>
    </div>

    <el-dropdown trigger="click" @click.stop @command="handleCommand">
      <button class="menu-btn" @click.stop aria-label="更多操作">⋯</button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item command="edit">编辑模块</el-dropdown-item>
          <el-dropdown-item command="delete" divided>删除模块</el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <!-- 常显统计角标 -->
    <div v-if="stats" class="stat-chip">
      <span>{{ stats.total }} 条</span>
      <span v-if="stats.done > 0" class="stat-done">已掌握 {{ stats.done }}</span>
    </div>

    <div class="card-overlay">
      <h3 class="card-title">{{ module.name }}</h3>
      <div class="card-meta">
        <span class="meta-date">{{ formatTime(module.createTime) }}</span>
        <span class="meta-enter">进入模块 →</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import type { Module } from '@/types'
import { resolveDisplayUrl } from '@/utils/fileUrl'

const props = defineProps<{ module: Module }>()
const emit = defineEmits<{
  (e: 'edit', module: Module): void
  (e: 'delete', module: Module): void
}>()

const router = useRouter()
const store = useAppStore()

const stats = computed(() => store.entryStats[String(props.module.id)])

const goDetail = () => router.push(`/module/${props.module.id}`)

const handleCommand = (cmd: string) => {
  if (cmd === 'edit') emit('edit', props.module)
  if (cmd === 'delete') emit('delete', props.module)
}

const formatTime = (t: string) => (t ? t.slice(0, 10) : '')

// 封面图：后端存的是受保护稳定路径，<img> 原生请求不带 JWT，
// 必须由前端带 token 拉成 blob URL 后再绑定
const coverUrl = ref('')
watch(
  () => props.module.coverImage,
  async (path) => {
    coverUrl.value = await resolveDisplayUrl(path)
  },
  { immediate: true }
)

// 图片加载失败时隐藏，让 fallback 纯色露出来
const onCoverError = (e: Event) => {
  ;(e.target as HTMLImageElement).style.display = 'none'
}
</script>

<style scoped lang="scss">
// el-dropdown 的包裹 div 默认 relative 且宽度塌缩为 0，
// 会让 absolute 的菜单按钮相对它错位定位；改为 static 后按钮相对卡片定位
:deep(.el-dropdown) {
  position: static;
}

.module-card {
  position: relative;
  aspect-ratio: 4 / 3;
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid $border-soft;
  cursor: pointer;
  background: $bg-subtle;
  transition: transform $dur-base ease, box-shadow $dur-base ease, filter $dur-base ease;

  &:hover {
    filter: brightness(1.03);
    transform: translateY(-3px);
    box-shadow: $shadow-card;
  }
}

// ============ 封面图 ============
.card-cover {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  z-index: 1;
  background: $bg-subtle;
}

// ============ 无封面时的纯色 fallback ============
.card-fallback {
  position: absolute;
  inset: 0;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.fallback-icon {
  font-size: 52px;
  opacity: 0.55;
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.15));
}

// ============ 右上角菜单 ============
.menu-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 4;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 50%;
  background: rgba(13, 12, 34, 0.42);
  backdrop-filter: blur(4px);
  color: #fff;
  font-size: 17px;
  line-height: 1;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s, background 0.2s;
}

// 触屏设备没有 hover，菜单保持常显；桌面端默认隐藏、hover 卡片时显示
@media (hover: none) {
  .menu-btn {
    opacity: 1;
  }
}
.module-card:hover .menu-btn {
  opacity: 1;
}

// ============ 常显统计角标（左下角） ============
.stat-chip {
  position: absolute;
  left: 12px;
  bottom: 12px;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 10px;
  border-radius: $radius-pill;
  background: rgba(13, 12, 34, 0.45);
  backdrop-filter: blur(4px);
  color: #fff;
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.02em;

  .stat-done {
    color: #7bf1a8;
  }
}

// ============ 底部渐变遮罩与文字（常驻，hover 时略微加深） ============
.card-overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 3;
  height: 52%;
  padding: 12px 16px 13px;
  background: linear-gradient(to bottom, rgba(0,0,0,0) 0%, rgba(13,12,34,0.28) 45%, rgba(13,12,34,0.72) 100%);
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  transition: background $dur-base ease;
}
.module-card:hover .card-overlay {
  background: linear-gradient(to bottom, rgba(0,0,0,0) 0%, rgba(13,12,34,0.34) 45%, rgba(13,12,34,0.8) 100%);
}

.card-title {
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 8px;
  letter-spacing: -0.01em;
  @include ellipsis(1);
}

.card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #fff;
}

.meta-enter {
  font-weight: 500;
}
</style>
