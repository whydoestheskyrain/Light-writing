<template>
  <Teleport to="body">
    <div
      v-if="visible"
      ref="floaterRef"
      class="music-floater"
      :class="{ expanded }"
      :style="positionStyle"
      @mousedown="startDrag"
      @touchstart="startTouch"
    >
      <button v-if="!expanded" class="mini-btn" @click.stop="expanded = true">
        <span class="mini-icon">🎵</span>
        <span class="mini-name">{{ currentSong.name }}</span>
      </button>

      <div v-else class="expanded-panel" @click.stop>
        <div class="exp-header">
          <div class="exp-cover">{{ currentSong.cover || '🎵' }}</div>
          <div class="exp-info">
            <div class="exp-name">{{ currentSong.name }}</div>
            <div class="exp-artist">{{ currentSong.artist }}</div>
          </div>
          <button class="exp-close" @click.stop="expanded = false">✕</button>
        </div>
        <div class="exp-progress" @click="onSeek">
          <div class="exp-bar" :style="{ width: progress + '%' }"></div>
        </div>
        <div class="exp-time">
          <span>{{ formatTime(currentTime) }}</span>
          <span>{{ formatTime(duration) }}</span>
        </div>
        <div class="exp-controls">
          <button @click="prev">⏮</button>
          <button class="play-btn" @click="togglePlay">{{ isPlaying ? '⏸' : '▶' }}</button>
          <button @click="next">⏭</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useMusicStore } from '@/stores/music'

const music = useMusicStore()

const visible = computed(() => !!music.currentSong)
const expanded = ref(false)

const currentSong = computed(() => music.currentSong)
const isPlaying = computed(() => music.isPlaying)
const currentTime = computed(() => music.currentTime)
const duration = computed(() => music.duration)
const progress = computed(() => music.progress)
const formatTime = music.formatTime

const togglePlay = () => music.togglePlay()
const next = () => music.next()
const prev = () => music.prev()

const onSeek = (e: MouseEvent) => {
  const el = e.currentTarget as HTMLElement
  const rect = el.getBoundingClientRect()
  const pct = ((e.clientX - rect.left) / rect.width) * 100
  music.seek(Math.max(0, Math.min(100, pct)))
}

// ============ 拖动 + 位置持久化 ============
const positionStyle = ref<Record<string, string>>({})
const dragging = ref(false)
let dragOffsetX = 0
let dragOffsetY = 0
let hasMoved = false
const POSITION_KEY = 'vl-music-pos'

const loadPosition = () => {
  try {
    const raw = localStorage.getItem(POSITION_KEY)
    if (raw) {
      const pos = JSON.parse(raw)
      positionStyle.value = { right: 'auto', bottom: 'auto', left: pos.x + 'px', top: pos.y + 'px' }
    } else {
      positionStyle.value = { right: '24px', bottom: '24px' }
    }
  } catch {
    positionStyle.value = { right: '24px', bottom: '24px' }
  }
}

