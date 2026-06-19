<template>
  <main ref="portalRef" class="portal-page" :style="portalStyle" @pointermove="handlePointerMove" @pointerleave="resetPointer">
    <header class="portal-nav glass-surface" aria-label="Personal Kline Assistant 门户导航">
      <a class="portal-brand" href="#top" aria-label="Personal Kline Assistant">
        <img :src="stockLogo" alt="Personal Kline Assistant logo" />
        <span>Personal Kline Assistant</span>
      </a>
      <nav class="portal-links" aria-label="页面导航">
        <a href="#workflow">流程</a>
        <a href="#backtest">回测</a>
        <a href="#ai-risk">AI 风控</a>
        <router-link to="/login">进入工作台</router-link>
      </nav>
    </header>

    <section id="top" class="portal-hero" aria-labelledby="portal-title">
      <div class="hero-copy reveal">
        <a-tag class="hero-tag" color="processing">ETF Strategy Lab</a-tag>
        <h1 id="portal-title">用回测验证策略，用 AI 审查风险。</h1>
        <p class="hero-lede">
          面向个人量化学习与准实盘观察的本地工作台。日 K、技术信号、模拟回测、AI 风险过滤和交易复盘，在同一条清晰链路里完成。
        </p>
        <div class="portal-actions">
          <router-link to="/login">
            <a-button type="primary" size="large" class="primary-action">
              <template #icon><LoginOutlined /></template>
              进入工作台
            </a-button>
          </router-link>
          <a href="#workflow">
            <a-button size="large" class="secondary-action">
              <template #icon><BranchesOutlined /></template>
              查看流程
            </a-button>
          </a>
        </div>
      </div>

      <div class="hero-visual reveal" aria-label="回测与 AI 风险控制预览">
        <a-card class="terminal-card glass-surface" :bordered="false">
          <div class="terminal-header">
            <div>
              <span>Quant Kline Console</span>
              <strong>BUY_CANDIDATE Review</strong>
            </div>
            <a-tag color="success">Local Mode</a-tag>
          </div>

          <div class="chart-stage" aria-hidden="true">
            <span v-for="bar in chartBars" :key="bar.left" class="chart-bar" :style="bar"></span>
            <svg class="equity-line" viewBox="0 0 460 170" role="img" aria-label="策略与基准资金曲线示意">
              <polyline class="line-main" points="10,132 54,120 98,95 142,102 186,82 230,70 274,76 318,52 362,43 450,30" />
              <polyline class="line-base" points="10,140 54,130 98,118 142,116 186,100 230,96 274,88 318,82 362,72 450,66" />
            </svg>
            <div class="scan-line"></div>
          </div>

          <div class="metric-grid">
            <div v-for="item in heroMetrics" :key="item.label" class="metric-cell">
              <span>{{ item.label }}</span>
              <strong>{{ item.prefix }}{{ item.value }}{{ item.suffix }}</strong>
            </div>
          </div>

          <div class="decision-strip">
            <CheckCircleOutlined />
            <div>
              <span>Final Decision</span>
              <strong>ALLOW_SIMULATION · 低风险观察仓位</strong>
            </div>
          </div>
        </a-card>
      </div>
    </section>

    <section id="workflow" class="portal-section reveal" aria-labelledby="workflow-title">
      <div class="section-heading">
        <a-tag color="blue">System Flow</a-tag>
        <h2 id="workflow-title">参考 Ant Landing 的清晰分层，突出你的真实交易辅助闭环。</h2>
      </div>
      <div class="workflow-grid">
        <a-card
          v-for="(item, index) in workflow"
          :key="item.title"
          class="workflow-card glass-card"
          :class="{ 'is-active': activeStep === index }"
          :bordered="false"
        >
          <component :is="item.icon" class="workflow-icon" />
          <span>{{ item.step }}</span>
          <h3>{{ item.title }}</h3>
          <p>{{ item.text }}</p>
        </a-card>
      </div>
    </section>

    <section class="portal-band reveal" aria-label="核心能力">
      <a-card id="backtest" class="feature-panel glass-card" :bordered="false">
        <a-tag color="gold">Backtest</a-tag>
        <h2>回测不是装饰，是策略上线前的第一道门槛。</h2>
        <ul class="feature-list">
          <li v-for="item in backtestFeatures" :key="item.text">
            <component :is="item.icon" />
            <span>{{ item.text }}</span>
          </li>
        </ul>
      </a-card>

      <a-card id="ai-risk" class="feature-panel glass-card accent-panel" :bordered="false">
        <a-tag color="purple">AI Risk Filter</a-tag>
        <h2>AI 不替你下单，只把风险、反方观点和约束条件说清楚。</h2>
        <ul class="feature-list">
          <li v-for="item in aiFeatures" :key="item.text">
            <component :is="item.icon" />
            <span>{{ item.text }}</span>
          </li>
        </ul>
      </a-card>
    </section>

    <section class="portal-section principles-section reveal" aria-labelledby="principles-title">
      <div class="section-heading">
        <a-tag color="cyan">Operating Rules</a-tag>
        <h2 id="principles-title">少一点噱头，多一点可解释、可复盘、可降级。</h2>
      </div>
      <div class="principles">
        <a-card v-for="item in principles" :key="item.title" class="principle glass-card" :bordered="false">
          <span>{{ item.value }}</span>
          <h3>{{ item.title }}</h3>
          <p>{{ item.text }}</p>
        </a-card>
      </div>
    </section>

    <footer class="portal-footer glass-surface">
      <span>Personal Kline Assistant</span>
      <router-link to="/login">进入本地工作台</router-link>
    </footer>
  </main>
