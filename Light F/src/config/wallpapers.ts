import { ref } from 'vue'

export interface Wallpaper {
  name: string
  icon: string
  bg: string
}

export const wallpapers: Wallpaper[] = [
  { name: '樱花粉', icon: '🌸', bg: 'linear-gradient(135deg, #fce7f3, #fbcfe8)' },
  { name: '海洋蓝', icon: '🌊', bg: 'linear-gradient(135deg, #dbeafe, #bfdbfe)' },
  { name: '薄荷绿', icon: '🌿', bg: 'linear-gradient(135deg, #d1fae5, #a7f3d0)' },
  { name: '落日橙', icon: '🌇', bg: 'linear-gradient(135deg, #ffedd5, #fed7aa)' },
  { name: '薰衣草', icon: '💜', bg: 'linear-gradient(135deg, #ede9fe, #ddd6fe)' },
  { name: '星空夜', icon: '✨', bg: 'linear-gradient(135deg, #1e1b4b, #312e81)' }
]

export const WALL_KEY = 'vl-wallpaper'

// -1 表示默认背景；App 负责应用，Profile 负责选择
export const wallIndex = ref<number>(readWallIndex())

function readWallIndex(): number {
  const raw = localStorage.getItem(WALL_KEY)
  if (raw === null) return -1
  const n = Number(raw)
  return Number.isInteger(n) && n >= 0 && n < wallpapers.length ? n : -1
}

export function applyWallpaper() {
  const el = document.documentElement
  if (wallIndex.value >= 0) {
    el.style.setProperty('--vl-wallpaper', wallpapers[wallIndex.value].bg)
  } else {
    el.style.removeProperty('--vl-wallpaper')
  }
}

export function pickWallpaper(i: number) {
  wallIndex.value = i
  localStorage.setItem(WALL_KEY, String(i))
  applyWallpaper()
}
