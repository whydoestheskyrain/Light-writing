<template>
  <div class="profile-page">
    <!-- 左侧 tabs -->
    <aside class="tabs-col">
      <div class="tabs-title">个人中心</div>
      <ul class="tabs">
        <li
          v-for="t in tabs"
          :key="t.key"
          class="tab-item"
          :class="{ active: activeTab === t.key }"
          @click="activeTab = t.key"
        >
          <span class="tab-ico">{{ t.icon }}</span>
          <span>{{ t.label }}</span>
        </li>
      </ul>
    </aside>

    <!-- 右侧内容 -->
    <main class="content-col">
      <!-- 个人资料 + 学习数据（合并） -->
      <section v-if="activeTab === 'profile'" class="section">
        <div class="section-head">
          <h2>个人资料</h2>
          <p>编辑你的昵称和签名，查看学习数据</p>
        </div>

        <div class="card profile-card">
          <div class="avatar-big">
            <span>{{ (form.name || 'L').charAt(0).toUpperCase() }}</span>
          </div>
          <div class="form-col">
            <label>昵称</label>
            <input v-model="form.name" type="text" maxlength="20" placeholder="学习者" />
            <label>签名</label>
            <input v-model="form.signature" type="text" maxlength="50" placeholder="今天也要加油学习" />
            <div class="form-actions">
              <button class="btn-ghost" @click="resetProfile">恢复默认</button>
              <button class="btn-primary" @click="saveProfile">保存</button>
            </div>
          </div>
        </div>

        <!-- 三个统计卡 -->
        <div class="stat-cards">
          <div class="stat-card">
            <div class="stat-label">今日学习</div>
            <div class="stat-value">{{ study.format(study.todaySeconds) }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">累计学习</div>
            <div class="stat-value">{{ study.format(study.totalSeconds) }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">连续打卡</div>
            <div class="stat-value">{{ study.streakDays }} <span class="stat-unit">天</span></div>
          </div>
        </div>

        <!-- 最近 7 天柱状图 -->
        <div class="card chart-card">
          <div class="card-title">最近 7 天</div>
          <div class="bar-chart">
            <div v-for="d in weekData" :key="d.date" class="bar-col">
              <div class="bar-min">{{ d.seconds >= 60 ? Math.floor(d.seconds / 60) + '分' : (d.seconds > 0 ? d.seconds + '秒' : '') }}</div>
              <div class="bar-track">
                <div class="bar-fill" :style="{ height: barHeight(d.seconds) }"></div>
              </div>
              <div class="bar-label">{{ d.label }}</div>
            </div>
          </div>
        </div>

        <!-- 各模块时间占比（环形饼图） -->
        <div class="card">
          <div class="pie-head">
            <div class="card-title">模块时间分布</div>
            <div class="range-switch">
              <button
                v-for="r in pieRanges" :key="r.key"
                class="range-pill"
                :class="{ active: pieRange === r.key }"
                @click="pieRange = r.key"
              >{{ r.label }}</button>
            </div>
          </div>
          <div v-if="moduleStats.length" ref="pieRef" class="pie-chart"></div>
          <div v-else class="empty-tip">
            {{ pieRange === 'all'
              ? '还没有按模块记录的时长 — 进入任意模块页学习一会儿，这里就会出现数据'
              : (pieRange === 'today' ? '今天还没有按模块记录的时长 — 进入任意模块页学习一会儿再来看看' : '最近 7 天还没有按模块记录的时长') }}
          </div>
        </div>

        <!-- 最近编辑 -->
        <div class="card">
          <div class="card-title">最近编辑</div>
          <ul v-if="recentEntries.length" class="recent-list">
            <li v-for="e in recentEntries" :key="e.id" class="recent-row" @click="goEntry(e.id)">
              <span
                class="type-pill"
                :style="{ background: typeMeta(e.type).color + '1a', color: typeMeta(e.type).color }"
              >{{ typeMeta(e.type).label }}</span>
              <span class="r-title">{{ e.title }}</span>
              <span class="r-mod">
                <i class="dot" :style="{ background: modMeta(e.moduleId).color }"></i>
                {{ modMeta(e.moduleId).name }}
              </span>
              <span class="r-time">{{ relTime(e.updateTime || e.createTime) }}</span>
            </li>
          </ul>
          <div v-else class="empty-tip">还没有条目 — 去模块里创建第一条笔记吧</div>
        </div>
      </section>

      <!-- 设置 -->
      <section v-if="activeTab === 'settings'" class="section">
        <div class="section-head">
          <h2>设置</h2>
          <p>一些关于体验的小开关</p>
        </div>

        <div class="settings-list">
          <div class="setting-row">
            <div>
              <div class="s-label">深色模式</div>
              <div class="s-desc">切换网站整体主题色</div>
            </div>
            <button
              class="toggle"
              :class="{ on: isDark }"
              @click="toggleDark"
            >
              <span class="dot"></span>
            </button>
          </div>
          <div class="setting-row">
            <div>
              <div class="s-label">音乐悬浮器</div>
              <div class="s-desc">在右下角显示迷你播放器</div>
            </div>
            <button class="toggle on" disabled>
              <span class="dot"></span>
            </button>
          </div>
          <div class="setting-row">
            <div>
              <div class="s-label">数据缓存</div>
              <div class="s-desc">5 分钟内重复打开模块不重新请求</div>
            </div>
            <span class="s-tag">默认开启</span>
          </div>
          <div class="setting-row">
            <div>
              <div class="s-label">数据备份</div>
              <div class="s-desc">导出学习记录与资料为 JSON 文件，或从备份恢复</div>
            </div>
            <div class="backup-actions">
              <button class="btn-ghost sm" @click="exportData">导出</button>
              <button class="btn-ghost sm" @click="triggerImport">导入</button>
              <input ref="importFile" type="file" accept=".json,application/json" hidden @change="onImportFile" />
            </div>
          </div>
          <div class="setting-row danger" @click="handleLogout">
            <div>
              <div class="s-label">退出登录</div>
              <div class="s-desc">本地学习模式，无需真正退出</div>
            </div>
            <span class="s-arrow">›</span>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { useStudyStore } from '@/stores/study'

echarts.use([PieChart, TooltipComponent, LegendComponent, TitleComponent, CanvasRenderer])
import { moduleApi, entryApi } from '@/api'
import { wallpapers, wallIndex, pickWallpaper } from '@/config/wallpapers'
import type { Module, Entry } from '@/types'

const study = useStudyStore()
const router = useRouter()
const route = useRoute()

// ============ 学习数据 ============
interface ModuleStat { id: string; name: string; color: string; seconds: number; percent: number }
type PieRange = 'today' | 'week' | 'all'

const weekData = computed(() => study.last7Days())
const moduleStats = ref<ModuleStat[]>([])
const pieRange = ref<PieRange>('all')
const pieRanges: { key: PieRange; label: string }[] = [
  { key: 'today', label: '今日' },
  { key: 'week', label: '本周' },
  { key: 'all', label: '全部' }
]

const pieRef = ref<HTMLElement | null>(null)
let pieChart: echarts.ECharts | null = null

function renderPie() {
  if (!pieRef.value || !moduleStats.value.length) return
  if (!pieChart) pieChart = echarts.init(pieRef.value)
  const totalSec = moduleStats.value.reduce((s, m) => s + m.seconds, 0)
  pieChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (p: any) => `${p.name}<br/>${study.format(p.value)}（${p.percent}%）`
    },
    legend: {
      bottom: 0,
      left: 'center',
      icon: 'circle',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: { fontSize: 12, color: '#6b6b76' }
    },
    title: {
      text: study.format(totalSec),
      subtext: '总时长',
      left: 'center',
      top: '34%',
      itemGap: 4,
      textStyle: { fontSize: 18, fontWeight: 700, color: '#1d1d24' },
      subtextStyle: { fontSize: 12, color: '#9a9aa5' }
    },
    series: [{
      type: 'pie',
      radius: ['48%', '72%'],
      center: ['50%', '42%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{d}%', fontSize: 12, color: '#6b6b76' },
      emphasis: { scaleSize: 6, label: { fontWeight: 600 } },
      data: moduleStats.value.map(m => ({
        name: m.name,
        value: m.seconds,
        itemStyle: { color: m.color }
      }))
    }]
  }, true)
}

