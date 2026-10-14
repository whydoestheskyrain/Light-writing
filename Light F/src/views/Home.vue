<template>
  <div class="home">
    <!-- Hero -->
    <section class="hero">
      <span class="hero-badge">
        <span class="dot"></span> 个人学习工作台
      </span>
      <h1 class="hero-title">
        让每一份 Java 学习资料<br />
        都<span class="grad">井然有序</span>
      </h1>
      <p class="hero-sub">
        整理视频教程、文字笔记、八股文与算法题，用卡片构建属于你的学习模块，持续跟踪掌握进度。
      </p>
      <div class="hero-actions">
        <button class="btn-ink" @click="openCreate">
          <span style="font-size: 16px">＋</span> 创建学习模块
        </button>
        <span v-if="store.modules.length" class="hero-count">
          已有 <strong>{{ store.modules.length }}</strong> 个模块
        </span>
        <span class="hero-count hero-study">
          今日学习 <strong>{{ study.format(study.todaySeconds) }}</strong>
        </span>
      </div>
    </section>

    <!-- 模块网格 -->
    <section class="section">
      <div class="section-head">
        <h2 class="section-title">我的模块</h2>
        <span class="section-count">{{ store.modules.length }} 个</span>
      </div>

      <div v-loading="store.loading">
        <div v-if="store.modules.length" class="module-grid">
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
          subtitle="点击创建你的第一个模块，开始整理学习资料"
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
  padding: 40px 0 56px;
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
  margin-bottom: 24px;

  .dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: $primary;
  }
}

.hero-title {
  font-size: 46px;
  font-weight: 800;
  line-height: 1.18;
  letter-spacing: -0.025em;
  color: $ink;
  margin: 0 0 20px;

  .grad {
    background: linear-gradient(120deg, $primary, #ff7eb3);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

.hero-sub {
  font-size: 16px;
  color: $muted;
  max-width: 560px;
  margin: 0 auto 32px;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
}

.btn-ink {
  height: 46px;
  padding: 0 28px;
  font-size: 15px;
}

.hero-count {
  font-size: 14px;
  color: $muted;

  strong {
    color: $ink;
  }
}
.hero-study strong { color: $primary; }

// ============ 网格区 ============
.section {
  margin-top: 8px;
}

.section-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 20px;
  font-weight: 700;
  color: $ink;
  letter-spacing: -0.01em;
}

.section-count {
  font-size: 13px;
  color: $faint;
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: $grid-gap;

  > * {
    width: 95%;
    justify-self: center;
  }
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

// 手机：hero 收缩；卡片保持两列（对齐 Dribbble 移动端）
@media (max-width: 640px) {
  .hero {
    padding: 16px 0 32px;
  }
  .hero-badge { margin-bottom: 18px; }
  .hero-title {
    font-size: 26px;
    margin-bottom: 14px;
  }
  .hero-sub {
    font-size: 13.5px;
    line-height: 1.65;
    margin-bottom: 22px;
  }
  .hero-actions {
    flex-wrap: wrap;
    gap: 10px;
    .btn-ink { width: 100%; height: 44px; justify-content: center; }
  }
  .section-head h2 { font-size: 20px; }
  .module-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
}

// 超窄屏：单列避免卡片过小
@media (max-width: 360px) {
  .module-grid {
    grid-template-columns: 1fr;
  }
}
</style>
