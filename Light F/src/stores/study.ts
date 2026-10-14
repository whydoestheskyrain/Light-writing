import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { learningApi } from '@/api'

export interface DayRecord {
  total: number // 秒
  modules: Record<string, number> // moduleId -> 秒
}

const KEY = 'vl-study-records'
const HEARTBEAT = 15 // 每 15 秒记一次

function todayKey(): string {
  const d = new Date()
  const m = (d.getMonth() + 1).toString().padStart(2, '0')
  const day = d.getDate().toString().padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

function load(): Record<string, DayRecord> {
  try {
    return JSON.parse(localStorage.getItem(KEY) || '{}')
  } catch {
    return {}
  }
}

export const useStudyStore = defineStore('study', () => {
  const records = ref<Record<string, DayRecord>>(load())
  // 当前学习上下文：进入模块/条目详情页时设置 moduleId
  const currentModuleId = ref('')

  let timer: ReturnType<typeof setInterval> | null = null

  function persist() {
    localStorage.setItem(KEY, JSON.stringify(records.value))
  }

  function ensure(key: string): DayRecord {
    if (!records.value[key]) records.value[key] = { total: 0, modules: {} }
    return records.value[key]
  }

  // 心跳：页面可见时每 HEARTBEAT 秒累计一次
  function heartbeat() {
    if (document.visibilityState !== 'visible') return
    const rec = ensure(todayKey())
    rec.total += HEARTBEAT
    if (currentModuleId.value) {
      rec.modules[currentModuleId.value] = (rec.modules[currentModuleId.value] || 0) + HEARTBEAT
    }
    persist()
    learningApi.heartbeat({
      eventId: `${todayKey()}-${Date.now()}-${Math.random().toString(36).slice(2)}`,
      seconds: HEARTBEAT,
      moduleId: currentModuleId.value ? Number(currentModuleId.value) : undefined
    }).catch(() => { /* 后端暂不可用时保留本地记录 */ })
  }

  function start() {
    if (timer) return
    timer = setInterval(heartbeat, HEARTBEAT * 1000)
  }

  function setModule(id: string) {
    currentModuleId.value = id
  }

  // ============ 统计数据 ============
  const todaySeconds = computed(() => records.value[todayKey()]?.total ?? 0)

  const totalSeconds = computed(() => {
    let sum = 0
    for (const k in records.value) sum += records.value[k].total
    return sum
  })

  // 连续学习天数（今天或昨天为起点往前数）
  const streakDays = computed(() => {
    let count = 0
    const d = new Date()
    // 今天没学不算断签，从昨天开始数
    if (!records.value[todayKey()]?.total) d.setDate(d.getDate() - 1)
    for (;;) {
      const m = (d.getMonth() + 1).toString().padStart(2, '0')
      const day = d.getDate().toString().padStart(2, '0')
      const key = `${d.getFullYear()}-${m}-${day}`
      if (records.value[key]?.total) {
        count++
        d.setDate(d.getDate() - 1)
      } else break
    }
    return count
  })

  // 最近 7 天 [{date, seconds}]
  function last7Days(): { date: string; label: string; seconds: number }[] {
    const out: { date: string; label: string; seconds: number }[] = []
    const week = ['日', '一', '二', '三', '四', '五', '六']
    for (let i = 6; i >= 0; i--) {
      const d = new Date()
      d.setDate(d.getDate() - i)
      const m = (d.getMonth() + 1).toString().padStart(2, '0')
      const day = d.getDate().toString().padStart(2, '0')
      const key = `${d.getFullYear()}-${m}-${day}`
      out.push({
        date: key,
        label: i === 0 ? '今天' : `周${week[d.getDay()]}`,
        seconds: records.value[key]?.total ?? 0
      })
    }
    return out
  }

  // 各模块累计秒数（降序）— 全量
  function moduleRanking(): { id: string; seconds: number }[] {
    return moduleRankingRange('all')
  }

  // 各模块秒数按时间范围聚合（降序）：today 今日 / week 最近 7 天 / all 全部
  function moduleRankingRange(range: 'today' | 'week' | 'all'): { id: string; seconds: number }[] {
    const map: Record<string, number> = {}
    if (range === 'today') {
      const mods = records.value[todayKey()]?.modules
      if (mods) for (const mid in mods) map[mid] = mods[mid]
    } else if (range === 'week') {
      for (let i = 0; i < 7; i++) {
        const d = new Date()
        d.setDate(d.getDate() - i)
        const m = (d.getMonth() + 1).toString().padStart(2, '0')
        const day = d.getDate().toString().padStart(2, '0')
        const mods = records.value[`${d.getFullYear()}-${m}-${day}`]?.modules
        if (mods) for (const mid in mods) map[mid] = (map[mid] || 0) + mods[mid]
      }
    } else {
      for (const k in records.value) {
        for (const mid in records.value[k].modules) {
          map[mid] = (map[mid] || 0) + records.value[k].modules[mid]
        }
      }
    }
    return Object.entries(map)
      .map(([id, seconds]) => ({ id, seconds }))
      .sort((a, b) => b.seconds - a.seconds)
  }

  function format(sec: number): string {
    if (sec < 60) return `${sec} 秒`
    const h = Math.floor(sec / 3600)
    const m = Math.floor((sec % 3600) / 60)
    if (h > 0) return `${h} 小时 ${m} 分钟`
    return `${m} 分钟`
  }

  return {
    records,
    currentModuleId,
    start,
    setModule,
    todaySeconds,
    totalSeconds,
    streakDays,
    last7Days,
    moduleRanking,
    moduleRankingRange,
    format,
    todayKey
  }
})
