<template>
  <Teleport to="body">
    <transition name="panel">
      <div v-if="open" ref="panelRef" class="user-panel" :style="panelStyle">
        <!-- ============ 区块 1：用户信息 ============ -->
        <div class="block user-block" @click="goFeature('个人中心')">
          <div class="avatar">{{ avatarText }}</div>
          <div class="user-info">
            <template v-if="!editing">
              <div class="user-name-row">
                <span class="user-name">{{ profile.name }}</span>
                <button class="edit-pencil" @click.stop="startEdit" title="编辑资料">✎</button>
              </div>
              <p class="user-sign">{{ profile.signature }}</p>
            </template>
            <template v-else>
              <el-input
                v-model="draft.name"
                size="small"
                maxlength="12"
                placeholder="昵称"
                @click.stop
              />
              <el-input
                v-model="draft.signature"
                size="small"
                maxlength="30"
                placeholder="一句话签名"
                @click.stop
              />
              <div class="edit-actions" @click.stop>
                <el-button size="small" text @click="editing = false">取消</el-button>
                <el-button size="small" type="primary" @click="saveProfile">保存</el-button>
              </div>
            </template>
          </div>
        </div>

        <div class="divider"></div>

        <!-- ============ 区块 2：学习速览 ============ -->
        <div class="block overview-block" @click="goFeature('统计')">
          <template v-if="stats.total > 0">
            <div class="overview-numbers">
              <div class="big">
                <span class="big-num">{{ stats.mastered }}</span>
                <span class="big-label">已掌握条目</span>
              </div>
              <div class="small-stats">
                <span>共 {{ stats.total }} 条</span>
                <span>在做 {{ stats.doing }} 条</span>
              </div>
            </div>
            <div class="stack-bar" title="各模块条目占比">
              <span
                v-for="seg in stats.segments"
                :key="seg.id"
                class="stack-seg"
                :style="{ flexGrow: seg.count, background: seg.color }"
              ></span>
            </div>
          </template>
          <p v-else class="guide-text">还没有学习数据，创建第一个条目开始吧 →</p>
        </div>

        <div class="divider"></div>

        <!-- ============ 区块 3：今日计划 ============ -->
        <div class="block plan-block">
          <div class="block-title">今日计划</div>
          <template v-if="plans.length">
            <div
              v-for="plan in plans"
              :key="plan.id"
              class="plan-item"
              @click="goEntry(plan.id)"
            >
              <div class="plan-head">
                <span class="plan-name">{{ plan.title }}</span>
                <span class="plan-percent">{{ plan.progress }}%</span>
              </div>
              <div class="progress-bar">
                <span
                  class="progress-fill"
                  :style="{ width: plan.progress + '%' }"
                ></span>
              </div>
              <p class="plan-module">{{ plan.moduleName }}</p>
            </div>
            <button class="view-all" @click="goFeature('计划')">查看全部 →</button>
          </template>
          <p v-else class="guide-text">今天没有待办，去模块里添加条目吧 →</p>
        </div>

        <div class="divider"></div>

        <!-- ============ 区块 4：功能入口 ============ -->
        <div class="block feature-block">
          <button
            v-for="f in features"
            :key="f.name"
            class="feature-item"
            @click="goFeature(f.name)"
          >
            <span class="feature-icon">{{ f.icon }}</span>
            <span class="feature-name">{{ f.name }}</span>
          </button>
        </div>

        <div class="divider"></div>

        <!-- ============ 区块 5：底部操作 ============ -->
        <div class="block bottom-block">
          <button class="logout-btn" @click="handleLogout">
            <span>⏻</span> 退出登录
          </button>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onBeforeUnmount, type PropType } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { moduleApi, entryApi } from '@/api'
import type { Module, Entry } from '@/types'

const props = defineProps({
  open: { type: Boolean, required: true },
  anchor: { type: Object as PropType<HTMLElement | null>, required: true }
})
const emit = defineEmits<{ (e: 'close'): void }>()

const router = useRouter()
const route = useRoute()
const panelRef = ref<HTMLElement | null>(null)

// ============ 用户资料（localStorage 持久化） ============
interface Profile {
  name: string
  signature: string
}
const PROFILE_KEY = 'vl-profile'
const loadProfile = (): Profile => {
  try {
    const raw = localStorage.getItem(PROFILE_KEY)
    if (raw) return { name: '学习者', signature: '今天也要加油学习', ...JSON.parse(raw) }
  } catch (e) {
    /* ignore */
  }
  return { name: '学习者', signature: '今天也要加油学习' }
}
const profile = reactive<Profile>(loadProfile())
const draft = reactive<Profile>({ ...profile })
const editing = ref(false)
const avatarText = computed(() => profile.name.slice(0, 1).toUpperCase())

const startEdit = () => {
  draft.name = profile.name
  draft.signature = profile.signature
  editing.value = true
}
const saveProfile = () => {
  profile.name = draft.name.trim() || '学习者'
  profile.signature = draft.signature.trim() || '今天也要加油学习'
  localStorage.setItem(PROFILE_KEY, JSON.stringify({ ...profile }))
  editing.value = false
}

