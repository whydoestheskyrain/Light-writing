<template>
  <div class="empty-wrap">
    <div class="empty-card">
      <div class="empty-illu">
        <!-- 极简线条插画：卡片 + 放大镜 + 闪光 -->
        <svg viewBox="0 0 160 130" fill="none" xmlns="http://www.w3.org/2000/svg">
          <ellipse cx="80" cy="112" rx="52" ry="8" fill="#f4f4f6" />
          <rect x="34" y="30" width="78" height="58" rx="12" fill="#fff" stroke="#e7e7e9" stroke-width="2" />
          <line x1="48" y1="48" x2="86" y2="48" stroke="#e7e7e9" stroke-width="3" stroke-linecap="round" />
          <line x1="48" y1="61" x2="98" y2="61" stroke="#f0f0f0" stroke-width="3" stroke-linecap="round" />
          <line x1="48" y1="74" x2="74" y2="74" stroke="#f0f0f0" stroke-width="3" stroke-linecap="round" />
          <circle cx="106" cy="78" r="17" fill="#fff" stroke="#ea4c89" stroke-width="3.5" />
          <line x1="118" y1="90" x2="128" y2="100" stroke="#ea4c89" stroke-width="4" stroke-linecap="round" />
          <path d="M100 74l3.2 6.5 7.1 1-5.1 5 1.2 7.1-6.4-3.4-6.4 3.4 1.2-7.1-5.1-5 7.1-1z" fill="#fdecf4" />
        </svg>
      </div>
      <p class="title">{{ title }}</p>
      <p class="sub">{{ subtitle }}</p>
      <div v-if="$slots.default || actionText" class="action">
        <slot>
          <button class="btn-primary-gradient" @click="$emit('click')">{{ actionText || '立即创建' }}</button>
        </slot>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ title?: string; subtitle?: string; actionText?: string }>()
defineEmits<{ (e: 'click'): void }>()
</script>

<style scoped lang="scss">
.empty-wrap {
  display: flex;
  justify-content: center;
  padding: 48px 0 64px;
}

.empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 14px;
  max-width: 420px;
}

.empty-illu {
  width: 160px;
  margin-bottom: 6px;
  animation: floaty 3.2s ease-in-out infinite;
}

@keyframes floaty {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

.title {
  font-size: 17px;
  font-weight: 700;
  color: $ink;
  letter-spacing: -0.01em;
  margin: 0;
}

.sub {
  font-size: 13.5px;
  color: $muted;
  line-height: 1.7;
  margin: 0;
}

.action {
  margin-top: 10px;
}
</style>