function resizePie() { pieChart?.resize() }

const maxWeekSeconds = computed(() => Math.max(...weekData.value.map(d => d.seconds), 1))
const barHeight = (sec: number) => {
  if (!sec) return '4px'
  return Math.max(8, Math.round((sec / maxWeekSeconds.value) * 100)) + '%'
}

async function loadModuleStats() {
  const ranking = study.moduleRankingRange(pieRange.value)
  if (!ranking.length) { moduleStats.value = []; return }
  try {
    const res = await moduleApi.list()
    const mods: any[] = res.data || []
    const totalSec = ranking.reduce((s, r) => s + r.seconds, 0)
    moduleStats.value = ranking
      .map(r => {
        const mod = mods.find(m => String(m.id) === String(r.id))
        return {
          id: r.id,
          name: mod?.name || `模块 #${r.id}`,
          color: mod?.color || '#ea4c89',
          seconds: r.seconds,
          percent: Math.round((r.seconds / totalSec) * 100)
        }
      })
  } catch {
    moduleStats.value = ranking.map(r => ({
      id: r.id, name: `模块 #${r.id}`, color: '#ea4c89', seconds: r.seconds,
      percent: Math.round((r.seconds / ranking.reduce((s, x) => s + x.seconds, 0)) * 100)
    }))
  }
}

