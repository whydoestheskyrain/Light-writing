<template>
  <div class="music-page">
    <!-- 左侧：歌单列表 -->
    <aside class="sidebar">
      <div class="section-title">歌单</div>
      <ul class="playlist-list">
        <li
          v-for="pl in allPlaylists"
          :key="pl.id"
          class="pl-item"
          :class="{ active: activePlaylistId === pl.id }"
          @click="selectPlaylist(pl.id)"
        >
          <span class="pl-icon">{{ pl.icon }}</span>
          <div class="pl-info">
            <div class="pl-name">{{ pl.name }}</div>
            <div class="pl-count">{{ pl.songs.length }} 首</div>
          </div>
        </li>
      </ul>
    </aside>

    <!-- 中间：歌曲列表 -->
    <main class="song-area">
      <header class="song-header">
        <div class="cover" :style="{ background: `linear-gradient(135deg, ${activePlaylist.songs[0]?.color || '#4f8cff'}, #7c3aed)` }">{{ activePlaylist.icon }}</div>
        <div>
          <div class="h-label">正在播放</div>
          <h1>{{ activePlaylist.name }}</h1>
          <div class="h-meta">{{ songs.length }} 首 · 学习专注</div>
        </div>
        <button class="primary-play" @click="onPlayAll">
          {{ isPlaying ? '⏸ 暂停' : '▶ 播放全部' }}
        </button>
      </header>

      <!-- 精选专辑装饰 -->
      <div class="album-wall">
        <div
          v-for="(song, i) in songs.slice(0, 4)"
          :key="i"
          class="album-card"
          :class="{ active: currentIndex === i }"
          :style="{ background: `linear-gradient(135deg, ${song.color}, ${song.color}cc)` }"
          @click="playAt(i)"
        >
          <span class="album-emoji">{{ song.cover }}</span>
          <div class="album-name">{{ song.name }}</div>
          <div class="album-artist">{{ song.artist }}</div>
          <div v-if="currentIndex === i && isPlaying" class="album-playing">♪ 播放中</div>
        </div>
      </div>

      <!-- 搜索过滤 -->
      <div class="song-search">
        <input v-model="keyword" type="text" placeholder="🔍 搜索歌名或歌手..." />
      </div>

      <ul class="song-list">
        <li
          v-for="song in filteredSongs"
          :key="song.id || song.name"
          class="song-row"
          :class="{ playing: isPlaying && currentIndex === songs.indexOf(song), active: currentIndex === songs.indexOf(song) }"
          @click="playAt(songs.indexOf(song))"
        >
          <span class="s-idx">{{ currentIndex === songs.indexOf(song) && isPlaying ? '♪' : String(songs.indexOf(song) + 1).padStart(2, '0') }}</span>
          <span class="s-cover">{{ song.cover }}</span>
          <div class="s-info">
            <div class="s-name">{{ song.name }}</div>
            <div class="s-artist">{{ song.artist }}</div>
          </div>
          <span
            class="s-fav"
            :class="{ liked: isFavorite(song) }"
            @click.stop="toggleFavorite(song)"
          >{{ isFavorite(song) ? '❤️' : '🤍' }}</span>
          <span class="s-dur">{{ formatTime(song.duration) }}</span>
        </li>
        <li v-if="!filteredSongs.length" class="song-empty">没有找到匹配的歌曲</li>
      </ul>
    </main>

    <!-- 底部播放器条 -->
    <footer class="player-bar">
      <div class="pb-now">
        <div class="pb-cover" :style="{ background: `linear-gradient(135deg, ${currentSong.color}, ${currentSong.color}cc)` }">{{ currentSong.cover }}</div>
        <div class="pb-info">
          <div class="pb-name">{{ currentSong.name }}</div>
          <div class="pb-artist">{{ currentSong.artist }}</div>
        </div>
      </div>

      <div class="pb-center">
        <div class="pb-controls">
          <button @click="prev" title="上一首">⏮</button>
          <button class="pb-play" @click="togglePlay">{{ isPlaying ? '⏸' : '▶' }}</button>
          <button @click="next" title="下一首">⏭</button>
        </div>
        <div
          class="pb-progress"
          @mousedown="startDragSeek"
        >
          <div class="pb-bar" :style="{ width: progress + '%' }"></div>
        </div>
        <div class="pb-time">
          <span>{{ formatTime(currentTime) }}</span>
          <span>{{ formatTime(duration) }}</span>
        </div>
      </div>

      <div class="pb-right">
        <button class="pb-mode" :title="modeLabel[playMode]" @click="cycleMode">
          {{ modeLabel[playMode] }}
        </button>
        <button class="pb-mute" :title="volume > 0 ? '静音' : '取消静音'" @click="toggleMute">
          {{ volume > 0 ? '🔊' : '🔇' }}
        </button>
        <input
          class="pb-volume"
          type="range"
          min="0"
          max="100"
          :value="volume"
          @input="setVolume(Number(($event.target as HTMLInputElement).value))"
        />
        <span class="pb-vol-num">{{ volume }}</span>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { storeToRefs } from 'pinia'
