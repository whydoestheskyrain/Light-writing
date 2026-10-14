<template>
  <div class="home">
    <!-- Hero -->
    <section class="hero">
      <span class="hero-badge">
        <span class="dot"></span> 个人学习工作台
      </span>
      <h1 class="hero-title">
        让每一份学习资料<br />
        都<span class="grad">井然有序</span>
      </h1>
      <p class="hero-sub">
        整理视频教程、文字笔记、八股文与算法题，用卡片构建属于你的学习模块，持续跟踪掌握进度。
      </p>
      <div class="hero-actions">
        <button class="btn-primary-gradient hero-cta" @click="openCreate">
          <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor"
            stroke-width="2.4" stroke-linecap="round">
            <path d="M12 5v14M5 12h14" />
          </svg>
          创建学习模块
        </button>
        <div class="hero-stats">
          <span class="stat-pill">
            <span class="stat-icon is-blue">▦</span>
            已有 <strong>{{ store.modules.length }}</strong> 个模块
          </span>
          <span class="stat-pill">
            <span class="stat-icon is-pink">⏱</span>
            今日学习 <strong>{{ study.format(study.todaySeconds) }}</strong>
          </span>
        </div>
      </div>
    </section>

    <!-- 模块网格 -->
    <section class="section">
      <div class="section-head">
        <div class="head-left">
          <h2 class="section-title">我的模块</h2>
          <span class="section-count">{{ store.modules.length }}</span>
        </div>
        <button v-if="store.modules.length" class="head-add" @click="openCreate">
          <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor"
            stroke-width="2.4" stroke-linecap="round">
            <path d="M12 5v14M5 12h14" />
          </svg>
          新建
        </button>
      </div>

      <div>
        <div v-if="store.loading" class="module-grid">
          <div v-for="n in 4" :key="n" class="skeleton-card skeleton" />
        </div>
        <div v-else-if="store.modules.length" class="module-grid">
          <ModuleCard
            v-for="m in store.modules"
            :key="m.id"
            :module="m"
            @edit="openEdit"
            @delete="handleDelete"
          />
          <div class="add-card" @click="openCreate">
            <div class="add-inner">
              <span class="add-plus">
                <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor"
                  stroke-width="2" stroke-linecap="round">
                  <path d="M12 5v14M5 12h14" />
                </svg>
              </span>
              <span class="add-text">新建模块</span>
            </div>
          </div>
        </div>
        <EmptyState
          v-else
          title="还没有学习模块"
          subtitle="创建第一个模块，把零散的教程、笔记和算法题收纳进来"
          action-text="创建第一个模块"
          @click="openCreate"
        />
      </div>
    </section>

    <CreateModuleDialog
      v-model="dialogVisible"
      :module="editingModule"
      @submit="handleSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAppStore } from '@/stores/app'
import { useStudyStore } from '@/stores/study'
import { moduleApi } from '@/api'
import type { Module } from '@/types'
import ModuleCard from '@/components/ModuleCard.vue'
import EmptyState from '@/components/EmptyState.vue'
import CreateModuleDialog from '@/components/CreateModuleDialog.vue'

const store = useAppStore()
const study = useStudyStore()
const dialogVisible = ref(false)
const editingModule = ref<Module | null>(null)

onMounted(() => {
  store.fetchModules()
})

// 监听导航栏“新建模块”
watch(
  () => store.createSignal,
  () => openCreate()
)

const openCreate = () => {
  editingModule.value = null
  dialogVisible.value = true
}

const openEdit = (m: Module) => {
  editingModule.value = m
  dialogVisible.value = true
}

const handleSubmit = async (data: Partial<Module>) => {
  if (editingModule.value) {
    await moduleApi.update(editingModule.value.id, data)
    ElMessage.success('更新成功')
  } else {
    await moduleApi.create(data)
    ElMessage.success('创建成功')
  }
  await store.fetchModules()
}

const handleDelete = async (m: Module) => {
  try {
    await ElMessageBox.confirm(`确定删除模块「${m.name}」吗？模块内的条目也会保留但无法归类。`, '删除模块', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await moduleApi.delete(m.id)
    ElMessage.success('删除成功')
    await store.fetchModules()
  } catch {
    /* cancel */
  }
}
</script>

<style scoped lang="scss">
// ============ Hero ============
.hero {
  text-align: center;
  padding: 32px 0 44px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 6px 14px;
  background: #fff;
  border: 1px solid $border;
  border-radius: $radius-pill;
  font-size: 12.5px;
  font-weight: 500;
  color: $muted;
  margin-bottom: 22px;
  box-shadow: $shadow-sm;

  .dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: $primary;
    box-shadow: 0 0 0 3px rgba(234, 76, 137, 0.15);
  }
}

.hero-title {
  font-size: 42px;
  font-weight: 800;
  line-height: 1.18;
  letter-spacing: -0.025em;
  color: $ink;
  margin: 0 0 18px;

  .grad {
    background: linear-gradient(120deg, $primary, #a05cf0);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

.hero-sub {
  font-size: 15.5px;
  color: $muted;
  max-width: 540px;
  margin: 0 auto 28px;
  line-height: 1.75;
}

.hero-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 18px;
}

.hero-cta {
  height: 48px;
  padding: 0 30px;
  font-size: 15.5px;
}

.hero-stats {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px;
}

// ============ 网格区 ============
.section {
  margin-top: 8px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.head-left {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.section-title {
  font-size: 20px;
  font-weight: 800;
  color: $ink;
  letter-spacing: -0.01em;
}

.section-count {
  min-width: 24px;
  height: 22px;
  padding: 0 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: $radius-pill;
  background: $bg-subtle;
  font-size: 12px;
  font-weight: 600;
  color: $muted;
}

.head-add {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 36px;
  padding: 0 16px;
  border: 1px solid $border;
  border-radius: $radius-pill;
  background: #fff;
  font-size: 13px;
  font-weight: 600;
  color: $ink-soft;
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover {
    border-color: $primary;
    color: $primary;
    background: rgba(234, 76, 137, 0.04);
  }
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $grid-gap;

  > * {
    width: 100%;
    justify-self: stretch;
  }
}

.skeleton-card {
  aspect-ratio: 4 / 3;
  border-radius: $radius-md;
}

.add-card {
  border-radius: $radius-md;
  border: 2px dashed $border;
  aspect-ratio: 4 / 3;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;

  &:hover {
    border-color: $primary;
    background: rgba(234, 76, 137, 0.03);

    .add-plus {
      background: $primary;
      color: #fff;
    }
    .add-text {
      color: $primary;
    }
  }
}

.add-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}

.add-plus {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: $bg-subtle;
  color: $muted;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.add-text {
  font-size: 14px;
  font-weight: 500;
  color: $muted;
}

// ============ 响应式 ============
@media (max-width: 1024px) {
  .module-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .hero {
    padding: 12px 0 28px;
  }
  .hero-badge { margin-bottom: 16px; }
  .hero-title {
    font-size: 27px;
    margin-bottom: 12px;
  }
  .hero-sub {
    font-size: 13.5px;
    line-height: 1.65;
    margin-bottom: 22px;
  }
  .hero-cta { width: 100%; height: 46px; justify-content: center; }
  .hero-stats { width: 100%; }
  .stat-pill { flex: 1; justify-content: center; }
  .section-head h2 { font-size: 19px; }
  .module-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
}

@media (max-width: 360px) {
  .module-grid {
    grid-template-columns: 1fr;
  }
}
</style>
