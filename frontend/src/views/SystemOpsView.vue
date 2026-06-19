<template>
  <div class="page system-ops-page">
    <div class="glass-card ops-hero">
      <div>
        <div class="section-title">系统工具</div>
      </div>
      <div class="ops-actions">
        <a-button :loading="loading" @click="loadSources">
          <template #icon><ReloadOutlined /></template>
          刷新状态
        </a-button>
        <a-button type="primary" :loading="refreshing" @click="refreshCache">
          <template #icon><DatabaseOutlined /></template>
          刷新缓存
        </a-button>
      </div>
    </div>

    <div class="glass-card ai-health-card">
      <div class="ai-health-main">
        <div>
          <div class="section-title">AI 服务状态</div>
          <div class="ai-health-subtitle">
            {{ aiHealth?.service || 'etf-ai-analysis' }} · {{ aiHealth?.providerName || 'DeepSeek' }} · {{ aiHealth?.model || 'deepseek-chat' }}
          </div>
        </div>
        <span class="source-status" :class="aiHealth?.status === 'UP' ? 'up' : 'unknown'">
          {{ aiHealth?.status || 'UNKNOWN' }}
        </span>
      </div>
      <div class="ai-health-actions">
        <span class="enabled-pill" :class="{ off: !aiHealth?.providerEnabled }">
          {{ aiHealth?.providerEnabled ? 'Provider 已启用' : 'Provider 未启用' }}
        </span>
        <span class="table-count-pill">检查于 {{ aiHealth?.checkedAt || '-' }}</span>
        <a-button :loading="aiHealthLoading" @click="loadAiHealth(true)">
          <template #icon><ReloadOutlined /></template>
          检查 AI 服务
        </a-button>
      </div>
    </div>

    <div class="metric-grid ops-metrics">
      <div class="metric">
        <div class="metric-label">数据源总数</div>
        <div class="metric-value value-blue">{{ overview?.totalCount || 0 }}</div>
        <div class="metric-note">Configured Sources</div>
      </div>
      <div class="metric">
        <div class="metric-label">工作中</div>
        <div class="metric-value value-up">{{ overview?.upCount || 0 }}</div>
        <div class="metric-note">UP</div>
      </div>
      <div class="metric">
        <div class="metric-label">不可用</div>
        <div class="metric-value value-down">{{ overview?.downCount || 0 }}</div>
        <div class="metric-note">DOWN</div>
      </div>
      <div class="metric">
        <div class="metric-label">未知</div>
        <div class="metric-value">{{ overview?.unknownCount || 0 }}</div>
        <div class="metric-note">UNKNOWN</div>
      </div>
    </div>

    <div class="glass-card ai-settings-card">
      <a-collapse v-model:activeKey="activeToolPanels" ghost>
        <a-collapse-panel key="ai-provider">
          <template #header>
            <div class="collapse-header">
              <span>AI 模型配置</span>
              <span class="collapse-meta">
                {{ aiForm.provider }} · {{ aiForm.model }}
                <span v-if="aiSettings?.aiProvider.apiKeyConfigured"> · {{ aiSettings.aiProvider.apiKeyMasked }}</span>
              </span>
            </div>
          </template>
          <div class="settings-grid">
            <label class="setting-field inline-field">
              <span>启用</span>
              <a-switch v-model:checked="aiForm.enabled" />
            </label>
            <label class="setting-field">
              <span>Provider</span>
              <a-select v-model:value="aiForm.provider">
                <a-select-option value="deepseek">DeepSeek</a-select-option>
              </a-select>
            </label>
            <label class="setting-field">
              <span>Base URL</span>
              <a-input v-model:value="aiForm.baseUrl" />
            </label>
            <label class="setting-field">
              <span>模型</span>
              <a-select v-model:value="aiForm.model">
                <a-select-option value="deepseek-chat">deepseek-chat</a-select-option>
                <a-select-option value="deepseek-reasoner">deepseek-reasoner</a-select-option>
              </a-select>
            </label>
            <label class="setting-field">
              <span>API Key</span>
              <a-input-password
                v-model:value="aiApiKeyDraft"
                :placeholder="aiSettings?.aiProvider.apiKeyConfigured ? `已保存 ${aiSettings.aiProvider.apiKeyMasked}，留空则不变` : '粘贴 DeepSeek API Key'"
              />
            </label>
            <label class="setting-field">
              <span>Temperature</span>
              <a-input-number v-model:value="aiForm.temperature" :min="0" :max="2" :step="0.1" />
            </label>
            <label class="setting-field">
              <span>Max Tokens</span>
              <a-input-number v-model:value="aiForm.maxTokens" :min="256" :max="8192" :step="128" />
            </label>
          </div>
          <div class="settings-actions">
            <a-button :loading="aiSettingsLoading" @click="loadAiSettings">
              <template #icon><ReloadOutlined /></template>
              读取配置
            </a-button>
            <a-button type="primary" :loading="aiSettingsSaving" @click="saveAiSettings">
              保存 AI 配置
            </a-button>
          </div>
          <div class="ai-test-box">
            <a-input
              v-model:value="aiTestMessage"
              placeholder="输入一句话测试 DeepSeek，例如：用一句话说明沪深300ETF今天应关注什么风险"
              @pressEnter="testAiChat"
            />
            <a-button :loading="aiTesting" @click="testAiChat">测试调用</a-button>
          </div>
          <div v-if="aiTestResult" class="ai-test-result">
            {{ aiTestResult }}
          </div>
        </a-collapse-panel>

        <a-collapse-panel key="news-provider">
          <template #header>
            <div class="collapse-header">
              <span>新闻源 API 配置</span>
              <span class="collapse-meta">{{ enabledNewsProviderCount }} 个已启用</span>
            </div>
          </template>
          <div class="news-provider-list">
            <div v-for="provider in newsProviderForms" :key="provider.provider" class="news-provider-row">
              <a-switch v-model:checked="provider.enabled" />
              <div class="news-provider-main">
                <strong>{{ provider.displayName || provider.provider }}</strong>
                <span>{{ provider.provider }}</span>
              </div>
              <a-input v-model:value="provider.baseUrl" placeholder="Base URL，可选" />
              <a-input-password
                v-model:value="provider.apiKey"
                :placeholder="provider.apiKeyConfigured ? `已保存 ${provider.apiKeyMasked}，留空则不变` : 'API Key'"
              />
            </div>
          </div>
          <div class="settings-actions">
            <a-button type="primary" :loading="aiSettingsSaving" @click="saveAiSettings">
              保存新闻源配置
            </a-button>
          </div>
        </a-collapse-panel>
      </a-collapse>
    </div>

    <div class="glass-card table-card source-card">
      <a-collapse v-model:activeKey="sourceStatusPanels" ghost>
        <a-collapse-panel key="market-source">
          <template #header>
            <div class="table-head collapse-table-head">
              <div>
                <div class="section-title">行情数据源状态</div>
              </div>
              <div class="table-summary">
                <span class="table-count-pill">刷新于 {{ overview?.refreshedAt || '-' }}</span>
              </div>
            </div>
          </template>
          <a-table
            class="app-table"
            :data-source="overview?.sources || []"
            :loading="loading"
            :pagination="false"
            row-key="sourceCode"
            size="middle"
            :scroll="{ x: 1180 }"
          >
            <a-table-column data-index="sourceCode" title="编码" :width="150" fixed="left" />
            <a-table-column data-index="displayName" title="数据源" :width="170" />
            <a-table-column data-index="providerType" title="类型" :width="130" />
            <a-table-column title="启用" :width="90">
              <template #default="{ record }">
                <span class="enabled-pill" :class="{ off: !record.enabled }">{{ record.enabled ? '已启用' : '未启用' }}</span>
              </template>
            </a-table-column>
            <a-table-column title="状态" :width="110">
              <template #default="{ record }">
                <span class="source-status" :class="statusClass(record.status)">{{ record.status }}</span>
              </template>
            </a-table-column>
            <a-table-column data-index="role" title="用途" :width="190" />
            <a-table-column data-index="healthMessage" title="探测结果" :width="260" />
            <a-table-column data-index="lastSuccessAt" title="最近成功" :width="170" />
            <a-table-column data-index="lastErrorAt" title="最近失败" :width="170" />
            <a-table-column title="调用统计" :width="120">
              <template #default="{ record }">
                <span class="value-up">{{ record.successCount }}</span>
                <span class="muted"> / </span>
                <span class="value-down">{{ record.failureCount }}</span>
              </template>
            </a-table-column>
            <a-table-column data-index="lastError" title="最近错误" :width="280" />
          </a-table>
        </a-collapse-panel>
      </a-collapse>
    </div>

    <div v-if="refreshResult" class="glass-card refresh-result-card">
      <div class="section-title">缓存刷新结果</div>
      <strong>{{ refreshResult.message }}</strong>
      <div class="refresh-meta">
        <span>{{ refreshResult.status }}</span>
        <span>{{ refreshResult.refreshedAt }}</span>
      </div>
      <ul>
        <li v-for="item in refreshResult.actions" :key="item">{{ item }}</li>
      </ul>
    </div>

    <div class="glass-card redis-note-card">
      <div class="section-title">Redis</div>
      <div class="redis-short-list">
        <span>行情缓存</span>
        <span>名称解析</span>
        <span>数据源状态</span>
      </div>
      <div class="kline-cache-tool">
        <label>
          <span>ETF</span>
          <SymbolSelector v-model="klineCacheSymbol" />
        </label>
        <a-button :loading="klineRefreshing" @click="refreshKlineCache(false)">
          <template #icon><DatabaseOutlined /></template>
          刷新选中 ETF
        </a-button>
        <a-button type="primary" :loading="klineRefreshing" @click="refreshKlineCache(true)">
          <template #icon><DatabaseOutlined /></template>
          刷新全部 K 线缓存
        </a-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { DatabaseOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import SymbolSelector from '@/components/SymbolSelector.vue'
import { systemApi } from '@/api/systemApi'
import { etfAiApi, type AiRuntimeSettingsDTO, type EtfAiHealthDTO } from '@/api/etfAiApi'
import { feedback } from '@/utils/feedback'
import type { CacheRefreshResultDTO, MarketDataSourceOverviewDTO } from '@/types'

const overview = ref<MarketDataSourceOverviewDTO>()
const refreshResult = ref<CacheRefreshResultDTO>()
const loading = ref(false)
const refreshing = ref(false)
const klineRefreshing = ref(false)
const klineCacheSymbol = ref<string>()
const activeToolPanels = ref<string[]>(['ai-provider'])
const sourceStatusPanels = ref<string[]>(['market-source'])
const aiHealth = ref<EtfAiHealthDTO | null>(null)
const aiHealthLoading = ref(false)
const aiSettings = ref<AiRuntimeSettingsDTO>()
const aiSettingsLoading = ref(false)
const aiSettingsSaving = ref(false)
const aiTesting = ref(false)
const aiApiKeyDraft = ref('')
const aiTestMessage = ref('用一句话说明沪深300ETF今天应关注什么风险')
const aiTestResult = ref('')
const aiForm = reactive({
  enabled: false,
  provider: 'deepseek',
  baseUrl: 'https://api.deepseek.com',
  model: 'deepseek-chat',
  temperature: 0.2,
  maxTokens: 1600
})
const newsProviderForms = ref<Array<{
  provider: string
  displayName: string
  enabled: boolean
  baseUrl?: string
  apiKey?: string
  apiKeyConfigured?: boolean
  apiKeyMasked?: string
}>>([])

const enabledNewsProviderCount = computed(() => newsProviderForms.value.filter((item) => item.enabled).length)

const loadSources = async () => {
  loading.value = true
  try {
    overview.value = await systemApi.marketDataSources()
  } finally {
    loading.value = false
  }
}

const refreshCache = async () => {
  refreshing.value = true
  try {
    refreshResult.value = await systemApi.refreshCache()
    feedback.success('缓存刷新完成')
    await loadSources()
  } finally {
    refreshing.value = false
  }
}

const refreshKlineCache = async (all: boolean) => {
  if (!all && !klineCacheSymbol.value) {
    feedback.warning('请先选择 ETF，或点击刷新全部 K 线缓存')
    return
  }
  klineRefreshing.value = true
  try {
    refreshResult.value = await systemApi.refreshKlineCache(all ? undefined : klineCacheSymbol.value)
    feedback.success('K 线缓存刷新完成')
  } finally {
    klineRefreshing.value = false
  }
}

const loadAiHealth = async (showFeedback = false) => {
  aiHealthLoading.value = true
  try {
    aiHealth.value = await etfAiApi.health()
    if (showFeedback) {
      feedback.success('AI 服务状态已刷新')
    }
  } catch (error) {
    aiHealth.value = null
    if (showFeedback) {
      feedback.error(error instanceof Error ? error.message : 'AI 服务不可用')
    }
  } finally {
    aiHealthLoading.value = false
  }
}

const loadAiSettings = async () => {
  aiSettingsLoading.value = true
  try {
    const data = await etfAiApi.settings()
    aiSettings.value = data
    aiForm.enabled = data.aiProvider.enabled
    aiForm.provider = data.aiProvider.provider || 'deepseek'
    aiForm.baseUrl = data.aiProvider.baseUrl || 'https://api.deepseek.com'
    aiForm.model = data.aiProvider.model || 'deepseek-chat'
    aiForm.temperature = data.aiProvider.temperature ?? 0.2
    aiForm.maxTokens = data.aiProvider.maxTokens ?? 1600
    aiApiKeyDraft.value = ''
    newsProviderForms.value = (data.newsProviders || []).map((item) => ({
      provider: item.provider,
      displayName: item.displayName,
      enabled: item.enabled,
      baseUrl: item.baseUrl,
      apiKey: '',
      apiKeyConfigured: item.apiKeyConfigured,
      apiKeyMasked: item.apiKeyMasked
    }))
  } finally {
    aiSettingsLoading.value = false
  }
}

const saveAiSettings = async () => {
  aiSettingsSaving.value = true
  try {
    const payload = {
      aiProvider: {
        enabled: aiForm.enabled,
        provider: aiForm.provider,
        baseUrl: aiForm.baseUrl,
        model: aiForm.model,
        apiKey: aiApiKeyDraft.value.trim() || undefined,
        temperature: aiForm.temperature,
        maxTokens: aiForm.maxTokens
      },
      newsProviders: newsProviderForms.value.map((item) => ({
        provider: item.provider,
        displayName: item.displayName,
        enabled: item.enabled,
        baseUrl: item.baseUrl,
        apiKey: item.apiKey?.trim() || undefined
      }))
    }
    aiSettings.value = await etfAiApi.saveSettings(payload)
    feedback.success('AI 配置已保存')
    await loadAiSettings()
  } finally {
    aiSettingsSaving.value = false
  }
}

const testAiChat = async () => {
  if (!aiTestMessage.value.trim()) {
    feedback.warning('请输入测试内容')
    return
  }
  aiTesting.value = true
  aiTestResult.value = ''
  try {
    const response = await etfAiApi.chat({
      message: aiTestMessage.value,
      ragEnabled: false
    })
    aiTestResult.value = response.content
    if (response.fallbackUsed) {
      feedback.warning('AI Provider 未启用或未配置 Key，已返回本地占位回复')
    } else {
      feedback.success('DeepSeek 调用成功')
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : 'AI 测试调用失败'
    aiTestResult.value = message
    feedback.error(message)
  } finally {
    aiTesting.value = false
  }
}

const statusClass = (status?: string) => {
  if (status === 'UP') return 'up'
  if (status === 'DOWN') return 'down'
  return 'unknown'
}

onMounted(() => {
  loadSources()
  loadAiHealth()
  loadAiSettings()
})
</script>

<style scoped>
.ops-hero,
.source-card,
.refresh-result-card,
.redis-note-card,
.ai-settings-card,
.ai-health-card {
  padding: 10px;
}

.ops-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.ops-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.ops-actions :deep(.ant-btn) {
  margin-left: 0;
}

.ai-health-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.ai-health-main {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.ai-health-subtitle {
  margin-top: 4px;
  color: #5c7182;
  font-size: 12px;
  font-weight: 800;
}

.ai-health-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.ai-settings-card :deep(.ant-collapse-header) {
  padding: 6px 0 !important;
}

.ai-settings-card :deep(.ant-collapse-content-box) {
  padding: 8px 0 12px !important;
}

.collapse-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  font-weight: 900;
  color: #123044;
}

.source-card :deep(.ant-collapse-header) {
  padding: 0 !important;
}

.source-card :deep(.ant-collapse-content-box) {
  padding: 10px 0 0 !important;
}

.collapse-table-head {
  width: 100%;
  margin-bottom: 0;
}

.collapse-meta {
  color: #5c7182;
  font-size: 12px;
  font-weight: 800;
}

.settings-grid {
  display: grid;
  grid-template-columns: 110px minmax(180px, 220px) minmax(260px, 1fr) minmax(180px, 220px);
  gap: 10px;
  align-items: end;
}

.setting-field {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.setting-field > span {
  color: #5c7182;
  font-size: 12px;
  font-weight: 850;
}

.inline-field {
  align-items: center;
  grid-template-columns: auto auto;
  justify-content: start;
  gap: 8px;
}

.settings-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;
}

.ai-test-box {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) auto;
  gap: 8px;
  margin-top: 10px;
}

.ai-test-result {
  margin-top: 10px;
  padding: 10px;
  border-radius: 8px;
  color: #123044;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.14);
  line-height: 1.55;
  white-space: pre-wrap;
}

.news-provider-list {
  display: grid;
  gap: 8px;
}

.news-provider-row {
  display: grid;
  grid-template-columns: 44px minmax(150px, 220px) minmax(220px, 1fr) minmax(220px, 1fr);
  align-items: center;
  gap: 8px;
}

.news-provider-main {
  display: grid;
  gap: 2px;
}

.news-provider-main strong {
  color: #123044;
  font-size: 13px;
}

.news-provider-main span {
  color: #5c7182;
  font-size: 12px;
  font-weight: 800;
}

.source-status {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 900;
  border: 1px solid rgba(255, 255, 255, 0.14);
}

.enabled-pill {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 850;
  color: #f6f6f3;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.14);
}

.enabled-pill.off {
  color: #7a8fa0;
  background: rgba(255, 255, 255, 0.04);
}

.source-status.up {
  color: #34d399;
  background: rgba(52, 211, 153, 0.12);
}

.source-status.down {
  color: #fb7185;
  background: rgba(251, 113, 133, 0.12);
}

.source-status.unknown {
  color: #fbbf24;
  background: rgba(251, 191, 36, 0.12);
}

.refresh-meta {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin: 10px 0;
}

.refresh-meta span {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  padding: 0 8px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 850;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.refresh-result-card ul {
  margin: 6px 0 0;
  padding-left: 18px;
  color: #123044;
  line-height: 1.55;
}

.redis-short-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.redis-short-list span {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  color: #123044;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.12);
  font-size: 12px;
  font-weight: 850;
}

.kline-cache-tool {
  display: grid;
  grid-template-columns: minmax(220px, 280px) auto auto;
  align-items: end;
  gap: 8px;
  margin-top: 12px;
}

.kline-cache-tool label {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.kline-cache-tool label > span {
  color: #5c7182;
  font-size: 12px;
  font-weight: 850;
}

@media (max-width: 780px) {
  .ops-hero {
    flex-direction: column;
  }

  .ops-actions,
  .ops-actions :deep(.ant-btn),
  .ai-health-card,
  .ai-health-actions,
  .ai-health-actions :deep(.ant-btn),
  .settings-actions :deep(.ant-btn),
  .ai-test-box,
  .ai-test-box :deep(.ant-btn),
  .kline-cache-tool,
  .kline-cache-tool :deep(.symbol-selector-wrap),
  .kline-cache-tool :deep(.ant-btn) {
    width: 100%;
  }

  .settings-grid,
  .news-provider-row,
  .ai-test-box,
  .kline-cache-tool {
    grid-template-columns: 1fr;
  }
}
</style>