import { useMusicStore } from '@/stores/music'

const music = useMusicStore()
const {
  allPlaylists, activePlaylist, activePlaylistId, songs, currentSong,
  currentIndex, isPlaying, currentTime, duration, progress, volume, playMode
} = storeToRefs(music)
const modeLabel = music.modeLabel
const togglePlay = () => music.togglePlay()
const next = () => music.next()
const prev = () => music.prev()
const playAt = (i: number) => music.playAt(i)
const playAll = () => music.playAll()
const selectPlaylist = (id: string) => music.selectPlaylist(id)
const formatTime = music.formatTime
const setVolume = (v: number) => music.setVolume(v)
const toggleMute = () => music.toggleMute()
const cycleMode = () => music.cycleMode()
const toggleFavorite = (song: any) => music.toggleFavorite(song)
const isFavorite = (song: any) => music.isFavorite(song)

// 播放全部：未播放时从第一首开始，正在播放时暂停
const onPlayAll = () => {
  if (isPlaying.value) togglePlay()
  else playAll()
}

// 搜索过滤（歌名或歌手）
const keyword = ref('')
const filteredSongs = computed(() => {
  const k = keyword.value.trim().toLowerCase()
  if (!k) return songs.value
  return songs.value.filter(
    (s: any) => s.name.toLowerCase().includes(k) || s.artist.toLowerCase().includes(k)
  )
})

// ============ 进度条拖拽 seek ============
let dragging = false
const seekFromEvent = (e: MouseEvent) => {
  const el = document.querySelector('.pb-progress') as HTMLElement
  if (!el) return
  const rect = el.getBoundingClientRect()
  const pct = ((e.clientX - rect.left) / rect.width) * 100
  music.seek(Math.max(0, Math.min(100, pct)))
}
const startDragSeek = (e: MouseEvent) => {
  dragging = true
  seekFromEvent(e)
  const onMove = (ev: MouseEvent) => { if (dragging) seekFromEvent(ev) }
  const onUp = () => {
    dragging = false
    document.removeEventListener('mousemove', onMove)
    document.removeEventListener('mouseup', onUp)
  }
  document.addEventListener('mousemove', onMove)
  document.addEventListener('mouseup', onUp)
}

// ============ 键盘快捷键 ============
const onKeydown = (e: KeyboardEvent) => {
  const tag = (e.target as HTMLElement)?.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA') return // 输入框中不拦截
  switch (e.code) {
    case 'Space':
      e.preventDefault(); togglePlay(); break
    case 'ArrowLeft':
      e.preventDefault(); prev(); break
    case 'ArrowRight':
      e.preventDefault(); next(); break
    case 'ArrowUp':
      e.preventDefault(); setVolume(volume.value + 5); break
    case 'ArrowDown':
      e.preventDefault(); setVolume(volume.value - 5); break
    case 'KeyM':
      toggleMute(); break
  }
}
onMounted(() => {
  window.addEventListener('keydown', onKeydown)
  music.startTimer()
})
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped lang="scss">
.music-page {
  display: grid;
  grid-template-columns: 240px 1fr;
  grid-template-rows: 1fr 96px;
  grid-template-areas:
    "sidebar main"
    "player  player";
  height: calc(100vh - 83px);
  max-width: 1440px;
  margin: 0 auto;
  padding: 0 24px;
  gap: 24px;
}

// ============ 左侧歌单 ============
.sidebar {
  grid-area: sidebar;
  padding: 24px 0;
  overflow-y: auto;

  .section-title {
    font-size: 13px;
    font-weight: 600;
    color: $muted;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    margin-bottom: 16px;
    padding: 0 8px;
  }
}

.playlist-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.pl-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: $radius-md;
  cursor: pointer;
  transition: background $dur-fast;

  &:hover { background: $bg-subtle; }
  &.active {
    background: rgba($primary, 0.08);
    .pl-name { color: $primary; }
  }
}
.pl-icon {
  width: 40px;
  height: 40px;
  border-radius: $radius-sm;
  background: $bg-subtle;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}
