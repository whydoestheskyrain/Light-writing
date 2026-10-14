<template>
  <el-dialog v-model="visible" title="选择条目类型" width="560px">
    <div class="type-grid">
      <div
        v-for="t in types"
        :key="t.value"
        class="type-item"
        :style="{ '--c': t.color }"
        @click="handleSelect(t.value)"
      >
        <div class="type-icon">{{ t.icon }}</div>
        <div class="type-name">{{ t.label }}</div>
        <div class="type-desc">{{ t.desc }}</div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { EntryType } from '@/types'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'select', type: EntryType): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const types = [
  { value: 'note' as EntryType, label: '笔记', icon: '📝', color: '#4f8cff', desc: '自由记录学习笔记' },
  { value: 'algorithm' as EntryType, label: '算法题', icon: '🧮', color: '#ea4c89', desc: 'LeetCode 算法题整理' },
  { value: 'qa' as EntryType, label: '八股题', icon: '📖', color: '#7c5cff', desc: '面试八股文问答' },
  { value: 'link' as EntryType, label: '链接', icon: '🔗', color: '#0bb8b8', desc: '收藏视频 / 文章链接' }
]

const handleSelect = (type: EntryType) => {
  emit('select', type)
  visible.value = false
}
</script>

<style scoped lang="scss">
.type-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}

.type-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  padding: 26px 20px;
  border: 1.5px solid $border-soft;
  border-radius: $radius-md;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;

  &:hover {
    border-color: var(--c);
    background: color-mix(in srgb, var(--c) 6%, transparent);

    .type-icon {
      transform: scale(1.08);
    }
    .type-name {
      color: var(--c);
    }
  }
}

.type-icon {
  font-size: 34px;
  transition: transform 0.2s ease;
}

.type-name {
  font-size: 15px;
  font-weight: 700;
  color: $ink;
  transition: color 0.2s ease;
}

.type-desc {
  font-size: 12px;
  color: $muted;
  text-align: center;
}
</style>
