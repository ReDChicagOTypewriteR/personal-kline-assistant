export const strategyName = (value?: string) => (value === 'ETF_DCA' ? 'ETF 定投策略' : 'ETF 趋势策略')

export const tradeActionName = (action: string) => (action === 'BUY' ? '买入' : action === 'SELL' ? '卖出' : action)

export const signalLabel = (value?: string) => {
  const labels: Record<string, string> = {
    BUY_CANDIDATE: '买入候选',
    ALLOW_SIMULATION: '允许模拟',
    BUY_WATCH: '买入观察',
    WATCH: '观察',
    WATCH_ONLY: '仅观察',
    AVOID: '规避',
    BLOCK: '阻断',
    SELL_WARNING: '卖出预警',
    REDUCE_POSITION: '降低仓位',
    NO_ACTION: '无动作',
    NEUTRAL: '中性'
  }
  return value ? labels[value] || value : ''
}

export const followUpLabel = (value?: string) => {
  if (value === 'WIN') return '符合预期'
  if (value === 'LOSS') return '不符合预期'
  if (value === 'FLAT') return '基本持平'
  if (value === 'INVALID') return '信号失效'
  return '待观察'
}

export const followUpTagClass = (value?: string) => {
  if (value === 'WIN') return 'executed-tag'
  if (value === 'LOSS' || value === 'INVALID') return 'loss-tag'
  if (value === 'FLAT') return 'flat-tag'
  return 'pending-tag'
}
