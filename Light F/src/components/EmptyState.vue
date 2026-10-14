<template>
  <div class="empty-wrap">
    <div class="empty-card" @click="$emit('click')">
      <div class="plus-circle">
        <svg viewBox="0 0 24 24" width="26" height="26" fill="none" stroke="currentColor"
          stroke-width="2.2" stroke-linecap="round">
          <path d="M12 5v14M5 12h14" />
        </svg>
      </div>
      <p class="title">{{ title }}</p>
      <p class="sub">{{ subtitle }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ title?: string; subtitle?: string }>()
defineEmits<{ (e: 'click'): void }>()
</script>

<style scoped lang="scss">
.empty-wrap {
  display: flex;
  justify-content: center;
  padding: 40px 0;
}

.empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 56px 72px;
  border: 2px dashed $border;
  border-radius: $radius-lg;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease;

  &:hover {
    border-color: $primary;
    background: rgba(234, 76, 137, 0.03);

    .plus-circle {
      background: $primary;
      border-color: $primary;
      color: #fff;
      transform: scale(1.04);
    }
  }
}

.plus-circle {
  width: 58px;
  height: 58px;
  border-radius: 50%;
  border: 2px solid $border;
  color: $muted;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.title {
  font-size: 16px;
  font-weight: 600;
  color: $ink;
}

.sub {
  font-size: 13px;
  color: $muted;
}
</style>