// ============ 数据聚合（打开时拉取，缓存 5 分钟） ============
interface PlanItem {
  id: number
  title: string
  progress: number
  moduleName: string
  moduleColor: string
}
interface Stats {
  total: number
  mastered: number
  doing: number
  segments: { id: number; count: number; color: string }[]
}
const stats = reactive<Stats>({ total: 0, mastered: 0, doing: 0, segments: [] })
const plans = ref<PlanItem[]>([])
const loading = ref(false)

let cacheTs = 0
const CACHE_TTL = 5 * 60 * 1000

const fetchData = async () => {
  if (loading.value) return
  if (Date.now() - cacheTs < CACHE_TTL && stats.total >= 0 && cacheTs > 0) return
  loading.value = true
  try {
    const modRes = await moduleApi.list()
    const modules: Module[] = modRes.data || []
    const entriesPer = await Promise.all(
      modules.map((m) => entryApi.list({ moduleId: m.id }).then((r) => r.data?.records || []))
    )

    let total = 0
    let mastered = 0
    let doing = 0
    const planList: (PlanItem & { updateTime: string })[] = []

    modules.forEach((m, idx) => {
      const entries: Entry[] = entriesPer[idx] || []
      total += entries.length
      entries.forEach((e) => {
        if (e.status === 2) mastered++
        if (e.status === 1) doing++
        if (e.status !== 2) {
          planList.push({
            id: e.id,
            title: e.title,
            progress: e.status === 1 ? 50 : 0,
            moduleName: m.name,
            moduleColor: m.color || '#ea4c89',
            updateTime: e.updateTime
          })
        }
      })
    })

    stats.total = total
    stats.mastered = mastered
    stats.doing = doing
    stats.segments = modules
      .map((m, idx) => ({ id: m.id, count: (entriesPer[idx] || []).length, color: m.color || '#ea4c89' }))
      .filter((s) => s.count > 0)

    plans.value = planList
      .sort((a, b) => (a.updateTime < b.updateTime ? 1 : -1))
      .slice(0, 3)

    cacheTs = Date.now()
  } catch (e) {
    ElMessage.error('数据加载失败，请确认后端服务已启动')
  } finally {
    loading.value = false
  }
}

// ============ 功能入口 ============
const features = [
  { icon: '📊', name: '统计' },
  { icon: '📝', name: '笔记' },
  { icon: '🎵', name: '音乐盒' },
  { icon: '📅', name: '计划' },
  { icon: '🔁', name: '复习' },
  { icon: '👤', name: '个人中心' },
  { icon: '⚙️', name: '设置' },
  { icon: '🎨', name: '壁纸' }
]

// ============ 跳转 ============
const goFeature = (name: string) => {
  emit('close')
  router.push({ path: '/coming-soon', query: { name } })
}
const goEntry = (id: number) => {
  emit('close')
  router.push(`/entry/${id}`)
}
const handleLogout = () => {
  ElMessage({ message: '当前是本地学习模式，无需退出登录', type: 'info' })
}

// ============ 定位（跟随头像按钮） ============
const panelStyle = ref<Record<string, string>>({})
const updatePosition = () => {
  const anchor = props.anchor
  if (!anchor) return
  const r = anchor.getBoundingClientRect()
  panelStyle.value = {
    top: `${r.bottom + 8}px`,
    right: `${window.innerWidth - r.right}px`
  }
}

// ============ 点击外部 / Esc 关闭 ============
// 注意：不能在 capture 回调里直接 emit('close')，否则 Vue 的离场 transition
// 不会执行（open 已变 false，面板 DOM 却残留）。延迟到本次点击分发结束后再关。
const onDocClick = (e: MouseEvent) => {
  const t = e.target as Node
  if (panelRef.value?.contains(t)) return
  if (props.anchor?.contains(t)) return // 头像自身的切换交给 App
  setTimeout(() => emit('close'), 0)
}
const onKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Escape') emit('close')
}

watch(
  () => props.open,
  (open) => {
    if (open) {
      fetchData()
      updatePosition()
      requestAnimationFrame(updatePosition)
      document.addEventListener('click', onDocClick, true)
      document.addEventListener('keydown', onKeydown)
      window.addEventListener('resize', updatePosition)
      window.addEventListener('scroll', updatePosition, true)
    } else {
      document.removeEventListener('click', onDocClick, true)
      document.removeEventListener('keydown', onKeydown)
      window.removeEventListener('resize', updatePosition)
      window.removeEventListener('scroll', updatePosition, true)
      editing.value = false
    }
  }
)

// 路由跳转自动关闭
watch(
  () => route.fullPath,
  () => emit('close')
)

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick, true)
  document.removeEventListener('keydown', onKeydown)
  window.removeEventListener('resize', updatePosition)
  window.removeEventListener('scroll', updatePosition, true)
})
</script>