const PROFILE_KEY = 'vl-profile'
const DARK_KEY = 'vl-dark'

type TabKey = 'profile' | 'settings'

const tabs: { key: TabKey; label: string; icon: string }[] = [
  { key: 'profile', label: '个人资料', icon: '👤' },
  { key: 'settings', label: '设置', icon: '⚙️' }
]

const activeTab = ref<TabKey>('profile')

watch(activeTab, (t) => {
  if (t === 'profile') { loadModuleStats(); loadRecentEntries() }
}, { immediate: true })

watch(pieRange, () => { loadModuleStats() })

// 数据或 tab 就绪后渲染饼图
watch([activeTab, moduleStats], async () => {
  if (activeTab.value !== 'profile') return
  await nextTick()
  renderPie()
}, { deep: true })

onMounted(() => window.addEventListener('resize', resizePie))
onBeforeUnmount(() => {
  window.removeEventListener('resize', resizePie)
  pieChart?.dispose()
  pieChart = null
})

// ============ 资料 ============
const form = reactive({ name: '学习者', signature: '今天也要加油学习' })
const defaultProfile = { name: '学习者', signature: '今天也要加油学习' }

onMounted(() => {
  try {
    const raw = localStorage.getItem(PROFILE_KEY)
    if (raw) Object.assign(form, JSON.parse(raw))
  } catch { /* ignore */ }
  // 支持 /profile?tab=settings 直达设置
  const qtab = String(route.query.tab || '') as TabKey
  if (tabs.some(t => t.key === qtab)) activeTab.value = qtab
})

const saveProfile = () => {
  localStorage.setItem(PROFILE_KEY, JSON.stringify(form))
  ElMessage.success('已保存')
}
const resetProfile = () => {
  Object.assign(form, defaultProfile)
  localStorage.setItem(PROFILE_KEY, JSON.stringify(form))
  ElMessage.success('已恢复默认')
}

// ============ 壁纸 ============
const pickedWall = wallIndex
const pickWall = pickWallpaper

// ============ 最近编辑 ============
interface RecentEntry {
  id: number; moduleId: number; type: string; title: string
  status: number; updateTime: string; createTime: string
}
const recentEntries = ref<RecentEntry[]>([])
const moduleList = ref<Module[]>([])

