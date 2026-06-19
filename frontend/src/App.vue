<template>
  <router-view v-if="isPublicLayout" />
  <a-layout v-else class="app-shell" :class="{ 'is-sidebar-collapsed': sidebarCollapsed }">
    <a-layout-sider
      :width="240"
      :collapsed-width="76"
      :collapsed="sidebarCollapsed"
      :trigger="null"
      class="app-aside"
      :class="{ 'is-collapsed': sidebarCollapsed }"
    >
      <div class="brand">
        <div class="brand-mark">
          <img :src="stockLogo" alt="Personal Kline Assistant logo" />
        </div>
        <div class="brand-copy">
          <div class="brand-title">Personal Kline</div>
          <div class="brand-subtitle">Assistant Terminal</div>
        </div>
      </div>

      <a-menu mode="inline" class="side-menu" :selected-keys="[activeMenu]" @click="handleMenuClick">
        <a-menu-item key="/dashboard">
          <template #icon><DashboardOutlined /></template>
          <span>总览</span>
        </a-menu-item>
        <a-menu-item key="/watchlist">
          <template #icon><StarOutlined /></template>
          <span>自选池</span>
        </a-menu-item>
        <a-menu-item key="/kline-detail">
          <template #icon><LineChartOutlined /></template>
          <span>K线详情</span>
        </a-menu-item>
        <a-menu-item key="/signals">
          <template #icon><BellOutlined /></template>
          <span>技术信号</span>
        </a-menu-item>
        <a-menu-item key="/trade-journal">
          <template #icon><FileTextOutlined /></template>
          <span>交易日志</span>
        </a-menu-item>
        <a-menu-item key="/backtest">
          <template #icon><BarChartOutlined /></template>
          <span>模拟回测</span>
        </a-menu-item>
        <a-menu-item key="/etf-ai-analysis">
          <template #icon><RobotOutlined /></template>
          <span>AI 分析</span>
        </a-menu-item>
        <a-menu-item key="/import">
          <template #icon><UploadOutlined /></template>
          <span>数据导入</span>
        </a-menu-item>
        <a-menu-item key="/system-ops">
          <template #icon><DatabaseOutlined /></template>
          <span>系统工具</span>
        </a-menu-item>
      </a-menu>
    </a-layout-sider>

    <a-layout class="app-content-shell">
      <a-layout-header class="app-header">
        <div class="header-title-group">
          <a-button class="sidebar-toggle" :aria-label="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'" @click="toggleSidebar">
            <template #icon>
              <MenuUnfoldOutlined v-if="sidebarCollapsed" />
              <MenuFoldOutlined v-else />
            </template>
          </a-button>
          <div>
            <div class="page-eyebrow">Personal Kline Assistant</div>
            <h1>{{ pageTitle }}</h1>
          </div>
        </div>
        <div class="header-status">
          <span>{{ currentDate }}</span>
          <span class="status-pill">本地模式</span>
          <a-button class="logout-button" size="small" @click="handleLogout">
            <template #icon><LogoutOutlined /></template>
            退出
          </a-button>
        </div>
      </a-layout-header>
      <a-layout-content class="app-main">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import stockLogo from '../resources/logo.png'
import { clearAuthSession } from '@/utils/auth'
import {
  BarChartOutlined,
  BellOutlined,
  DashboardOutlined,
  DatabaseOutlined,
  FileTextOutlined,
  LineChartOutlined,
  LogoutOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  RobotOutlined,
  StarOutlined,
  UploadOutlined
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const sidebarCollapsed = ref(localStorage.getItem('pka-sidebar-collapsed') === 'true')
const pageTitle = computed(() => String(route.meta.title || 'Dashboard'))
const activeMenu = computed(() => (route.path.startsWith('/kline-detail') ? '/kline-detail' : route.path))
const isPublicLayout = computed(() => route.meta.publicLayout === true)
const currentDate = computed(() => {
  const now = new Date()
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    weekday: 'short'
  }).format(now)
})

const handleMenuClick = ({ key }: { key: string }) => {
  if (key && key !== route.path) {
    router.push(key)
  }
}

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
  localStorage.setItem('pka-sidebar-collapsed', String(sidebarCollapsed.value))
  window.dispatchEvent(new Event('resize'))
}

const handleLogout = () => {
  clearAuthSession()
  router.replace({
    path: '/login',
    query: { redirect: route.fullPath }
  })
}
</script>
