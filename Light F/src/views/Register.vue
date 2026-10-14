<template>
  <div class="auth-page">
    <!-- 品牌展示区（宽屏可见） -->
    <aside class="brand-pane">
      <div class="brand-inner">
        <div class="brand-logo" @click="router.push('/login')">Light</div>
        <h2 class="brand-headline">
          加入 Light<br />开启<span>有序学习</span>
        </h2>
        <p class="brand-desc">
          一分钟创建账号，把零散的教程、笔记和算法题收进一个持续生长的知识库。
        </p>
        <ul class="brand-points">
          <li>
            <span class="bp-icon">✓</span>
            <div><strong>免费开始</strong><small>无需信用卡，注册即用</small></div>
          </li>
          <li>
            <span class="bp-icon">✓</span>
            <div><strong>数据独立</strong><small>每个账号的内容严格隔离</small></div>
          </li>
          <li>
            <span class="bp-icon">✓</span>
            <div><strong>随时可用</strong><small>浏览器直接访问，进度自动保存</small></div>
          </li>
        </ul>
      </div>
      <div class="brand-deco deco-1"></div>
      <div class="brand-deco deco-2"></div>
    </aside>

    <!-- 表单区 -->
    <main class="form-pane">
      <div class="form-card">
        <div class="mobile-logo">Light</div>
        <h1 class="form-title">创建账号</h1>
        <p class="form-sub">注册成功后将自动登录</p>

        <el-form class="auth-form" @submit.prevent="submit">
          <el-form-item>
            <el-input v-model="form.username" placeholder="用户名" size="large" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.nickname" placeholder="昵称（选填）" size="large" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" type="password" placeholder="密码（至少 6 位）" size="large"
              show-password @keyup.enter="submit" />
          </el-form-item>
          <button class="btn-primary-gradient auth-submit" :disabled="loading" type="submit">
            {{ loading ? '注册中…' : '注册并登录' }}
          </button>
        </el-form>

        <p class="form-switch">
          已有账号？<el-link type="primary" underline="never" @click="router.push('/login')">返回登录</el-link>
        </p>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { identityApi } from '@/api'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: '', nickname: '', password: '' })

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (form.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  loading.value = true
  try {
    const r = await identityApi.register(form)
    localStorage.setItem('light_access_token', r.data.accessToken)
    localStorage.setItem('light_refresh_token', r.data.refreshToken)
    ElMessage.success('注册成功，欢迎加入 Light')
    router.push('/')
  } catch {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.auth-page {
  display: grid;
  grid-template-columns: 1.05fr 1fr;
  min-height: 100vh;
}

.brand-pane {
  position: relative;
  overflow: hidden;
  background: linear-gradient(150deg, #7c5cff 0%, #9b5cf0 38%, #ea4c89 100%);
  color: #fff;
  display: flex;
  align-items: center;
  padding: 64px 72px;
}

.brand-inner {
  position: relative;
  z-index: 2;
  max-width: 440px;
}

.brand-logo {
  font-size: 30px;
  font-weight: 900;
  font-style: italic;
  font-family: Georgia, 'Times New Roman', serif;
  letter-spacing: -0.03em;
  margin-bottom: 56px;
  cursor: pointer;
}

.brand-headline {
  font-size: 38px;
  font-weight: 800;
  line-height: 1.25;
  letter-spacing: -0.02em;
  margin: 0 0 18px;

  span {
    color: #ffd9e8;
  }
}

.brand-desc {
  font-size: 15px;
  line-height: 1.8;
  opacity: 0.88;
  margin: 0 0 40px;
}

.brand-points {
  display: flex;
  flex-direction: column;
  gap: 22px;

  li {
    display: flex;
    gap: 14px;
    align-items: flex-start;
  }
}

.bp-icon {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.22);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  margin-top: 1px;
}

.brand-points strong {
  display: block;
  font-size: 15px;
  font-weight: 700;
  margin-bottom: 3px;
}
.brand-points small {
  font-size: 13px;
  opacity: 0.82;
}

.brand-deco {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  z-index: 1;
}
.deco-1 {
  width: 380px;
  height: 380px;
  right: -120px;
  top: -100px;
}
.deco-2 {
  width: 260px;
  height: 260px;
  left: -80px;
  bottom: -70px;
  background: rgba(255, 255, 255, 0.06);
}

.form-pane {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  background: #fff;
}

.form-card {
  width: 100%;
  max-width: 380px;
}

.mobile-logo {
  display: none;
  font-size: 28px;
  font-weight: 900;
  font-style: italic;
  font-family: Georgia, serif;
  background: linear-gradient(135deg, #ea4c89, #7c5cff);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 28px;
}

.form-title {
  font-size: 28px;
  font-weight: 800;
  color: $ink;
  letter-spacing: -0.02em;
  margin: 0 0 8px;
}

.form-sub {
  font-size: 14.5px;
  color: $muted;
  margin: 0 0 32px;
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 18px;
}
.auth-form :deep(.el-input__wrapper) {
  border-radius: 12px;
  padding: 4px 14px;
  box-shadow: 0 0 0 1px $border inset;
  transition: box-shadow 0.15s;

  &:hover { box-shadow: 0 0 0 1px $faint inset; }
  &.is-focus { box-shadow: 0 0 0 2px rgba(234, 76, 137, 0.35) inset; }
}
.auth-form :deep(.el-input__inner) {
  height: 44px;
  font-size: 15px;
}

.auth-submit {
  width: 100%;
  height: 48px;
  justify-content: center;
  font-size: 15.5px;
  margin-top: 6px;

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.form-switch {
  margin-top: 24px;
  text-align: center;
  font-size: 14px;
  color: $muted;
}

@media (max-width: 900px) {
  .auth-page {
    grid-template-columns: 1fr;
  }
  .brand-pane {
    display: none;
  }
  .mobile-logo {
    display: block;
  }
  .form-pane {
    background: linear-gradient(180deg, #fff 0%, #f6f3fe 100%);
  }
}
</style>