</template>

<script setup lang="ts">
import type { CSSProperties, Component } from 'vue'
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import stockLogo from '../../resources/logo.png'
import {
  BarChartOutlined,
  BranchesOutlined,
  CheckCircleOutlined,
  DatabaseOutlined,
  FundProjectionScreenOutlined,
  LineChartOutlined,
  LoginOutlined,
  RobotOutlined,
  SafetyCertificateOutlined,
  StockOutlined,
  TableOutlined
} from '@ant-design/icons-vue'

type Feature = {
  text: string
  icon: Component
}

const portalRef = ref<HTMLElement | null>(null)
const activeStep = ref(0)
const animated = reactive({
  returns: 0,
  drawdown: 0,
  risk: 0
})
const pointer = reactive({
  x: 0,
  y: 0
})

let raf = 0
let timer = 0
let observer: IntersectionObserver | undefined

const portalStyle = computed<CSSProperties>(() => ({
  '--tilt-x': `${pointer.y * -4}deg`,
  '--tilt-y': `${pointer.x * 5}deg`,
  '--shift-x': `${pointer.x * 16}px`,
  '--shift-y': `${pointer.y * 14}px`
}) as CSSProperties)

const heroMetrics = computed(() => [
  { label: '策略收益', prefix: '+', value: animated.returns.toFixed(1), suffix: '%' },
  { label: '最大回撤', prefix: '-', value: animated.drawdown.toFixed(1), suffix: '%' },
  { label: 'AI 风险分', prefix: '', value: Math.round(animated.risk).toString(), suffix: '/100' }
])

const chartBars = [
  { left: '5%', height: '34%', background: '#9fb2c4', animationDelay: '0ms' },
  { left: '14%', height: '45%', background: '#b9c6d1', animationDelay: '80ms' },
  { left: '23%', height: '39%', background: '#8fa5ba', animationDelay: '160ms' },
  { left: '32%', height: '58%', background: '#d7a84d', animationDelay: '240ms' },
  { left: '41%', height: '50%', background: '#9fb2c4', animationDelay: '320ms' },
  { left: '50%', height: '66%', background: '#14a39a', animationDelay: '400ms' },
  { left: '59%', height: '54%', background: '#9fb2c4', animationDelay: '480ms' },
  { left: '68%', height: '74%', background: '#1677ff', animationDelay: '560ms' },
  { left: '77%', height: '62%', background: '#d7a84d', animationDelay: '640ms' },
  { left: '86%', height: '80%', background: '#14a39a', animationDelay: '720ms' }
]