.pl-info { flex: 1; min-width: 0; }
.pl-name {
  font-size: 14px;
  font-weight: 600;
  color: $ink;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.pl-count { font-size: 12px; color: $faint; margin-top: 2px; }

// ============ 中间歌曲 ============
.song-area {
  grid-area: main;
  padding: 24px 0;
  overflow-y: auto;
}

.song-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 28px;
  padding-bottom: 24px;
  border-bottom: 1px solid $border-soft;

  .cover {
    width: 80px;
    height: 80px;
    border-radius: $radius-md;
    background: linear-gradient(135deg, $primary, #7c3aed);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 36px;
    flex-shrink: 0;
    box-shadow: $shadow-card;
  }

  h1 {
    font-size: 28px;
    font-weight: 700;
    color: $ink;
    margin: 4px 0 4px;
    letter-spacing: -0.02em;
  }
  .h-label { font-size: 11px; font-weight: 600; color: $primary; letter-spacing: 0.1em; text-transform: uppercase; }
  .h-meta { font-size: 13px; color: $faint; }

  .primary-play {
    margin-left: auto;
    height: 44px;
    padding: 0 24px;
    border: none;
    background: $ink;
    color: #fff;
    border-radius: $radius-pill;
    font-size: 14px;
    font-weight: 600;
    cursor: pointer;
    transition: background $dur-fast;
    &:hover { background: $ink-soft; }
  }
}

.song-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
}

.song-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 12px;
  border-radius: $radius-sm;
  cursor: pointer;
  transition: background $dur-fast;

  &:hover { background: $bg-subtle; }
  &.active { .s-name { color: $primary; } }
  &.playing { .s-idx { color: $primary; font-style: normal; font-weight: 700; } }

  .s-idx {
    width: 24px;
    text-align: center;
    font-size: 13px;
    color: $faint;
    font-variant-numeric: tabular-nums;
  }
  .s-cover {
    width: 36px;
    height: 36px;
    border-radius: $radius-xs;
    background: $bg-subtle;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    flex-shrink: 0;
  }
  .s-info { flex: 1; min-width: 0; }
  .s-name {
    font-size: 14px;
    font-weight: 500;
    color: $ink;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .s-artist { font-size: 12px; color: $faint; margin-top: 2px; }
  .s-dur {
    font-size: 13px;
    color: $faint;
    font-variant-numeric: tabular-nums;
  }
  // 收藏按钮
  .s-fav {
    cursor: pointer;
    font-size: 16px;
    opacity: 0.5;
    transition: opacity $dur-fast, transform $dur-fast;
    &:hover { opacity: 1; transform: scale(1.15); }
    &.liked { opacity: 1; }
  }
}

// ============ 底部播放器条 ============
.player-bar {
  grid-area: player;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 0 20px;
  background: $bg-card;
  border-top: 1px solid $border-soft;
  border-radius: $radius-lg $radius-lg 0 0;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.04);
}