<style scoped lang="scss">
.user-panel {
  position: fixed;
  z-index: 200;
  width: 380px;
  max-height: 80vh;
  overflow-y: auto;
  background: $bg-card;
  border: 1px solid $border-soft;
  border-radius: $radius-lg;
  box-shadow: $shadow-dropdown;
}

// 从头像下方淡入 + 从上往下 4px 位移，200ms ease
.panel-enter-active,
.panel-leave-active {
  transition: opacity $dur-base ease, transform $dur-base ease;
}
.panel-enter-from,
.panel-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

.block {
  padding: 20px;
}

.divider {
  height: 1px;
  background: $border-soft;
}

// ============ 区块 1：用户信息 ============
.user-block {
  display: flex;
  gap: $space-sm;
  align-items: center;
  cursor: pointer;
}

.avatar {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #a855f7, #7c3aed);
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: $space-xs;
}

.user-name {
  font-size: 16px;
  font-weight: 700;
  color: $ink;
}

.edit-pencil {
  border: none;
  background: transparent;
  color: $faint;
  font-size: 13px;
  cursor: pointer;
  opacity: 0;
  transition: opacity $dur-fast, color $dur-fast;

  &:hover {
    color: $primary;
  }
}
.user-block:hover .edit-pencil {
  opacity: 1;
}

.user-sign {
  font-size: 13px;
  color: $faint;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.edit-actions {
  display: flex;
  justify-content: flex-end;
  gap: $space-xxs;
}

// ============ 区块 2：学习速览 ============
.overview-block {
  cursor: pointer;
}

.overview-numbers {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: $space-sm;
}

.big {
  display: flex;
  align-items: baseline;
  gap: 7px;
}

.big-num {
  font-size: 32px;
  font-weight: 800;
  color: $ink;
  letter-spacing: -0.02em;
}

.big-label {
  font-size: 13px;
  color: $faint;
}

.small-stats {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  font-size: 13px;
  color: $faint;
}

.stack-bar {
  display: flex;
  height: 6px;
  border-radius: 3px;
  overflow: hidden;
  background: $border-soft;
}

.stack-seg {
  flex-basis: 0;
  min-width: 3px;
}

// ============ 区块 3：今日计划 ============
.block-title {
  font-size: 14px;
  font-weight: 700;
  color: $ink;
  margin-bottom: $space-sm;
}

.plan-item {
  margin-bottom: $space-md;
  cursor: pointer;
}

.plan-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 6px;
}

.plan-name {
  font-size: 14px;
  font-weight: 500;
  color: $ink;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 270px;
}

.plan-percent {
  font-size: 13px;
  color: $faint;
}

.progress-bar {
  height: 4px;
  border-radius: 2px;
  background: $border-soft;
  overflow: hidden;
}

.progress-fill {
  display: block;
  height: 100%;
  border-radius: 2px;
  background: $blue;
  transition: width $dur-slow ease;
}

.plan-module {
  font-size: 12px;
  color: $faint;
  margin: 5px 0 0;
}

.view-all {
  border: none;
  background: transparent;
  padding: 0;
  font-size: 13px;
  color: $primary;
  cursor: pointer;
}

.guide-text {
  font-size: 13px;
  color: $faint;
  margin: 0;
  line-height: 1.6;
}

// ============ 区块 4：功能入口（3 列网格） ============
.feature-block {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: $space-sm 4px;
  border: none;
  border-radius: $radius-md;
  background: transparent;
  cursor: pointer;
  transition: background-color $dur-base ease;

  &:hover {
    background: $bg-subtle;

    .feature-name {
      color: $ink;
    }
  }
}

.feature-icon {
  font-size: 28px;
  line-height: 1;
}

.feature-name {
  font-size: 13px;
  font-weight: 500;
  color: $muted;
}

// ============ 区块 5：底部操作 ============
.bottom-block {
  padding: $space-md 20px;
}

.logout-btn {
  width: 100%;
  border: none;
  background: transparent;
  padding: $space-xxs 0;
  font-size: 13px;
  font-weight: 500;
  color: $faint;
  cursor: pointer;
  text-align: left;
  transition: color $dur-base ease;

  span {
    margin-right: $space-xs;
  }

  &:hover {
    color: $muted;
  }
}

// ============ 暗色模式 ============
:global(.dark) .user-panel {
  background: $dark-card;
  border-color: $dark-border;
}
:global(.dark) .divider {
  background: $dark-border;
}
:global(.dark) .user-name,
:global(.dark) .block-title,
:global(.dark) .big-num,
:global(.dark) .plan-name {
  color: $dark-ink;
}
:global(.dark) .user-sign,
:global(.dark) .big-label,
:global(.dark) .guide-text {
  color: $dark-muted;
}
:global(.dark) .stack-bar,
:global(.dark) .progress-bar {
  background: $dark-border;
}
:global(.dark) .feature-item:hover {
  background: $dark-border;
}
:global(.dark) .feature-item:hover .feature-name {
  color: $dark-ink;
}
</style>