const workflow = [
  {
    step: '01',
    title: '日 K 与指标',
    text: '沉淀 daily_kline，计算 MA、RSI、ATR、量能和价格位置。',
    icon: StockOutlined
  },
  {
    step: '02',
    title: '技术信号',
    text: '趋势、均线、RSI 与量能共同筛出 BUY_CANDIDATE。',
    icon: FundProjectionScreenOutlined
  },
  {
    step: '03',
    title: '模拟回测',
    text: 'ETF_TREND 与 ETF_DCA 回放历史，校验收益、回撤和交易频率。',
    icon: BarChartOutlined
  },
  {
    step: '04',
    title: 'AI 风险审查',
    text: 'LLM 只审查候选标的，输出风险分、反方观点和行动约束。',
    icon: RobotOutlined
  }
]

const backtestFeatures: Feature[] = [
  { icon: BarChartOutlined, text: 'ETF 趋势策略与 ETF 定投策略并行验证' },
  { icon: LineChartOutlined, text: '资金曲线、基准对比、回撤与胜率集中呈现' },
  { icon: TableOutlined, text: '保留成交记录，方便后续复制给 AI 做横向分析' }
]

const aiFeatures: Feature[] = [
  { icon: RobotOutlined, text: '只有 BUY_CANDIDATE 进入 AI 风险过滤' },
  { icon: SafetyCertificateOutlined, text: '风险分低于 40 才允许模拟，高风险直接阻断' },
  { icon: DatabaseOutlined, text: '同一标的同一天走缓存，失败时保守降级为观察' }
]

const principles = [
  {
    value: '70',
    title: 'ETF 候选阈值',
    text: '默认 BUY_CANDIDATE 分数阈值更适合 ETF，允许部分均线确认。'
  },
  {
    value: '40',
    title: '低风险放行线',
    text: 'AI 风险分低于 40 才允许进入模拟交易，否则观察或阻断。'
  },
  {
    value: '0',
    title: '不自动下单',
    text: '系统定位是验证与复盘，不接券商 API，不做无人确认执行。'
  }
]

const animateNumber = (key: keyof typeof animated, target: number, duration: number) => {
  const started = performance.now()
  const tick = (now: number) => {
    const progress = Math.min((now - started) / duration, 1)
    const eased = 1 - Math.pow(1 - progress, 3)
    animated[key] = target * eased
    if (progress < 1) {
      raf = requestAnimationFrame(tick)
    }
  }
  raf = requestAnimationFrame(tick)
}

const handlePointerMove = (event: PointerEvent) => {
  const width = window.innerWidth || 1
  const height = window.innerHeight || 1
  pointer.x = (event.clientX / width - 0.5) * 2
  pointer.y = (event.clientY / height - 0.5) * 2
}

const resetPointer = () => {
  pointer.x = 0
  pointer.y = 0
}

onMounted(() => {
  if (!window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    animateNumber('returns', 18.6, 900)
    animateNumber('drawdown', 6.8, 1000)
    animateNumber('risk', 32, 850)
    timer = window.setInterval(() => {
      activeStep.value = (activeStep.value + 1) % workflow.length
    }, 2200)
  } else {
    animated.returns = 18.6
    animated.drawdown = 6.8
    animated.risk = 32
  }

  observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-visible')
        }
      })
    },
    { threshold: 0.16 }
  )

  portalRef.value?.querySelectorAll('.reveal').forEach((item) => observer?.observe(item))
})

onUnmounted(() => {
  cancelAnimationFrame(raf)
  window.clearInterval(timer)
  observer?.disconnect()
})
</script>

<style scoped>
.portal-page {
  min-height: 100dvh;
  overflow-x: hidden;
  color: #101828;
  background:
    linear-gradient(135deg, rgba(22, 119, 255, 0.16), transparent 32%),
    linear-gradient(225deg, rgba(20, 163, 154, 0.14), transparent 34%),
    linear-gradient(180deg, #f8fbff 0%, #edf4fb 48%, #f6f7fb 100%);
  font-family: Inter, "PingFang SC", "Microsoft YaHei", Arial, sans-serif;
}

.portal-page::before {
  content: "";
  position: fixed;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(16, 24, 40, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(16, 24, 40, 0.04) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.72), transparent 78%);
}