async function loadRecentEntries() {
  try {
    const [eRes, mRes] = await Promise.all([entryApi.list({}), moduleApi.list()])
    moduleList.value = mRes.data || []
    const list: Entry[] = eRes.data?.records || []
    recentEntries.value = list
      .slice()
      .sort((a, b) =>
        String(b.updateTime || b.createTime).localeCompare(String(a.updateTime || a.createTime))
      )
      .slice(0, 8)
  } catch {
    recentEntries.value = []
  }
}

const typeMeta = (t: string) =>
  ({ note: { label: '笔记', color: '#4f8cff' }, algorithm: { label: '算法', color: '#ea4c89' }, qa: { label: '八股', color: '#7c5cff' }, link: { label: '链接', color: '#0bb8b8' } } as Record<string, { label: string; color: string }>)[t]
  || { label: t, color: '#6e6d7a' }

const modMeta = (id: number) => {
  const m = moduleList.value.find(x => String(x.id) === String(id))
  return { name: m?.name || `模块 #${id}`, color: m?.color || '#ea4c89' }
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

const goEntry = (id: number) => router.push(`/entry/${id}`)

// ============ 数据导入导出 ============
const importFile = ref<HTMLInputElement | null>(null)
const triggerImport = () => importFile.value?.click()

async function exportData() {
  try {
    const [mRes, eRes] = await Promise.all([moduleApi.list(), entryApi.list({})])
    const payload = {
      app: 'light-study',
      version: 1,
      exportedAt: new Date().toISOString(),
      prefs: {
        profile: localStorage.getItem('vl-profile'),
        wallpaper: localStorage.getItem('vl-wallpaper'),
        dark: localStorage.getItem('vl-dark'),
        musicVolume: localStorage.getItem('vl-music-volume'),
        musicMode: localStorage.getItem('vl-music-mode')
      },
      studyRecords: JSON.parse(localStorage.getItem('vl-study-records') || '{}'),
      modules: mRes.data || [],
      entries: eRes.data?.records || []
    }
    const blob = new Blob([JSON.stringify(payload, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `light-backup-${new Date().toISOString().slice(0, 10)}.json`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('备份已导出')
  } catch {
    ElMessage.error('导出失败，请重试')
  }
}

async function onImportFile(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return

  let data: any
  try {
    data = JSON.parse(await file.text())
  } catch {
    ElMessage.error('文件解析失败，请选择有效的 JSON 备份')
    return
  }
  if (data?.app !== 'light-study') {
    ElMessage.error('不是有效的 Light 备份文件')
    return
  }

  // 1. 恢复本地偏好与学习记录
  const p = data.prefs || {}
  if (p.profile) localStorage.setItem('vl-profile', p.profile)
  if (p.dark != null) localStorage.setItem('vl-dark', p.dark)
  if (p.musicVolume) localStorage.setItem('vl-music-volume', p.musicVolume)
  if (p.musicMode) localStorage.setItem('vl-music-mode', p.musicMode)
  if (p.wallpaper != null) pickWallpaper(Number(p.wallpaper))
  if (data.studyRecords) localStorage.setItem('vl-study-records', JSON.stringify(data.studyRecords))

  // 2. 可选：合并备份中的模块与条目到数据库
  let createdEntries = 0
  let newModules = 0
  const mods: any[] = data.modules || []
  const entries: any[] = data.entries || []
  if (mods.length || entries.length) {
    let doMerge = false
    try {
      await ElMessageBox.confirm(
        '备份中包含模块与条目资料，是否合并导入到数据库？（同名模块/同标题条目会跳过）',
        '导入资料',
        { confirmButtonText: '导入', cancelButtonText: '跳过', type: 'info' }
      )
      doMerge = true
    } catch { doMerge = false }

    if (doMerge) {
      try {
        const curRes = await moduleApi.list()
        const byName = new Map<string, any>((curRes.data || []).map(m => [m.name, m]))
        for (const m of mods) {
          if (byName.has(m.name)) continue
          await moduleApi.create({
            name: m.name, icon: m.icon, color: m.color,
            coverImage: m.coverImage, description: m.description, sortOrder: m.sortOrder
          })
          newModules++
        }
        // 重新拉取，建立 旧模块id → 现模块id 映射
        const freshRes = await moduleApi.list()
        const freshByName = new Map<string, any>((freshRes.data || []).map(m => [m.name, m]))
        const idMap = new Map<number, number>()
        for (const m of mods) {
          const cur = freshByName.get(m.name)
          if (cur) idMap.set(m.id, cur.id)
        }
        const curERes = await entryApi.list({})
        const existKey = new Set((curERes.data?.records || []).map((x: Entry) => `${x.moduleId}|${x.title}`))
        for (const en of entries) {
          const mid = idMap.get(en.moduleId)
          if (!mid || existKey.has(`${mid}|${en.title}`)) continue
          await entryApi.create({
            moduleId: mid, type: en.type, title: en.title, contentMd: en.contentMd,
            dataJson: en.dataJson, status: en.status, tags: en.tags, nextReviewTime: en.nextReviewTime
          })
          createdEntries++
        }
      } catch {
        ElMessage.warning('资料合并部分失败，偏好与学习记录已恢复')
      }
    }
  }

  ElMessage.success(
    `导入完成${newModules ? `，新建模块 ${newModules} 个` : ''}${createdEntries ? `，新建条目 ${createdEntries} 条` : ''}，即将刷新页面`
  )
  setTimeout(() => location.reload(), 1200)
}

// ============ 设置 ============
const isDark = ref(document.documentElement.classList.contains('dark'))
const toggleDark = () => {
  isDark.value = !isDark.value
  document.documentElement.classList.toggle('dark', isDark.value)
  localStorage.setItem(DARK_KEY, isDark.value ? '1' : '0')
}
const handleLogout = () => ElMessage.info('本地学习模式，无需退出登录 🎉')
</script>

<style scoped lang="scss">
.profile-page {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 40px;
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 24px;
  min-height: calc(100vh - 83px);
}

// ============ 左侧 tabs ============
.tabs-col {
  .tabs-title {
    font-size: 13px;
    font-weight: 600;
    color: $muted;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    margin-bottom: 20px;
  }
}
.tabs {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.tab-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  border-radius: $radius-md;
  font-size: 15px;
  font-weight: 500;
  color: $muted;
  cursor: pointer;
  transition: background $dur-fast, color $dur-fast;

  .tab-ico { font-size: 18px; }
  &:hover { background: $bg-subtle; color: $ink; }
  &.active { background: $bg-subtle; color: $ink; font-weight: 600; }
}

// ============ 右侧内容 ============
.content-col { min-width: 0; }

.section-head { margin-bottom: 28px; }
h2 {
  font-size: 26px;
  font-weight: 700;
  color: $ink;
  margin: 0 0 6px;
  letter-spacing: -0.02em;
}
.section-head p { font-size: 14px; color: $faint; margin: 0; }

.card {
  background: $bg-card;
  border: 1px solid $border-soft;
  border-radius: $radius-lg;
  padding: 24px;
  margin-bottom: 24px;
}

// 资料卡
.profile-card {
  display: flex;
  gap: 32px;
  align-items: flex-start;
}
.avatar-big {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: linear-gradient(135deg, #a855f7, #7c3aed);
  color: #fff;
  font-size: 36px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.form-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;

  label {
    font-size: 12px;
    font-weight: 600;
    color: $muted;
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }
  input {
    height: 44px;
    padding: 0 16px;
    border: 1px solid $border;
    border-radius: $radius-md;
    background: $bg-card;
    font-size: 15px;
    color: $ink;
    outline: none;
    transition: border-color $dur-fast, box-shadow $dur-fast;
    &:focus { border-color: $primary; box-shadow: 0 0 0 3px rgba($primary, 0.15); }
  }
}
.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 8px;
}
.btn-primary {
  height: 40px;
  padding: 0 24px;
  border: none;
  border-radius: $radius-pill;
  background: $ink;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background $dur-fast;
  &:hover { background: $ink-soft; }
}
.btn-ghost {
  height: 40px;
  padding: 0 20px;
  border: 1px solid $border;
  border-radius: $radius-pill;
  background: transparent;
  color: $muted;
  font-size: 14px;
  cursor: pointer;
  transition: color $dur-fast, border-color $dur-fast;
  &:hover { color: $ink; border-color: $ink; }
}

// 壁纸网格
.wall-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}
.wall-card {
  aspect-ratio: 4 / 3;
  border-radius: $radius-md;
  cursor: pointer;
  position: relative;
  display: flex;
  align-items: flex-end;
  padding: 14px;
  overflow: hidden;
  border: 2px solid transparent;
  transition: transform $dur-fast, border-color $dur-fast;

  &:hover { transform: translateY(-2px); }
  &.picked { border-color: $primary; }

  .wall-ico {
    position: absolute;
    top: 14px;
    right: 14px;
    font-size: 28px;
  }
  .wall-name {
    font-size: 14px;
    font-weight: 600;
    color: #fff;
    text-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
  }
}

.wall-default {
  background: repeating-conic-gradient($bg-subtle 0% 25%, $bg-card 0% 50%) 50% / 16px 16px;
  border: 2px dashed $border;

  .wall-name-ink {
    color: $muted;
    text-shadow: none;
  }
}

.backup-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.btn-ghost.sm {
  height: 34px;
  padding: 0 16px;
  font-size: 13px;
}

// 设置列表
.settings-list {
  background: $bg-card;
  border: 1px solid $border-soft;
  border-radius: $radius-lg;
  overflow: hidden;
}
.setting-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid $border-soft;
  &:last-child { border-bottom: none; }
  &:hover { background: rgba($primary, 0.02); }
  &.danger { cursor: pointer; .s-label { color: $danger; } }
}
.s-label { font-size: 15px; font-weight: 600; color: $ink; }
.s-desc { font-size: 13px; color: $faint; margin-top: 4px; }
.s-tag { font-size: 12px; color: $faint; background: $bg-subtle; padding: 4px 10px; border-radius: $radius-pill; }
.s-arrow { font-size: 22px; color: $faint; }

.toggle {
  width: 44px;
  height: 24px;
  border: none;
  border-radius: 999px;
  background: $border;
  position: relative;
  cursor: pointer;
  padding: 0;
  transition: background $dur-fast;
  &.on { background: $primary; }
  .dot {
    position: absolute;
    top: 2px;
    left: 2px;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    background: #fff;
    transition: transform $dur-base;
    box-shadow: $shadow-sm;
  }
  &.on .dot { transform: translateX(20px); }
  &[disabled] { cursor: not-allowed; opacity: 0.6; }
}

// ============ 学习数据 ============
.stat-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}
.stat-card {
  background: $bg-card;
  border: 1px solid $border-soft;
  border-radius: $radius-lg;
  padding: 24px;
}
.stat-label {
  font-size: 13px;
  color: $faint;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: $ink;
  letter-spacing: -0.02em;
}
.stat-unit { font-size: 14px; font-weight: 500; color: $muted; }

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: $ink;
  margin-bottom: 20px;
}

