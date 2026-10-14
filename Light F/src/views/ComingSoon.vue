<template>
  <div class="coming-soon">
    <div class="soon-card">
      <div class="soon-illu">
        <svg viewBox="0 0 160 130" fill="none" xmlns="http://www.w3.org/2000/svg">
          <ellipse cx="80" cy="112" rx="52" ry="8" fill="#f4f4f6" />
          <rect x="28" y="26" width="104" height="76" rx="14" fill="#fff" stroke="#e7e7e9" stroke-width="2" />
          <circle cx="56" cy="60" r="15" fill="#fdecf4" stroke="#ea4c89" stroke-width="2.5" />
          <path d="M50 60l4.5 4.5L62 57" stroke="#ea4c89" stroke-width="2.5" fill="none" stroke-linecap="round" stroke-linejoin="round" />
          <line x1="82" y1="54" x2="114" y2="54" stroke="#e7e7e9" stroke-width="3" stroke-linecap="round" />
          <line x1="82" y1="66" x2="106" y2="66" stroke="#f0f0f0" stroke-width="3" stroke-linecap="round" />
          <path d="M118 30l3-7 3 7 7 3-7 3-3 7-3-7-7-3z" fill="#7c5cff" opacity="0.85" />
        </svg>
      </div>
      <h2 class="soon-title">{{ featureName }}</h2>
      <p class="soon-sub">这个功能正在赶来的路上，敬请期待 ✨</p>
      <div class="soon-actions">
        <button class="btn-ghost" @click="goBack">返回上一页</button>
        <button class="btn-primary-gradient" @click="router.push('/')">回到首页</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const featureName = computed(() => {
  const name = (route.query.name as string) || '功能'
  return `${name} · 即将上线`
})

const goBack = () => router.back()
</script>

<style scoped lang="scss">
.coming-soon {
  display: flex;
  justify-content: center;
  padding: 56px 0 72px;
  animation: soon-fade 0.2s ease;
}

.soon-card {
  width: 420px;
  max-width: 100%;
  padding: 44px 36px 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 12px;
}

.soon-illu {
  width: 150px;
  margin-bottom: 8px;
  animation: floaty 3.2s ease-in-out infinite;
}

@keyframes floaty {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

.soon-title {
  font-size: 21px;
  font-weight: 800;
  color: $ink;
  margin: 0;
  letter-spacing: -0.01em;
}

.soon-sub {
  font-size: 14px;
  color: $muted;
  margin: 0 0 14px;
}

.soon-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: center;
}

@keyframes soon-fade {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>
