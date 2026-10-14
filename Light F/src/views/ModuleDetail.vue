<template>
  <div class="module-detail">
    <el-breadcrumb separator="/" class="crumb">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>{{ module?.name }}</el-breadcrumb-item>
    </el-breadcrumb>

    <div v-if="module" class="module-header vl-card">
      <div class="header-left">
        <div class="icon-big" :style="{ background: (module.color || '#ea4c89') + '1a' }">
          {{ module.icon || '📁' }}
        </div>
        <div class="header-text">
          <h1 class="module-name">{{ module.name }}</h1>
          <p class="module-desc">{{ module.description || '暂无简介' }}</p>
        </div>
      </div>
      <button class="btn-ink" @click="router.push(`/entry/edit/${moduleId}`)">
        <span style="font-size: 16px">＋</span> 添加条目
      </button>
    </div>

    <div class="filter-bar">
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

    <div v-if="entries.length" class="entry-grid">
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
      v-else
      title="该模块还没有条目"
      subtitle="点击右上角添加第一个条目"
      @click="router.push(`/entry/edit/${moduleId}`)"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { moduleApi, entryApi } from '@/api'
import type { Module, Entry } from '@/types'
import EntryCard from '@/components/EntryCard.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const moduleId = Number(route.params.id)

const module = ref<Module | null>(null)
const entries = ref<Entry[]>([])
const filterStatus = ref<number | null>(null)
const keyword = ref('')

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
  margin-bottom: 20px
}

.module-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 28px;
  margin-bottom: 26px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 18px;
}

.icon-big {
  width: 60px;
  height: 60px;
  border-radius: $radius-md;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
}

.module-name {
  font-size: 22px;
  font-weight: 700;
  color: $ink;
  margin: 0 0 5px;
  letter-spacing: -0.01em;
}

.module-desc {
  color: $muted;
  font-size: 14px;
  margin: 0;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
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
    align-items: stretch;
    gap: 16px;
    padding: 18px;
  }
  .header-left { gap: 12px; }
  .icon-big { width: 48px; height: 48px; font-size: 24px; }
  .module-name { font-size: 19px; }
  .module-header .btn-ink { width: 100%; justify-content: center; }

  .filter-bar { gap: 12px; }
  // 模块内搜索框占满整行（覆盖 inline 的 240px）
  .filter-bar .el-input { width: 100% !important; }
  .entry-grid {
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 14px;
  }
}

@media (max-width: 480px) {
  // 状态筛选按钮组横向滚动，不折行
  .filter-bar .el-radio-group {
    max-width: 100%;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
  .entry-grid { grid-template-columns: 1fr; }
}

@keyframes page-fade {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
</style>