.bar-chart {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  height: 160px;
}
.bar-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
}
.bar-min {
  font-size: 11px;
  color: $faint;
  min-height: 16px;
}
.bar-track {
  flex: 1;
  width: 100%;
  max-width: 36px;
  background: $bg-subtle;
  border-radius: $radius-sm;
  display: flex;
  align-items: flex-end;
  overflow: hidden;
}
.bar-fill {
  width: 100%;
  background: linear-gradient(180deg, $primary, #7c3aed);
  border-radius: $radius-sm;
  transition: height $dur-slow ease;
  min-height: 4px;
}
.bar-label {
  font-size: 12px;
  color: $muted;
  margin-top: 8px;
}

// 模块时间分布：环形饼图
.pie-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
}
.range-switch {
  display: flex;
  gap: 4px;
  background: $bg-subtle;
  border-radius: $radius-pill;
  padding: 3px;
}
.range-pill {
  border: none;
  background: transparent;
  border-radius: $radius-pill;
  padding: 4px 14px;
  font-size: 12px;
  font-weight: 500;
  color: $muted;
  cursor: pointer;
  transition: background $dur-fast, color $dur-fast;

  &.active {
    background: $bg-card;
    color: $ink;
    font-weight: 600;
    box-shadow: $shadow-card;
  }
}
.pie-chart {
  height: 320px;
  margin-top: 8px;
}