.glass-surface,
.glass-card :deep(.ant-card-body),
.terminal-card :deep(.ant-card-body) {
  border: 1px solid rgba(255, 255, 255, 0.62);
  background: rgba(255, 255, 255, 0.52);
  box-shadow:
    0 24px 70px rgba(42, 61, 84, 0.16),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(24px) saturate(150%);
  -webkit-backdrop-filter: blur(24px) saturate(150%);
}

.portal-nav {
  position: sticky;
  top: 16px;
  z-index: 20;
  width: min(1180px, calc(100% - 32px));
  min-height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 16px auto 0;
  padding: 0 18px;
  border-radius: 8px;
}

.portal-brand,
.portal-links,
.portal-actions,
.terminal-header,
.decision-strip,
.feature-list li,
.portal-footer {
  display: flex;
  align-items: center;
}

.portal-brand {
  min-height: 44px;
  gap: 12px;
  color: #101828;
  font-size: 15px;
  font-weight: 850;
  text-decoration: none;
}

.portal-brand img {
  width: 40px;
  height: 40px;
  border: 1px solid rgba(22, 119, 255, 0.12);
  border-radius: 8px;
  box-shadow: 0 10px 24px rgba(22, 119, 255, 0.12);
}

.portal-links {
  gap: 6px;
}

.portal-links a,
.portal-links :deep(a) {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  padding: 0 14px;
  border-radius: 8px;
  color: #344054;
  font-size: 14px;
  font-weight: 760;
  text-decoration: none;
  transition: color 0.2s ease, background-color 0.2s ease;
}

.portal-links a:hover,
.portal-links :deep(a:hover) {
  color: #1677ff;
  background: rgba(22, 119, 255, 0.08);
}

.portal-hero {
  position: relative;
  z-index: 1;
  width: min(1180px, calc(100% - 40px));
  min-height: min(820px, calc(100dvh - 80px));
  display: grid;
  grid-template-columns: minmax(0, 1.03fr) minmax(420px, 0.97fr);
  align-items: center;
  gap: clamp(28px, 4vw, 56px);
  margin: 0 auto;
  padding: 48px 0 70px;
}

.hero-copy {
  max-width: 640px;
}

.hero-tag {
  height: 30px;
  display: inline-flex;
  align-items: center;
  border-radius: 8px;
  font-weight: 780;
}

.hero-copy h1,
.section-heading h2,
.feature-panel h2 {
  margin: 0;
  color: #101828;
  font-weight: 900;
  letter-spacing: 0;
}

.hero-copy h1 {
  margin-top: 20px;
  font-size: clamp(44px, 5.4vw, 66px);
  line-height: 1.06;
}

.hero-lede {
  max-width: 620px;
  margin: 24px 0 0;
  color: #475467;
  font-size: 18px;
  line-height: 1.76;
  font-weight: 560;
}

.portal-actions {
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 34px;
}

.portal-actions a {
  text-decoration: none;
}

.primary-action.ant-btn,
.secondary-action.ant-btn {
  min-width: 148px;
  height: 48px;
  border-radius: 8px;
  font-weight: 820;
  box-shadow: none;
}

