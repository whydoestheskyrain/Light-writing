<template>
  <div class="coming-soon">
    <div class="soon-card vl-card">
      <div class="soon-icon">{{ icon }}</div>
      <h2 class="soon-title">{{ featureName }}</h2>
      <p class="soon-sub">这个功能正在赶来的路上，敬请期待 ✨</p>
      <button class="btn-ink" @click="goBack">返回上一页</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const featureName = computed(() => (route.query.name as string) || '功能')

const iconMap: Record<string, string> = {
  统计: '📊',
  笔记: '📝',
  音乐盒: '🎵',
  计划: '📅',
  复习: '🔁',
  个人中心: '👤',
  设置: '⚙️',
  壁纸: '🎨'
}
const icon = computed(() => iconMap[featureName.value] || '🚧')

const goBack = () => router.back()
</script>

<style scoped lang="scss">
.coming-soon {
  display: flex;
  justify-content: center;
  padding: 60px 0;
  animation: soon-fade 0.2s ease;
}

.soon-card {
  width: 420px;
  max-width: 100%;
  padding: 48px 36px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.soon-icon {
  font-size: 52px;
  line-height: 1;
  margin-bottom: 6px;
}

.soon-title {
  font-size: 22px;
  font-weight: 800;
  color: $ink;
  margin: 0;
  letter-spacing: -0.01em;
}

.soon-sub {
  font-size: 14px;
  color: $muted;
  margin: 0 0 16px;
}

@keyframes soon-fade {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
</style>
