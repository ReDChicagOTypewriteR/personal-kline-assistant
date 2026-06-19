<template>
  <main class="login-page">
    <section class="login-panel" aria-label="登录 Personal Kline Assistant">
      <div class="login-brand">
        <img :src="stockLogo" alt="Personal Kline Assistant logo" />
        <div>
          <p>Personal Kline Assistant</p>
          <h1>ETF 回测工作台</h1>
        </div>
      </div>

      <div class="login-copy">
        <span class="login-badge">Local Mode</span>
        <h2>登录工作台</h2>
        <p>本地单用户模式，默认账号和密码均为 admin。</p>
      </div>

      <form class="login-form" @submit.prevent="handleLogin">
        <label class="login-field">
          <span>用户名</span>
          <a-input
            v-model:value="form.username"
            autocomplete="username"
            size="large"
            placeholder="admin"
            :disabled="submitting"
          >
            <template #prefix><UserOutlined /></template>
          </a-input>
        </label>

        <label class="login-field">
          <span>密码</span>
          <a-input-password
            v-model:value="form.password"
            autocomplete="current-password"
            size="large"
            placeholder="admin"
            :disabled="submitting"
          >
            <template #prefix><LockOutlined /></template>
          </a-input-password>
        </label>

        <a-button class="login-submit" type="primary" html-type="submit" size="large" :loading="submitting" block>
          登录
        </a-button>
      </form>

      <div class="login-foot">
        <span>ETF Strategy Lab</span>
        <span>Local Account</span>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { LockOutlined, UserOutlined } from '@ant-design/icons-vue'
import { authApi } from '@/api/authApi'
import { saveAuthSession } from '@/utils/auth'
import { feedback } from '@/utils/feedback'
import stockLogo from '../../resources/logo.png'

const route = useRoute()
const router = useRouter()
const submitting = ref(false)
const form = reactive({
  username: 'admin',
  password: 'admin'
})

const handleLogin = async () => {
  if (!form.username.trim() || !form.password.trim()) {
    feedback.warning('请输入用户名和密码')
    return
  }
  submitting.value = true
  try {
    const session = await authApi.login({
      username: form.username.trim(),
      password: form.password
    })
    saveAuthSession(session)
    feedback.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    await router.replace(redirect || '/dashboard')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  color: #153247;
  background:
    radial-gradient(circle at 28% 18%, rgba(93, 173, 255, 0.22), transparent 30%),
    radial-gradient(circle at 76% 76%, rgba(20, 184, 166, 0.12), transparent 32%),
    linear-gradient(135deg, #f8fbff 0%, #eef7fd 48%, #e7f1f8 100%);
  overflow: hidden;
}

.login-page::before {
  content: "";
  position: fixed;
  inset: 9%;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.34);
  filter: blur(42px);
  pointer-events: none;
}

.login-panel {
  position: relative;
  width: min(100%, 420px);
  display: flex;
  flex-direction: column;
  padding: 30px;
  border: 1px solid rgba(142, 169, 190, 0.3);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.54);
  box-shadow:
    0 24px 60px rgba(55, 88, 112, 0.14),
    inset 0 1px 0 rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(22px);
  -webkit-backdrop-filter: blur(22px);
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
}

.login-brand img {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  border: 1px solid rgba(47, 128, 237, 0.14);
  box-shadow: 0 12px 24px rgba(47, 128, 237, 0.1);
}

.login-brand p,
.login-copy p,
.login-foot {
  margin: 0;
  color: #668197;
  font-size: 13px;
  font-weight: 700;
}

.login-brand h1,
.login-copy h2 {
  margin: 2px 0 0;
  color: #153247;
  letter-spacing: 0;
}

.login-brand h1 {
  font-size: 19px;
  font-weight: 900;
}

.login-copy {
  margin-bottom: 22px;
}

.login-badge {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 12px;
  margin-bottom: 12px;
  border-radius: 999px;
  color: #1d6fb9;
  background: rgba(47, 128, 237, 0.1);
  border: 1px solid rgba(47, 128, 237, 0.16);
  font-size: 12px;
  font-weight: 850;
}

.login-copy h2 {
  font-size: 26px;
  line-height: 1.18;
  font-weight: 900;
}

.login-copy p {
  max-width: 360px;
  margin-top: 8px;
  line-height: 1.6;
  font-weight: 650;
}

.login-form {
  display: grid;
  gap: 14px;
}

.login-field {
  display: grid;
  gap: 8px;
}

.login-field > span {
  color: #49687d;
  font-size: 13px;
  font-weight: 850;
}

.login-submit {
  margin-top: 4px;
  height: 44px;
  border-radius: 12px;
}

.login-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid rgba(142, 169, 190, 0.22);
  font-size: 12px;
}

.login-field :deep(.ant-input-affix-wrapper) {
  width: 100%;
  height: 44px;
  box-sizing: border-box;
  color: #153247 !important;
  background: rgba(255, 255, 255, 0.58) !important;
  border: 1px solid rgba(142, 169, 190, 0.32) !important;
  border-radius: 12px !important;
  box-shadow: none !important;
}

.login-field :deep(.ant-input-affix-wrapper:hover) {
  background: rgba(255, 255, 255, 0.72) !important;
  border-color: rgba(47, 128, 237, 0.36) !important;
}

.login-field :deep(.ant-input-affix-wrapper-focused) {
  background: rgba(255, 255, 255, 0.82) !important;
  border-color: rgba(47, 128, 237, 0.58) !important;
  box-shadow: 0 0 0 3px rgba(47, 128, 237, 0.1) !important;
}

.login-field :deep(.ant-input) {
  height: auto;
  color: #153247 !important;
  background: transparent !important;
  border: 0 !important;
  box-shadow: none !important;
}

.login-field :deep(.ant-input::placeholder) {
  color: #8aa0af !important;
}

:deep(.ant-input-prefix) {
  color: #2f80ed;
  margin-right: 10px;
}

@media (max-width: 960px) {
  .login-page {
    padding: 18px;
  }
}

@media (max-width: 560px) {
  .login-panel {
    padding: 24px 20px;
    border-radius: 22px;
  }

  .login-brand {
    margin-bottom: 34px;
  }

  .login-copy h2 {
    font-size: 24px;
  }
}
</style>