.primary-action.ant-btn {
  background: linear-gradient(135deg, #1677ff 0%, #14a39a 100%);
}

.secondary-action.ant-btn {
  color: #1d2939;
  border-color: rgba(22, 119, 255, 0.18);
  background: rgba(255, 255, 255, 0.58);
  backdrop-filter: blur(18px);
}

.hero-visual {
  perspective: 1200px;
}

.terminal-card {
  border-radius: 8px;
  transform: translate3d(var(--shift-x), var(--shift-y), 0) rotateX(var(--tilt-x)) rotateY(var(--tilt-y));
  transform-style: preserve-3d;
  transition: transform 0.18s ease-out;
}

.terminal-card :deep(.ant-card-body) {
  padding: 24px;
  border-radius: 8px;
}

.terminal-header {
  justify-content: space-between;
  gap: 16px;
}

.terminal-header span,
.metric-cell span,
.decision-strip span {
  color: #667085;
  font-size: 13px;
  font-weight: 760;
}

.terminal-header strong {
  display: block;
  margin-top: 4px;
  color: #101828;
  font-size: 16px;
}

.chart-stage {
  position: relative;
  height: 288px;
  margin-top: 22px;
  overflow: hidden;
  border: 1px solid rgba(22, 119, 255, 0.1);
  border-radius: 8px;
  background:
    linear-gradient(rgba(16, 24, 40, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(16, 24, 40, 0.06) 1px, transparent 1px),
    rgba(255, 255, 255, 0.48);
  background-size: 100% 58px, 58px 100%, auto;
}

.chart-bar {
  position: absolute;
  bottom: 26px;
  width: 24px;
  border-radius: 7px 7px 0 0;
  opacity: 0.76;
  animation: bar-rise 0.82s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.equity-line {
  position: absolute;
  inset: 54px 24px 42px;
  width: calc(100% - 48px);
  height: 170px;
  overflow: visible;
}

.equity-line polyline {
  fill: none;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.line-main {
  stroke: #1677ff;
  stroke-width: 5;
  stroke-dasharray: 720;
  stroke-dashoffset: 720;
  animation: line-draw 1.5s 0.16s ease-out forwards;
}

.line-base {
  stroke: #d7a84d;
  stroke-width: 3;
  opacity: 0.72;
  stroke-dasharray: 720;
  stroke-dashoffset: 720;
  animation: line-draw 1.7s 0.26s ease-out forwards;
}

.scan-line {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 80px;
  background: linear-gradient(90deg, transparent, rgba(22, 119, 255, 0.14), transparent);
  animation: scan 3s ease-in-out infinite;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.metric-cell {
  min-width: 0;
  padding: 16px;
  border: 1px solid rgba(22, 119, 255, 0.1);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.46);
}

.metric-cell strong {
  display: block;
  margin-top: 8px;
  color: #101828;
  font-size: 24px;
  line-height: 1.1;
}

.decision-strip {
  gap: 12px;
  margin-top: 14px;
  padding: 16px;
  border: 1px solid rgba(20, 163, 154, 0.18);
  border-radius: 8px;
  color: #14a39a;
  background: rgba(20, 163, 154, 0.09);
}

.decision-strip svg {
  flex: 0 0 auto;
  font-size: 22px;
}

.decision-strip strong {
  display: block;
  margin-top: 4px;
  color: #101828;
  font-size: 15px;
}

.portal-section,
.portal-band,
.portal-footer {
  position: relative;
  z-index: 1;
  width: min(1180px, calc(100% - 40px));
  margin: 0 auto;
}

.portal-section {
  padding: 88px 0;
}

.section-heading {
  max-width: 780px;
}

.section-heading h2,
.feature-panel h2 {
  margin-top: 14px;
  font-size: clamp(30px, 4vw, 48px);
  line-height: 1.14;
}

.workflow-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-top: 36px;
}

.workflow-card :deep(.ant-card-body),
.feature-panel :deep(.ant-card-body),
.principle :deep(.ant-card-body) {
  border-radius: 8px;
}

.workflow-card {
  min-height: 256px;
  border-radius: 8px;
  transform: translateY(0);
  transition: transform 0.24s ease, box-shadow 0.24s ease;
}

.workflow-card.is-active {
  transform: translateY(-6px);
}

.workflow-card.is-active :deep(.ant-card-body) {
  box-shadow:
    0 30px 80px rgba(22, 119, 255, 0.22),
    inset 0 1px 0 rgba(255, 255, 255, 0.86);
}

.workflow-icon {
  color: #1677ff;
  font-size: 30px;
}

.workflow-card span {
  display: block;
  margin-top: 28px;
  color: #a66f00;
  font-size: 13px;
  font-weight: 860;
}

.workflow-card h3,
.principle h3 {
  margin: 10px 0 0;
  color: #101828;
  font-size: 19px;
  line-height: 1.3;
}

.workflow-card p,
.principle p {
  margin: 12px 0 0;
  color: #526577;
  font-size: 15px;
  line-height: 1.68;
}

.portal-band {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  padding: 0 0 88px;
}

.feature-panel {
  border-radius: 8px;
}

.feature-panel :deep(.ant-card-body) {
  min-height: 440px;
  padding: clamp(26px, 4vw, 40px);
}

.accent-panel :deep(.ant-card-body) {
  background: rgba(250, 252, 255, 0.62);
}

.feature-list {
  display: grid;
  gap: 16px;
  margin: 32px 0 0;
  padding: 0;
  list-style: none;
}

.feature-list li {
  min-height: 60px;
  gap: 14px;
  color: #344054;
  font-size: 15px;
  line-height: 1.52;
  font-weight: 700;
}

.feature-list svg {
  flex: 0 0 auto;
  color: #1677ff;
  font-size: 22px;
}

.principles {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 36px;
}

.principle {
  border-radius: 8px;
}

.principle :deep(.ant-card-body) {
  min-height: 220px;
  padding: 26px;
}

.principle span {
  color: #1677ff;
  font-size: 48px;
  line-height: 1;
  font-weight: 900;
}

.portal-footer {
  min-height: 92px;
  justify-content: space-between;
  margin-bottom: 24px;
  padding: 0 22px;
  border-radius: 8px;
  color: #667085;
  font-size: 14px;
  font-weight: 760;
}

.portal-footer a {
  min-height: 44px;
  display: inline-flex;
  align-items: center;
  padding: 0 14px;
  border-radius: 8px;
  color: #1677ff;
  text-decoration: none;
}

.portal-footer a:hover {
  background: rgba(22, 119, 255, 0.08);
}

.reveal {
  opacity: 0;
  transform: translateY(24px);
  transition: opacity 0.58s ease, transform 0.58s ease;
}

.reveal.is-visible {
  opacity: 1;
  transform: translateY(0);
}

@keyframes bar-rise {
  from {
    transform: scaleY(0);
    transform-origin: bottom;
  }
  to {
    transform: scaleY(1);
    transform-origin: bottom;
  }
}

@keyframes line-draw {
  to {
    stroke-dashoffset: 0;
  }
}

@keyframes scan {
  0% {
    transform: translateX(-110px);
  }
  48%,
  100% {
    transform: translateX(560px);
  }
}

@media (max-width: 1020px) {
  .portal-nav {
    position: static;
  }

  .portal-links {
    display: none;
  }

  .portal-hero {
    min-height: auto;
    grid-template-columns: 1fr;
    padding-top: 42px;
  }

  .terminal-card {
    transform: none;
  }

  .workflow-grid,
  .portal-band,
  .principles {
    grid-template-columns: 1fr;
  }

  .workflow-card,
  .feature-panel :deep(.ant-card-body),
  .principle :deep(.ant-card-body) {
    min-height: auto;
  }
}

@media (max-width: 620px) {
  .portal-nav,
  .portal-hero,
  .portal-section,
  .portal-band,
  .portal-footer {
    width: min(100% - 28px, 1180px);
  }

  .portal-nav {
    margin-top: 14px;
    padding: 0 12px;
  }

  .portal-brand span {
    font-size: 14px;
  }

  .hero-copy h1 {
    font-size: 40px;
    line-height: 1.1;
  }

  .hero-lede {
    font-size: 16px;
  }

  .portal-actions,
  .portal-actions a,
  .portal-actions .ant-btn {
    width: 100%;
  }

  .terminal-card :deep(.ant-card-body) {
    padding: 16px;
  }

  .terminal-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .chart-stage {
    height: 238px;
  }

  .chart-bar {
    width: 20px;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .portal-section {
    padding: 68px 0;
  }

  .portal-band {
    padding-bottom: 68px;
  }

  .portal-footer {
    align-items: flex-start;
    flex-direction: column;
    justify-content: center;
    gap: 8px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .terminal-card,
  .workflow-card,
  .reveal,
  .portal-links a,
  .portal-links :deep(a),
  .portal-footer a {
    transition: none;
  }

  .chart-bar,
  .line-main,
  .line-base,
  .scan-line {
    animation: none;
  }
}
</style>
