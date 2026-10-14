<template>
  <div class="module-detail">
    <el-breadcrumb separator="/" class="crumb">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>{{ module?.name || '模块' }}</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- 加载骨架 -->
    <div v-if="!module" class="header-skeleton skeleton"></div>

    <div v-else class="module-header vl-card">
      <div class="header-main">
        <div class="icon-big" :style="{ background: (module.color || '#ea4c89') + '1a' }">
          {{ module.icon || '📁' }}
        </div>
        <div class="header-text">
          <h1 class="module-name">{{ module.name }}</h1>
          <p class="module-desc">{{ module.description || '这个模块还没有写简介' }}</p>
          <div class="module-stats">
            <span class="ms-item"><strong>{{ entries.length }}</strong> 个条目</span>
            <span class="ms-dot">·</span>
            <span class="ms-item doing"><strong>{{ doingCount }}</strong> 在做</span>
            <span class="ms-dot">·</span>
            <span class="ms-item done"><strong>{{ doneCount }}</strong> 已掌握</span>
          </div>
        </div>
      </div>
      <div class="header-actions">
        <button class="btn-ghost" @click="goEditModule">编辑模块</button>
        <button class="btn-primary-gradient" @click="router.push(`/entry/edit/${moduleId}`)">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor"
            stroke-width="2.4" stroke-linecap="round">
            <path d="M12 5v14M5 12h14" />
          </svg>
          添加条目
        </button>
      </div>
    </div>

    <div v-if="module" class="filter-bar">
      <el-radio-group v-model="filterStatus" @change="fetchList">
        <el-radio-button :label="null">全部</el-radio-button>
        <el-radio-button :label="0">未做</el-radio-button>
        <el-radio-button :label="1">在做</el-radio-button>
        <el-radio-button :label="2">已掌握</el-radio-button>
      </el-radio-group>
      <el-input
        v-model="keyword"
        placeholder="搜索条目标题"
        clearable
        style="width: 240px"
        @keyup.enter="fetchList"
        @clear="fetchList"
      />
    </div>

    <div v-if="module && entries.length" class="entry-grid">
      <EntryCard
        v-for="e in entries"
        :key="e.id"
        :entry="e"
        @edit="handleEdit"
        @delete="handleDelete"
        @status="handleStatus"
      />
    </div>
    <EmptyState
      v-else-if="module"
      title="该模块还没有条目"
      subtitle="添加第一条笔记、算法题或八股文，开始填充这个模块"
      action-text="添加第一个条目"
      @click="router.push(`/entry/edit/${moduleId}`)"
    />

    <CreateModuleDialog v-model="editDialogVisible" :module="module" @submit="handleModuleSubmit" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { moduleApi, entryApi } from '@/api'
import type { Module, Entry } from '@/types'
import EntryCard from '@/components/EntryCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import CreateModuleDialog from '@/components/CreateModuleDialog.vue'

const route = useRoute()
const router = useRouter()
const moduleId = String(route.params.id)

const module = ref<Module | null>(null)
const entries = ref<Entry[]>([])
const filterStatus = ref<number | null>(null)
const keyword = ref('')
const editDialogVisible = ref(false)

const doingCount = computed(() => entries.value.filter((e) => e.status === 1).length)
const doneCount = computed(() => entries.value.filter((e) => e.status === 2).length)

const goEditModule = () => {
  editDialogVisible.value = true
}

const handleModuleSubmit = async (data: Partial<Module>) => {
  await moduleApi.update(moduleId, data)
  ElMessage.success('模块已更新')
  const res = await moduleApi.detail(moduleId)
  module.value = res.data
}

onMounted(async () => {
  const [mRes, eRes] = await Promise.all([
    moduleApi.detail(moduleId),
    entryApi.list({ moduleId })
  ])
  module.value = mRes.data
  entries.value = eRes.data.records
})

const fetchList = async () => {
  const res = await entryApi.list({
    moduleId,
    status: filterStatus.value ?? undefined,
    keyword: keyword.value || undefined
  })
  entries.value = res.data.records
}

const handleEdit = (entry: Entry) => {
  router.push(`/entry/edit/${moduleId}/${entry.id}`)
}

const handleDelete = async (entry: Entry) => {
  try {
    await ElMessageBox.confirm(`确定删除「${entry.title}」吗？`, '删除条目', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await entryApi.delete(entry.id)
    ElMessage.success('删除成功')
    fetchList()
  } catch {
    /* cancel */
  }
}

const handleStatus = async (entry: Entry, status: number) => {
  await entryApi.updateStatus(entry.id, status)
  entry.status = status as Entry['status']
  ElMessage.success('状态已更新')
}
</script>

<style scoped lang="scss">
.module-detail {
  animation: page-fade 0.2s ease;
}

.crumb {
  margin-bottom: 18px;
}

.header-skeleton {
  height: 120px;
  border-radius: $radius-md;
  margin-bottom: 24px;
}

.module-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  padding: 24px 28px;
  margin-bottom: 24px;
}

.header-main {
  display: flex;
  align-items: flex-start;
  gap: 18px;
  min-width: 0;
}

.icon-big {
  flex-shrink: 0;
  width: 60px;
  height: 60px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
}

.header-text {
  min-width: 0;
}

.module-name {
  font-size: 23px;
  font-weight: 800;
  color: $ink;
  margin: 2px 0 6px;
  letter-spacing: -0.015em;
}

.module-desc {
  color: $muted;
  font-size: 14px;
  line-height: 1.6;
  margin: 0 0 12px;
}

.module-stats {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 13px;
  color: $muted;

  strong {
    color: $ink;
    font-weight: 700;
  }
  .doing strong { color: $warning; }
  .done strong { color: $success; }
}

.ms-dot {
  color: $border;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 22px;
  gap: 16px;
  flex-wrap: wrap;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: $gap;
}

// ============ 响应式 ============
@media (max-width: 768px) {
  .module-header {
    flex-direction: column;
    gap: 16px;
    padding: 18px;
  }
  .icon-big { width: 50px; height: 50px; font-size: 25px; }
  .module-name { font-size: 19px; }
  .header-actions {
    width: 100%;
    .btn-ghost,
    .btn-primary-gradient {
      flex: 1;
      justify-content: center;
    }
  }

  .filter-bar { gap: 12px; }
  .filter-bar .el-input { width: 100% !important; }
  .entry-grid {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 14px;
  }
}

@media (max-width: 480px) {
  .filter-bar .el-radio-group {
    max-width: 100%;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
  .entry-grid { grid-template-columns: 1fr; }
}

@keyframes page-fade {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>
