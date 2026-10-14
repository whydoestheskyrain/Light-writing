<template>
  <div class="search-page">
    <header class="page-head">
      <h1>搜索结果</h1>
      <p v-if="keyword">
        关键词「<span class="kw">{{ keyword }}</span>」· 找到 {{ results.length }} 条
      </p>
    </header>

    <div v-if="loading" class="state-tip">
      <div v-for="n in 4" :key="n" class="skeleton search-skeleton"></div>
    </div>

    <ul v-else-if="results.length" class="result-list">
      <li
        v-for="e in results"
        :key="e.id"
        class="result-row"
        @click="router.push(`/entry/${e.id}`)"
      >
        <span class="type-pill" :style="{ background: typeMeta(e.type).color + '1a', color: typeMeta(e.type).color }">
          {{ typeMeta(e.type).label }}
        </span>
        <div class="r-main">
          <div class="r-title">{{ e.title }}</div>
          <div class="r-meta">
            <span class="r-module">
              <span class="dot" :style="{ background: moduleMeta(e.moduleId).color }"></span>
              {{ moduleMeta(e.moduleId).name }}
            </span>
            <span class="r-time">编辑于 {{ relTime(e.updateTime || e.createTime) }}</span>
          </div>
        </div>
        <span class="r-status" :style="{ color: statusMeta[e.status]?.color }">
          <i class="s-dot" :style="{ background: statusMeta[e.status]?.color }"></i>
          {{ statusMeta[e.status]?.label || '未做' }}
        </span>
        <span class="r-arrow">›</span>
      </li>
    </ul>

    <div v-else class="state-tip">
      <div class="state-illu">
        <svg viewBox="0 0 120 100" fill="none" xmlns="http://www.w3.org/2000/svg">
          <ellipse cx="60" cy="86" rx="40" ry="6" fill="#f4f4f6" />
          <circle cx="52" cy="42" r="24" fill="#fff" stroke="#e7e7e9" stroke-width="2.5" />
          <line x1="70" y1="60" x2="88" y2="78" stroke="#ea4c89" stroke-width="4" stroke-linecap="round" />
          <line x1="42" y1="38" x2="58" y2="38" stroke="#f0f0f0" stroke-width="3" stroke-linecap="round" />
          <line x1="42" y1="48" x2="54" y2="48" stroke="#f0f0f0" stroke-width="3" stroke-linecap="round" />
        </svg>
      </div>
      <p class="state-title">{{ keyword ? '没有找到相关条目' : '开始搜索' }}</p>
      <p class="state-sub">
        {{ keyword ? `没有与「${keyword}」相关的结果，换个关键词试试` : '在顶部搜索框输入关键词后按回车' }}
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { entryApi, moduleApi } from '@/api'
import type { Entry, Module } from '@/types'

const route = useRoute()
const router = useRouter()

const keyword = computed(() => String(route.query.q || '').trim())
const loading = ref(false)
const results = ref<Entry[]>([])
const modules = ref<Module[]>([])

const typeMap: Record<string, { label: string; color: string }> = {
  note: { label: '笔记', color: '#4f8cff' },
  algorithm: { label: '算法', color: '#ea4c89' },
  qa: { label: '八股', color: '#7c5cff' },
  link: { label: '链接', color: '#0bb8b8' }
}
const typeMeta = (t: string) => typeMap[t] || { label: t, color: '#6e6d7a' }

const statusMeta: Record<number, { label: string; color: string }> = {
  0: { label: '未做', color: '#9e9ea7' },
  1: { label: '在做', color: '#f7b500' },
  2: { label: '已掌握', color: '#00b150' }
}

const moduleMeta = (id: string) => {
  const m = modules.value.find(x => x.id === id)
  return { name: m?.name || '未知模块', color: m?.color || '#ea4c89' }
}

const relTime = (t: string) => {
  if (!t) return ''
  const diff = Date.now() - new Date(t.replace(' ', 'T')).getTime()
  const min = Math.floor(diff / 60000)
  if (min < 1) return '刚刚'
  if (min < 60) return `${min} 分钟前`
  const h = Math.floor(min / 60)
  if (h < 24) return `${h} 小时前`
  const d = Math.floor(h / 24)
  if (d < 30) return `${d} 天前`
  return t.slice(0, 10)
}

async function runSearch(q: string) {
  if (!q) { results.value = []; return }
  loading.value = true
  try {
    if (!modules.value.length) {
      const mRes = await moduleApi.list()
      modules.value = mRes.data || []
    }
    const res = await entryApi.list({ keyword: q })
    results.value = res.data?.records || []
  } catch {
    results.value = []
  } finally {
    loading.value = false
  }
}

watch(keyword, (q) => runSearch(q), { immediate: true })
</script>

<style scoped lang="scss">
.search-page {
  max-width: 860px;
  margin: 0 auto;
}

.page-head {
  margin-bottom: 28px;

  h1 {
    font-size: 28px;
    font-weight: 700;
    color: $ink;
    margin: 0 0 8px;
    letter-spacing: -0.02em;
  }
  p {
    font-size: 14px;
    color: $faint;
    margin: 0;
  }
  .kw { color: $primary; font-weight: 600; }
}

.result-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.result-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: $bg-card;
  border: 1px solid $border-soft;
  border-radius: $radius-md;
  cursor: pointer;
  transition: transform $dur-fast, box-shadow $dur-fast, border-color $dur-fast;

  &:hover {
    transform: translateY(-1px);
    border-color: rgba($primary, 0.35);
    box-shadow: $shadow-sm;

    .r-arrow { color: $primary; transform: translateX(3px); }
  }
}

.type-pill {
  padding: 3px 11px;
  border-radius: $radius-pill;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.r-main { flex: 1; min-width: 0; }
.r-title {
  font-size: 15px;
  font-weight: 600;
  color: $ink;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.r-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 5px;
  font-size: 12.5px;
  color: $faint;
}
.r-module {
  display: inline-flex;
  align-items: center;
  gap: 6px;

  .dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
  }
}
.r-time { font-variant-numeric: tabular-nums; }

.r-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12.5px;
  font-weight: 600;
  flex-shrink: 0;

  .s-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }
}

.r-arrow {
  font-size: 22px;
  color: $faint;
  flex-shrink: 0;
  transition: color $dur-fast, transform $dur-fast;
}

.state-tip {
  text-align: center;
  padding: 48px 0 64px;
}

.search-skeleton {
  height: 72px;
  border-radius: $radius-md;
  margin-bottom: 12px;
}

.state-illu {
  width: 120px;
  margin: 0 auto 14px;
}

.state-title {
  font-size: 16.5px;
  font-weight: 700;
  color: $ink;
  margin: 0 0 6px;
}

.state-sub {
  font-size: 13.5px;
  color: $muted;
  margin: 0;
}

// ============ 暗色模式 ============
:global(.dark) .page-head h1 { color: $dark-ink; }
:global(.dark) .result-row {
  background: $dark-card;
  border-color: $dark-border;
  &:hover { border-color: rgba($primary, 0.5); }
}
:global(.dark) .r-title { color: $dark-ink; }

@media (max-width: 640px) {
  .r-status, .r-time { display: none; }
}
</style>
