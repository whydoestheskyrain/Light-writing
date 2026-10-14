import { defineStore } from 'pinia'
import { moduleApi, entryApi } from '@/api'
import type { Module } from '@/types'

export const useAppStore = defineStore('app', {
  state: () => ({
    modules: [] as Module[],
    loading: false,
    // 各模块条目统计：moduleId -> { total 条目数, done 已掌握数 }
    entryStats: {} as Record<string, { total: number; done: number }>,
    // 导航栏“新建模块”按钮的触发信号
    createSignal: 0
  }),
  actions: {
    async fetchModules() {
      this.loading = true
      try {
        const res = await moduleApi.list()
        this.modules = res.data
        // 顺带统计每个模块的条目数与已掌握数（条目量级小，一次全量拉取）
        try {
          const eRes = await entryApi.list({})
          const stats: Record<string, { total: number; done: number }> = {}
          for (const e of eRes.data?.records || []) {
            const key = String(e.moduleId)
            const s = (stats[key] ??= { total: 0, done: 0 })
            s.total++
            if (e.status === 2) s.done++
          }
          this.entryStats = stats
        } catch { /* 统计失败不影响模块列表展示 */ }
      } finally {
        this.loading = false
      }
    },
    bumpCreate() {
      this.createSignal++
    }
  }
})