.empty-tip {
  font-size: 14px;
  color: $faint;
  padding: 12px 0;
}

// 最近编辑列表
.recent-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}
.recent-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 8px;
  border-radius: $radius-sm;
  cursor: pointer;
  transition: background $dur-fast;

  &:hover { background: $bg-subtle; }

  .type-pill {
    padding: 3px 10px;
    border-radius: $radius-pill;
    font-size: 12px;
    font-weight: 600;
    flex-shrink: 0;
  }
  .r-title {
    flex: 1;
    min-width: 0;
    font-size: 14px;
    font-weight: 500;
    color: $ink;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .r-mod {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    font-size: 12.5px;
    color: $faint;
    flex-shrink: 0;
    max-width: 140px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;

    .dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      flex-shrink: 0;
    }
  }
  .r-time {
    font-size: 12px;
    color: $faint;
    flex-shrink: 0;
    font-variant-numeric: tabular-nums;
    width: 76px;
    text-align: right;
  }
}

// ============ 暗色模式 ============
:global(.dark) .section-head h2 { color: $dark-ink; }
:global(.dark) .tab-item { color: $dark-muted; &:hover, &.active { background: $dark-border; color: $dark-ink; } }
:global(.dark) .card { background: $dark-card; border-color: $dark-border; }
:global(.dark) .form-col input { background: $dark-card; border-color: $dark-border; color: $dark-ink; }
:global(.dark) .settings-list { background: $dark-card; border-color: $dark-border; }
:global(.dark) .setting-row { border-bottom-color: $dark-border; &:hover { background: rgba($primary, 0.08); } }
:global(.dark) .s-label { color: $dark-ink; }
:global(.dark) .recent-row {
  &:hover { background: $dark-border; }
  .r-title { color: $dark-ink; }
}
:global(.dark) .wall-default {
  background: repeating-conic-gradient($dark-border 0% 25%, $dark-card 0% 50%) 50% / 16px 16px;
  border-color: $dark-border;
  .wall-name-ink { color: $dark-muted; }
}

