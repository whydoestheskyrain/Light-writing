<template>
  <div class="entry-card vl-card" @click="goDetail">
    <div class="card-top">
      <span class="type-pill" :style="{ background: typeColor + '1a', color: typeColor }">
        {{ typeLabel }}
      </span>
      <el-dropdown trigger="click" @click.stop @command="handleCommand">
        <button class="more-btn" @click.stop aria-label="更多">⋯</button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="edit">编辑条目</el-dropdown-item>
            <el-dropdown-item command="delete" divided>删除条目</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <h4 class="entry-title">{{ entry.title }}</h4>

    <div v-if="tagList.length" class="tag-row">
      <span v-for="tag in tagList" :key="tag" class="tag">#{{ tag }}</span>
    </div>

    <div class="card-footer">
      <el-dropdown trigger="click" @click.stop @command="handleStatus">
        <span class="status-trigger">
          <span class="status-dot" :style="{ background: statusColor }"></span>
          {{ statusLabel }}
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="0">
              <span class="li-dot" style="background: #9e9ea7"></span> 未做
            </el-dropdown-item>
            <el-dropdown-item command="1">
              <span class="li-dot" style="background: #f7b500"></span> 在做
            </el-dropdown-item>
            <el-dropdown-item command="2">
              <span class="li-dot" style="background: #00b150"></span> 已掌握
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <span class="update-time">{{ formatTime(entry.updateTime) }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { Entry } from '@/types'

const props = defineProps<{ entry: Entry }>()
const emit = defineEmits<{
  (e: 'edit', entry: Entry): void
  (e: 'delete', entry: Entry): void
  (e: 'status', entry: Entry, status: number): void
}>()

const router = useRouter()

const typeMap: Record<string, { label: string; color: string }> = {
  note: { label: '笔记', color: '#4f8cff' },
  algorithm: { label: '算法', color: '#ea4c89' },
  qa: { label: '八股', color: '#7c5cff' },
  link: { label: '链接', color: '#0bb8b8' }
}

const statusMap: Record<number, { label: string; color: string }> = {
  0: { label: '未做', color: '#9e9ea7' },
  1: { label: '在做', color: '#f7b500' },
  2: { label: '已掌握', color: '#00b150' }
}

const typeLabel = computed(() => typeMap[props.entry.type]?.label || props.entry.type)
const typeColor = computed(() => typeMap[props.entry.type]?.color || '#6e6d7a')
const statusLabel = computed(() => statusMap[props.entry.status]?.label || '未做')
const statusColor = computed(() => statusMap[props.entry.status]?.color || '#9e9ea7')

const tagList = computed(() =>
  props.entry.tags ? props.entry.tags.split(',').filter(Boolean) : []
)

const goDetail = () => router.push(`/entry/${props.entry.id}`)

const handleCommand = (cmd: string) => {
  if (cmd === 'edit') emit('edit', props.entry)
  if (cmd === 'delete') emit('delete', props.entry)
}

const handleStatus = (status: string) => {
  emit('status', props.entry, Number(status))
}

const formatTime = (t: string) => (t ? t.slice(0, 10) : '')
</script>

<style scoped lang="scss">
.entry-card {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 13px;
  cursor: pointer;

  &:hover .more-btn {
    opacity: 1;
  }
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.type-pill {
  padding: 3px 11px;
  border-radius: $radius-pill;
  font-size: 12px;
  font-weight: 600;
}

.more-btn {
  width: 26px;
  height: 26px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 16px;
  color: $faint;
  border-radius: 50%;
  opacity: 0;
  transition: all 0.2s;

  &:hover {
    background: $bg-subtle;
    color: $ink;
  }
}

.entry-title {
  font-size: 15.5px;
  font-weight: 700;
  color: $ink;
  margin: 0;
  line-height: 1.45;
  letter-spacing: -0.01em;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;

  .tag {
    font-size: 12px;
    color: $muted;
    background: $bg-subtle;
    padding: 2px 9px;
    border-radius: $radius-sm;
  }
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
  padding-top: 13px;
  border-top: 1px solid $border-soft;
}

.status-trigger {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 12.5px;
  font-weight: 600;
  color: $ink-soft;
  cursor: pointer;
  padding: 3px 9px;
  border-radius: $radius-pill;
  transition: background 0.1s ease;

  .status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }

  &:hover {
    background: $bg-subtle;
  }
}

.update-time {
  font-size: 12px;
  color: $faint;
}

.li-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
}
</style>