const startDrag = (e: MouseEvent) => {
  const el = e.currentTarget as HTMLElement
  const rect = el.getBoundingClientRect()
  dragOffsetX = e.clientX - rect.left
  dragOffsetY = e.clientY - rect.top
  dragging.value = true
  hasMoved = false
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

const onDrag = (e: MouseEvent) => {
  if (!dragging.value) return
  hasMoved = true
  const x = Math.max(0, Math.min(window.innerWidth - 60, e.clientX - dragOffsetX))
  const y = Math.max(0, Math.min(window.innerHeight - 60, e.clientY - dragOffsetY))
  positionStyle.value = { right: 'auto', bottom: 'auto', left: x + 'px', top: y + 'px' }
}

const stopDrag = () => {
  if (dragging.value && hasMoved) {
    const el = document.querySelector('.music-floater') as HTMLElement
    if (el) {
      const rect = el.getBoundingClientRect()
      localStorage.setItem(POSITION_KEY, JSON.stringify({ x: rect.left, y: rect.top }))
    }
  }
  dragging.value = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

onMounted(() => {
  loadPosition()
  music.startTimer()
})

// ============ 触摸拖动（手机端） ============
const floaterRef = ref<HTMLElement | null>(null)
let touchStartX = 0
let touchStartY = 0

const startTouch = (e: TouchEvent) => {
  const t = e.touches[0]
  touchStartX = t.clientX
  touchStartY = t.clientY
  const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
  dragOffsetX = t.clientX - rect.left
  dragOffsetY = t.clientY - rect.top
  document.addEventListener('touchmove', onTouchMove, { passive: false })
  document.addEventListener('touchend', onTouchEnd)
}

const onTouchMove = (e: TouchEvent) => {
  const t = e.touches[0]
  // 移动不足 8px 视为点击，不拦截（保证展开/切歌的 click 正常）
  const moved =
    Math.abs(t.clientX - touchStartX) > 8 || Math.abs(t.clientY - touchStartY) > 8
  if (!moved) return
  hasMoved = true
  e.preventDefault()
  const w = floaterRef.value?.offsetWidth ?? 60
  const h = floaterRef.value?.offsetHeight ?? 60
  const x = Math.max(0, Math.min(window.innerWidth - w, t.clientX - dragOffsetX))
  const y = Math.max(0, Math.min(window.innerHeight - h, t.clientY - dragOffsetY))
  positionStyle.value = { right: 'auto', bottom: 'auto', left: x + 'px', top: y + 'px' }
}

const onTouchEnd = () => {
  if (hasMoved && floaterRef.value) {
    const rect = floaterRef.value.getBoundingClientRect()
    localStorage.setItem(POSITION_KEY, JSON.stringify({ x: rect.left, y: rect.top }))
  }
  document.removeEventListener('touchmove', onTouchMove)
  document.removeEventListener('touchend', onTouchEnd)
}

onBeforeUnmount(() => {
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
})
</script>

<style scoped lang="scss">
.music-floater {
  position: fixed;
  z-index: 150;
  user-select: none;
}

.mini-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 44px;
  padding: 0 16px 0 10px;
  border: none;
  border-radius: $radius-pill;
  background: $ink;
  color: #fff;
  cursor: pointer;
  box-shadow: $shadow-card;
  transition: box-shadow $dur-base ease, transform $dur-fast ease;

  &:hover { box-shadow: $shadow-modal; transform: scale(1.03); }
  &:active { transform: scale(0.97); }
}
.mini-icon { font-size: 20px; line-height: 1; }
.mini-name {
  font-size: 13px; font-weight: 500; white-space: nowrap;
  max-width: 100px; overflow: hidden; text-overflow: ellipsis;
}

.expanded-panel {
  width: 280px;
  background: $bg-card;
  border: 1px solid $border-soft;
  border-radius: $radius-lg;
  box-shadow: $shadow-modal;
  padding: 16px;
  cursor: default;
}
.exp-header {
  display: flex; align-items: center; gap: 12px; margin-bottom: 14px;
}
.exp-cover {
  width: 44px; height: 44px; border-radius: $radius-md;
  background: $bg-subtle; display: flex; align-items: center;
  justify-content: center; font-size: 22px; flex-shrink: 0;
}
.exp-info { flex: 1; min-width: 0; }
.exp-name {
  font-size: 14px; font-weight: 600; color: $ink;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.exp-artist { font-size: 12px; color: $faint; margin-top: 2px; }
.exp-close {
  border: none; background: transparent; font-size: 14px;
  color: $faint; cursor: pointer; padding: 4px; transition: color $dur-fast;
  &:hover { color: $ink; }
}

.exp-progress {
  height: 4px; border-radius: 2px; background: $border-soft;
  overflow: hidden; margin-bottom: 6px; cursor: pointer;
}
.exp-bar { height: 100%; border-radius: 2px; background: $primary; transition: width 0.3s linear; }
.exp-time { display: flex; justify-content: space-between; font-size: 11px; color: $faint; margin-bottom: 12px; }
.exp-controls {
  display: flex; align-items: center; justify-content: center; gap: 20px;
  button {
    border: none; background: transparent; font-size: 18px; cursor: pointer;
    color: $muted; padding: 4px; transition: color $dur-fast;
    &:hover { color: $ink; }
  }
  .play-btn {
    width: 40px; height: 40px; border-radius: 50%; background: $ink; color: #fff;
    font-size: 16px; display: flex; align-items: center; justify-content: center;
    &:hover { background: $ink-soft; color: #fff; }
  }
}

:global(.dark) .expanded-panel { background: $dark-card; border-color: $dark-border; }
:global(.dark) .exp-cover { background: $dark-border; }
:global(.dark) .exp-name { color: $dark-ink; }
:global(.dark) .exp-artist { color: $dark-muted; }
:global(.dark) .exp-controls button { color: $dark-muted; &:hover { color: $dark-ink; } }
:global(.dark) .exp-progress { background: $dark-border; }

// ============ 响应式 ============
@media (max-width: 640px) {
  .expanded-panel { width: min(280px, calc(100vw - 24px)); }
  .mini-name { max-width: 70px; }
  .mini-btn { padding-right: 12px; }
}
</style>