.pb-now {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 200px;
}
.pb-cover {
  width: 56px;
  height: 56px;
  border-radius: $radius-md;
  background: linear-gradient(135deg, $primary, #7c3aed);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
  flex-shrink: 0;
}
.pb-info { min-width: 0; }
.pb-name {
  font-size: 14px;
  font-weight: 600;
  color: $ink;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.pb-artist { font-size: 12px; color: $faint; margin-top: 2px; }

.pb-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  max-width: 600px;
  margin: 0 auto;
}
.pb-controls {
  display: flex;
  align-items: center;
  gap: 24px;
  button {
    border: none;
    background: transparent;
    font-size: 18px;
    cursor: pointer;
    color: $muted;
    padding: 4px;
    transition: color $dur-fast;
    &:hover { color: $ink; }
  }
  .pb-play {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    background: $ink;
    color: #fff;
    font-size: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    &:hover { background: $primary; color: #fff; }
  }
}
.pb-progress {
  width: 100%;
  height: 4px;
  border-radius: 2px;
  background: $border-soft;
  overflow: hidden;
  cursor: pointer;
}
.pb-bar { height: 100%; border-radius: 2px; background: $primary; transition: width 0.3s linear; }
.pb-time {
  display: flex;
  gap: 8px;
  font-size: 11px;
  color: $faint;
  font-variant-numeric: tabular-nums;
}

.pb-right {
  min-width: 220px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}
.pb-mute {
  font-size: 18px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 2px;
  &:hover { opacity: 0.7; }
}
.pb-mode {
  border: 1px solid $border;
  background: transparent;
  border-radius: $radius-pill;
  padding: 6px 12px;
  font-size: 12px;
  color: $muted;
  cursor: pointer;
  white-space: nowrap;
  transition: color $dur-fast, border-color $dur-fast;
  &:hover { color: $primary; border-color: $primary; }
}
.pb-volume {
  width: 80px;
  accent-color: $primary;
  cursor: pointer;
}
.pb-vol-num {
  width: 24px;
  font-size: 11px;
  color: $faint;
  text-align: left;
  font-variant-numeric: tabular-nums;
}

// ============ 精选专辑装饰墙 ============
.album-wall {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.album-card {
  border-radius: $radius-md;
  padding: 20px 16px;
  cursor: pointer;
  transition: transform $dur-fast, box-shadow $dur-fast;
  position: relative;
  overflow: hidden;
  min-height: 120px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  box-shadow: $shadow-card;

  &:hover { transform: translateY(-3px); box-shadow: $shadow-hover; }
  &.active { box-shadow: 0 0 0 2px $primary, $shadow-card; }
}
.album-emoji {
  position: absolute;
  top: 14px;
  left: 16px;
  font-size: 28px;
}
.album-name {
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 1px 4px rgba(0,0,0,0.2);
}
.album-artist {
  font-size: 11px;
  color: rgba(255,255,255,0.85);
  margin-top: 2px;
}
.album-playing {
  position: absolute;
  top: 14px;
  right: 12px;
  font-size: 10px;
  font-weight: 600;
  color: #fff;
  background: rgba(0,0,0,0.25);
  padding: 2px 8px;
  border-radius: $radius-pill;
}

.song-search {
  margin-bottom: 16px;
  input {
    width: 100%;
    max-width: 360px;
    height: 40px;
    padding: 0 16px;
    border: 1px solid $border;
    border-radius: $radius-pill;
    background: $bg-card;
    font-size: 14px;
    color: $ink;
    outline: none;
    transition: border-color $dur-fast, box-shadow $dur-fast;
    &:focus { border-color: $primary; box-shadow: 0 0 0 3px rgba($primary, 0.12); }
  }
}
.song-empty {
  padding: 32px 12px;
  text-align: center;
  font-size: 14px;
  color: $faint;
  list-style: none;
}

// ============ 暗色模式 ============
:global(.dark) .pl-item:hover { background: $dark-border; }
:global(.dark) .pl-item.active { background: rgba($primary, 0.15); .pl-name { color: $primary; } }
:global(.dark) .pl-icon { background: $dark-border; }
:global(.dark) .pl-name { color: $dark-ink; }
:global(.dark) h1 { color: $dark-ink; }
:global(.dark) .song-row:hover { background: $dark-border; }
:global(.dark) .s-idx { color: $dark-muted; }
:global(.dark) .s-cover { background: $dark-border; }
:global(.dark) .s-name { color: $dark-ink; }
:global(.dark) .player-bar { background: $dark-card; border-top-color: $dark-border; }
:global(.dark) .pb-name { color: $dark-ink; }
:global(.dark) .pb-progress { background: $dark-border; }
:global(.dark) .pb-controls button { color: $dark-muted; &:hover { color: $dark-ink; } }
:global(.dark) .pb-mode { border-color: $dark-border; color: $dark-muted; &:hover { color: $primary; border-color: $primary; } }
:global(.dark) .song-search input { background: $dark-card; border-color: $dark-border; color: $dark-ink; }

// ============ 响应式 ============
@media (max-width: 900px) {
  .music-page {
    grid-template-columns: 1fr;
    grid-template-rows: auto 1fr auto;
    grid-template-areas:
      "sidebar"
      "main"
      "player";
    height: calc(100vh - var(--vl-header-h, 83px));
    gap: 12px;
    padding: 0;
  }
  // 歌单侧栏 → 顶部横向滚动 chips（不隐藏，保证可切换歌单）
  .sidebar {
    display: block;
    min-width: 0;
    padding: 12px 0 0;
    overflow: visible;
    .section-title { display: none; }
  }
  .playlist-list {
    flex-direction: row;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    gap: 8px;
  }
  .pl-item {
    flex-shrink: 0;
    padding: 6px 12px 6px 8px;
    gap: 8px;
    .pl-icon { width: 32px; height: 32px; font-size: 16px; }
    .pl-count { display: none; }
  }
  .song-area { padding: 8px 0; }
}

@media (max-width: 640px) {
  // 专辑墙 → 2 列
  .album-wall { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  // 歌曲头部收缩，播放全部按钮独占一行
  .song-header {
    flex-wrap: wrap;
    gap: 12px;
    margin-bottom: 16px;
    padding-bottom: 14px;
    .cover { width: 52px; height: 52px; font-size: 24px; }
    h1 { font-size: 20px; }
  }
  .primary-play { margin-left: 0; width: 100%; height: 40px; }

  // 播放条换行：进度条区独占整行
  .player-bar {
    flex-wrap: wrap;
    gap: 10px 12px;
    padding: 10px 12px;
  }
  .pb-now { min-width: 0; flex: 1; gap: 8px; }
  .pb-cover { width: 40px; height: 40px; font-size: 18px; }
  .pb-center { order: 3; flex-basis: 100%; max-width: none; gap: 6px; }
  .pb-controls { gap: 16px; }
  .pb-right { min-width: 0; gap: 6px; }
  .pb-volume { width: 60px; }
  .pb-vol-num, .pb-mute { display: none; }
  .pb-mode { padding: 6px 10px; }
}
</style>