// ============ 响应式 ============
@media (max-width: 768px) {
  .profile-page {
    grid-template-columns: 1fr;
    gap: 20px;
    padding: 20px 16px;
    min-height: calc(100vh - var(--vl-header-h, 60px));
  }
  // 左侧 tabs → 顶部横向滚动条
  .tabs-col { min-width: 0; }
  .tabs-title { display: none; }
  .tabs {
    flex-direction: row;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    gap: 6px;
  }
  .tab-item { flex-shrink: 0; white-space: nowrap; padding: 8px 14px; }
  .profile-card { flex-direction: column; align-items: center; text-align: center; }
}

@media (max-width: 640px) {
  h2 { font-size: 21px; }
  .card { padding: 16px; }
  .stat-card { padding: 16px; }
  .stat-value { font-size: 20px; }
  // 7 天柱状图压缩间距
  .bar-chart { gap: 4px; height: 140px; }
  .bar-label { font-size: 10.5px; }
  // 模块分布与最近编辑的次要列收敛
  .pie-chart { height: 260px; }
  .range-pill { padding: 4px 11px; }
  .recent-row .r-mod { display: none; }
  // 设置行允许换行，备份按钮不被挤掉
  .setting-row { flex-wrap: wrap; gap: 8px; }
  .backup-actions { width: 100%; justify-content: flex-end; }
}
</style>
