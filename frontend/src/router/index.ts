import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated } from '@/utils/auth'

const LoginView = () => import('@/views/LoginView.vue')
const PortalView = () => import('@/views/PortalView.vue')
const DashboardView = () => import('@/views/DashboardView.vue')
const WatchlistView = () => import('@/views/WatchlistView.vue')
const KlineDetailView = () => import('@/views/KlineDetailView.vue')
const SignalListView = () => import('@/views/SignalListView.vue')
const ImportDataView = () => import('@/views/ImportDataView.vue')
const BacktestView = () => import('@/views/BacktestView.vue')
const TradeJournalView = () => import('@/views/TradeJournalView.vue')
const EtfAiAnalysisView = () => import('@/views/EtfAiAnalysisView.vue')
const SystemOpsView = () => import('@/views/SystemOpsView.vue')

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/dashboard' },
    { path: '/portal', name: 'portal', component: PortalView, meta: { title: '项目门户', public: true, publicLayout: true } },
    { path: '/login', name: 'login', component: LoginView, meta: { title: '登录', public: true, publicLayout: true } },
    { path: '/dashboard', name: 'dashboard', component: DashboardView, meta: { title: 'Dashboard' } },
    { path: '/watchlist', name: 'watchlist', component: WatchlistView, meta: { title: '自选池' } },
    { path: '/kline-detail/:symbolCode?', name: 'kline-detail', component: KlineDetailView, meta: { title: 'K 线详情' } },
    { path: '/signals', name: 'signals', component: SignalListView, meta: { title: '技术信号' } },
    { path: '/trade-journal', name: 'trade-journal', component: TradeJournalView, meta: { title: '交易日志' } },
    { path: '/backtest', name: 'backtest', component: BacktestView, meta: { title: '模拟回测' } },
    { path: '/etf-ai-analysis', name: 'etf-ai-analysis', component: EtfAiAnalysisView, meta: { title: 'AI 分析' } },
    { path: '/import', name: 'import', component: ImportDataView, meta: { title: '数据导入' } },
    { path: '/system-ops', name: 'system-ops', component: SystemOpsView, meta: { title: '系统工具' } }
  ]
})

router.beforeEach((to) => {
  const authed = isAuthenticated()
  if (to.meta.public) {
    if (authed && to.path === '/login') {
      const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : '/dashboard'
      return redirect || '/dashboard'
    }
    return true
  }
  if (!authed) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }
  return true
})

export default router
