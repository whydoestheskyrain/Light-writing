import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface Song {
  name: string
  artist: string
  cover: string // emoji 占位
  color: string // 专辑封面渐变色
  duration: number // 秒
  url?: string // 真实音频链接
  id?: string // 唯一标识（用于收藏去重）
}

export interface Playlist {
  id: string
  name: string
  icon: string
  songs: Song[]
}

const defaultPlaylists: Playlist[] = [
  {
    id: 'study',
    name: '专注学习',
    icon: '📚',
    songs: [
      { name: 'Lo-Fi Study', artist: 'Chillhop Music', cover: '🎧', color: '#4f8cff', duration: 180, url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3' },
      { name: 'Deep Focus', artist: 'Ambient Vibes', cover: '🌧️', color: '#5b6eee', duration: 200, url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3' },
      { name: 'Coding Mode', artist: 'Synthwave Kid', cover: '🌃', color: '#7c3aed', duration: 160, url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3' },
      { name: 'Coffee Jazz', artist: 'Relax Ensemble', cover: '☕', color: '#b8794a', duration: 190, url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3' },
      { name: 'Rainy Window', artist: 'Nature Sounds', cover: '🌿', color: '#2bb8a0', duration: 210, url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3' }
    ]
  },
  {
    id: 'energy',
    name: '活力满满',
    icon: '🔥',
    songs: [
      { name: 'Morning Boost', artist: 'Upbeat Collective', cover: '☀️', color: '#ff8c42', duration: 170, url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3' },
      { name: 'Run Harder', artist: 'Pulse & Drive', cover: '💪', color: '#ea4c89', duration: 150, url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3' },
      { name: 'Go Getter', artist: 'Electro Youth', cover: '⚡', color: '#f5a623', duration: 185, url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3' }
    ]
  },
  {
    id: 'night',
    name: '夜晚放松',
    icon: '🌙',
    songs: [
      { name: 'Midnight Talk', artist: 'Night Voices', cover: '🌙', color: '#6c5ce7', duration: 200, url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3' },
      { name: 'Starry Sky', artist: 'Dream Orchestra', cover: '✨', color: '#5b6eee', duration: 220, url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3' },
      { name: 'City Lights', artist: 'Neon Jazz', cover: '🏙️', color: '#2d3436', duration: 195, url: 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3' }
    ]
  }
]

// 给每首歌加唯一 id（用于收藏去重）
defaultPlaylists.forEach(pl => pl.songs.forEach((s, i) => { s.id ||= pl.id + '-' + i }))

export const useMusicStore = defineStore('music', () => {
  // ============ 收藏歌单（localStorage 持久化） ============
  const favSongs = ref<Song[]>(
    JSON.parse(localStorage.getItem('vl-music-favs') || '[]')
  )
  const toggleFavorite = (song: Song) => {
    const idx = favSongs.value.findIndex(s => s.id === song.id)
    if (idx >= 0) favSongs.value.splice(idx, 1)
    else favSongs.value.push({ ...song })
    localStorage.setItem('vl-music-favs', JSON.stringify(favSongs.value))
  }
  const isFavorite = (song: Song) => favSongs.value.some(s => s.id === song.id)

  // 收藏歌单作为动态 playlist
  const favoritesPlaylist = computed<Playlist>(() => ({
    id: 'favorites',
    name: '我喜欢的',
    icon: '❤️',
    songs: favSongs.value
  }))

  // 所有歌单（含收藏）— 用于侧栏展示和选择
  const allPlaylists = computed<Playlist[]>(() =>
    [...playlists.value, favoritesPlaylist.value]
  )

  // ============ 播放状态持久化 ============
  const savedPlaylist = localStorage.getItem('vl-music-pl') || 'study'
  const savedIndex = Number(localStorage.getItem('vl-music-idx') || 0)

  const playlists = ref<Playlist[]>(defaultPlaylists)
  const activePlaylistId = ref<string>(savedPlaylist)
  const currentIndex = ref(savedIndex)
  const isPlaying = ref(false)
  const currentTime = ref(0)

  // 当前歌单（可能是收藏歌单）
  const activePlaylist = computed<Playlist>(() => {
    if (activePlaylistId.value === 'favorites') return favoritesPlaylist.value
    return playlists.value.find(p => p.id === activePlaylistId.value) ?? playlists.value[0]
  })
  const songs = computed(() => activePlaylist.value.songs)
  const currentSong = computed(() => songs.value[currentIndex.value])
  const duration = computed(() => currentSong.value?.duration ?? 180)
  const progress = computed(() => (currentTime.value / duration.value) * 100)

  let timer: ReturnType<typeof setInterval> | null = null

  // ============ 音量 + 播放模式（持久化） ============
  const volume = ref(Number(localStorage.getItem('vl-music-volume') ?? 80))
  const playMode = ref<'order' | 'loop' | 'shuffle'>(
    (localStorage.getItem('vl-music-mode') as any) || 'order'
  )
  const modeLabel = { order: '🔁 顺序', loop: '🔂 单曲', shuffle: '🔀 随机' } as const

  // ============ 真实音频播放 ============
  const audioEl = new Audio()
  audioEl.volume = volume.value / 100

  const loadTrack = () => {
    const song = currentSong.value
    if (!song) return
    if (song.url) {
      audioEl.src = song.url
      audioEl.load()
      if (isPlaying.value) audioEl.play().catch(() => {})
    }
  }

  // 音频事件：ended → 下一首，loadedmetadata → 更新时长，timeupdate → 更新进度
  audioEl.addEventListener('loadedmetadata', () => {
    if (currentSong.value?.url && audioEl.duration) {
      const idx = currentIndex.value
      const pl = playlists.value.find(p => p.id === activePlaylistId.value)
      if (pl && pl.songs[idx]) pl.songs[idx].duration = Math.round(audioEl.duration)
    }
  })
  audioEl.addEventListener('timeupdate', () => {
    if (currentSong.value?.url && audioEl.currentTime) {
      currentTime.value = Math.floor(audioEl.currentTime)
    }
  })
  audioEl.addEventListener('ended', () => {
    next()
  })

  // ============ 静音 ============
  let prevVolume = volume.value
  const toggleMute = () => {
    if (volume.value > 0) {
      prevVolume = volume.value
      setVolume(0)
    } else {
      setVolume(prevVolume || 80)
    }
  }

  const setVolume = (v: number) => {
    volume.value = Math.max(0, Math.min(100, Math.round(v)))
    audioEl.volume = volume.value / 100
    localStorage.setItem('vl-music-volume', String(volume.value))
  }
  const cycleMode = () => {
    const order: Array<'order' | 'loop' | 'shuffle'> = ['order', 'loop', 'shuffle']
    playMode.value = order[(order.indexOf(playMode.value) + 1) % order.length]
    localStorage.setItem('vl-music-mode', playMode.value)
  }

  const startTimer = () => {
    if (timer) clearInterval(timer)
    timer = setInterval(() => {
      // 有真实音频时由 timeupdate 事件更新进度，这里只处理模拟播放
      if (isPlaying.value && !currentSong.value?.url) {
        currentTime.value += 1
        if (currentTime.value >= duration.value) next()
      }
    }, 1000)
  }

  const togglePlay = () => {
    isPlaying.value = !isPlaying.value
    if (isPlaying.value && currentSong.value?.url) audioEl.play().catch(() => {})
    else if (!isPlaying.value && currentSong.value?.url) audioEl.pause()
    persistState()
  }
  const next = () => {
    if (!songs.value.length) return
    if (playMode.value === 'loop') {
      currentTime.value = 0
      if (currentSong.value?.url) { audioEl.currentTime = 0; audioEl.play().catch(() => {}) }
      isPlaying.value = true
      return
    }
    if (playMode.value === 'shuffle' && songs.value.length > 1) {
      let r = currentIndex.value
      while (r === currentIndex.value) r = Math.floor(Math.random() * songs.value.length)
      currentIndex.value = r
    } else {
      currentIndex.value = (currentIndex.value + 1) % songs.value.length
    }
    currentTime.value = 0
    isPlaying.value = true
    loadTrack()
    persistState()
  }
  const prev = () => {
    if (!songs.value.length) return
    currentIndex.value = (currentIndex.value - 1 + songs.value.length) % songs.value.length
    currentTime.value = 0
    isPlaying.value = true
    loadTrack()
    persistState()
  }
  const seek = (percent: number) => {
    currentTime.value = Math.floor((percent / 100) * duration.value)
    if (currentSong.value?.url) audioEl.currentTime = currentTime.value
  }
  const playAt = (index: number) => {
    currentIndex.value = index
    currentTime.value = 0
    isPlaying.value = true
    loadTrack()
    persistState()
  }
  const selectPlaylist = (id: string) => {
    activePlaylistId.value = id
    currentIndex.value = 0
    currentTime.value = 0
    isPlaying.value = false
    if (currentSong.value?.url) { audioEl.pause(); audioEl.src = '' }
    persistState()
  }
  // 播放全部：从第一首开始播放
  const playAll = () => {
    if (!songs.value.length) return
    currentIndex.value = 0
    currentTime.value = 0
    isPlaying.value = true
    loadTrack()
    persistState()
  }
  // 状态持久化
  const persistState = () => {
    localStorage.setItem('vl-music-pl', activePlaylistId.value)
    localStorage.setItem('vl-music-idx', String(currentIndex.value))
  }

  const formatTime = (s: number) => {
    const m = Math.floor(s / 60)
    const sec = Math.floor(s % 60)
    return `${m}:${sec.toString().padStart(2, '0')}`
  }

  return {
    playlists,
    allPlaylists,
    activePlaylistId,
    activePlaylist,
    songs,
    currentSong,
    volume,
    playMode,
    modeLabel,
    setVolume,
    toggleMute,
    cycleMode,
    currentIndex,
    isPlaying,
    currentTime,
    duration,
    progress,
    togglePlay,
    next,
    prev,
    seek,
    loadTrack,
    playAt,
    playAll,
    selectPlaylist,
    toggleFavorite,
    isFavorite,
    favSongs,
    startTimer,
    formatTime
  }
})
