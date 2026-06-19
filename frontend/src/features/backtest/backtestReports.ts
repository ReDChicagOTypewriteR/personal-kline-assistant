import type { BacktestRecordSummaryDTO, BacktestResultDTO, BacktestRunRequest, BacktestTradeDTO } from '@/types'
import {
  diffRate,
  formatMoney,
  formatNumber,
  formatPercent,
  formatSignedMoney,
  formatSignedPercent
} from '@/utils/formatters'
import { strategyName, tradeActionName } from '@/utils/labels'

export interface BacktestReportContext {
  data: BacktestResultDTO
  form: BacktestRunRequest
  selectedRange?: [string, string]
}

export interface BacktestComparisonStats {
  outperformCount: number
  bestExcess?: BacktestRecordSummaryDTO
  bestReturn?: BacktestRecordSummaryDTO
  lowestDrawdown?: BacktestRecordSummaryDTO
}

export const buildBacktestAiReport = ({ data, form, selectedRange }: BacktestReportContext) => {
  const summary = data.summary
  const position = data.position
  const benchmark = data.benchmark
  const excess = diffRate(summary.totalReturnRate, benchmark?.totalReturnRate)
  const best = bestTrade(data.trades)
  const worst = worstTrade(data.trades)
  const drawdown = data.equityCurve.length ? maxDrawdownPoint(data.equityCurve) : undefined
  const [requestStart, requestEnd] = selectedRange || []
  const closedTrades = summary.winCount + summary.lossCount
  const quality = data.dataQuality

  return [
    '# ETF 回测结果分析请求',
    '',
    '请你以量化交易辅助系统分析师的角度，帮我评估这次 ETF 回测结果。重点关注策略稳定性、收益来源、回撤风险、交易频率、是否适合进入准实盘观察，以及后续应该补充的风控规则。',
    '',
    '## 一、回测基本信息',
    `- 标的代码：${data.symbolCode}`,
    `- 策略类型：${strategyName(data.strategyType)}`,
    `- 实际回测区间：${data.start} 至 ${data.end}`,
    `- 页面选择区间：${requestStart || '-'} 至 ${requestEnd || '-'}`,
    `- 成交模型：${data.executionMode === 'NEXT_OPEN' ? '信号日收盘后生成委托，下一交易日开盘成交' : '收盘价成交'}`,
    `- 初始资金：${formatMoney(form.initialCapital)}`,
    `- 仓位比例：${form.positionRatio ?? '-'}`,
    `- 手续费率：${form.feeRate ?? '-'}`,
    `- 滑点率：${form.slippageRate ?? '-'}`,
    `- 最小交易单位：${form.lotSize ?? '-'}`,
    '',
    '## 二、数据质量检查',
    `- K 线数量：${quality?.totalRows ?? '-'}`,
    `- 价格异常行数：${quality?.invalidPriceRows ?? '-'}`,
    `- 日期长断档次数：${quality?.longCalendarGapCount ?? '-'}`,
    `- 缺少成交量行数：${quality?.missingVolumeRows ?? '-'}`,
    `- 缺少成交额行数：${quality?.missingAmountRows ?? '-'}`,
    `- 单日异常涨跌行数：${quality?.suspiciousChangeRows ?? '-'}`,
    `- 检查结论：${quality?.passed === false ? '未通过' : '通过或仅有警告'}`,
    `- 质量提示：${quality?.warnings?.length ? quality.warnings.join('；') : '-'}`,
    '',
    '## 三、核心绩效',
    `- 最终权益：${formatMoney(summary.finalEquity)}`,
    `- 总收益率：${formatPercent(summary.totalReturnRate)}`,
    `- 年化收益率：${formatPercent(summary.annualizedReturnRate)}`,
    `- 最大回撤：${formatPercent(summary.maxDrawdownRate)}`,
    `- 已实现盈亏：${formatSignedMoney(summary.realizedPnl)}`,
    `- 交易记录数：${summary.tradeCount}`,
    `- 已闭合交易数：${closedTrades}`,
    `- 胜率：${formatPercent(summary.winRate)}（${summary.winCount} 赢 / ${summary.lossCount} 输）`,
    `- 盈亏比：${summary.profitLossRatio == null ? '-' : formatNumber(summary.profitLossRatio)}`,
    '',
    '## 四、买入持有基准',
    `- 基准最终权益：${formatMoney(benchmark?.finalEquity)}`,
    `- 基准总收益率：${formatPercent(benchmark?.totalReturnRate)}`,
    `- 基准年化收益率：${formatPercent(benchmark?.annualizedReturnRate)}`,
    `- 基准最大回撤：${formatPercent(benchmark?.maxDrawdownRate)}`,
    `- 策略超额收益：${formatSignedPercent(excess)}`,
    `- 基准持仓数量：${benchmark?.shares ?? '-'}`,
    `- 基准首日价格：${formatNumber(benchmark?.startPrice)}`,
    `- 基准末日价格：${formatNumber(benchmark?.endPrice)}`,
    '',
    '## 五、当前模拟持仓',
    `- 状态：${position.status === 'HOLDING' ? '持仓中' : '空仓'}`,
    `- 持仓数量：${position.shares}`,
    `- 均价：${formatNumber(position.avgCost)}`,
    `- 最新价：${formatNumber(position.marketPrice)}`,
    `- 市值：${formatMoney(position.marketValue)}`,
    `- 浮动盈亏：${formatSignedMoney(position.unrealizedPnl)}`,
    `- 浮动收益率：${formatSignedPercent(position.unrealizedPnlRate)}`,
    '',
    '## 六、风险与交易特征',
    `- 最大回撤日期：${drawdown?.tradeDate || '-'}`,
    `- 最大回撤点总权益：${drawdown ? formatMoney(drawdown.totalEquity) : '-'}`,
    `- 最好一笔卖出：${best ? `${best.tradeDate}，${formatSignedPercent(best.pnlRate)}，盈亏 ${formatSignedMoney(best.pnl)}` : '-'}`,
    `- 最差一笔卖出：${worst ? `${worst.tradeDate}，${formatSignedPercent(worst.pnlRate)}，盈亏 ${formatSignedMoney(worst.pnl)}` : '-'}`,
    '',
    '## 七、交易明细',
    data.trades.length ? tradeSample(data.trades) : '- 无交易记录',
    '',
    '## 八、请重点回答',
    '1. 这套 ETF 策略是否跑赢买入持有基准？如果没有，主要输在哪里？',
    '2. 策略收益是否稳定，还是依赖少数大行情？',
    '3. 最大回撤是否偏高，我是否能承受这种波动？',
    '4. 胜率、盈亏比和交易次数是否健康？',
    '5. 是否存在过度交易或震荡行情反复止损的问题？',
    '6. 当前持仓是否应该继续观察，还是需要更严格的退出规则？',
    '7. 如果进入准实盘观察，建议增加哪些风控规则和数据验证？',
    '8. 这次回测还缺少哪些关键指标，下一版系统应该补充什么？'
  ].join('\n')
}

export const buildBacktestComparisonReport = (rows: BacktestRecordSummaryDTO[], stats: BacktestComparisonStats) => [
  '# ETF 回测记录对比分析请求',
  '',
  '请你以量化交易辅助系统分析师的角度，帮我横向比较这些回测记录。重点关注哪些标的/参数更稳定，哪些只是短期收益高但回撤或交易频率不可接受。',
  '',
  '## 一、对比概览',
  `- 对比记录数：${rows.length}`,
  `- 跑赢买入持有数量：${stats.outperformCount}`,
  `- 最佳超额收益：${stats.bestExcess ? `${stats.bestExcess.symbolCode} ${formatSignedPercent(stats.bestExcess.excessReturnRate)}` : '-'}`,
  `- 最高策略收益：${stats.bestReturn ? `${stats.bestReturn.symbolCode} ${formatPercent(stats.bestReturn.totalReturnRate)}` : '-'}`,
  `- 最低回撤：${stats.lowestDrawdown ? `${stats.lowestDrawdown.symbolCode} ${formatPercent(stats.lowestDrawdown.maxDrawdownRate)}` : '-'}`,
  '',
  '## 二、明细数据',
  '| 标的 | 策略 | 区间 | 策略收益 | 基准收益 | 超额收益 | 最大回撤 | 基准回撤 | 胜率 | 交易数 |',
  '| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |',
  ...rows.map((item) =>
    `| ${item.symbolCode} | ${strategyName(item.strategyType)} | ${item.startDate || '-'} 至 ${item.endDate || '-'} | ${formatPercent(item.totalReturnRate)} | ${formatPercent(item.benchmarkReturnRate)} | ${formatSignedPercent(item.excessReturnRate)} | ${formatPercent(item.maxDrawdownRate)} | ${formatPercent(item.benchmarkMaxDrawdownRate)} | ${formatPercent(item.winRate)} | ${item.tradeCount ?? '-'} |`
  ),
  '',
  '## 三、请重点回答',
  '1. 哪些记录真正跑赢了买入持有基准？',
  '2. 哪些标的收益高但回撤或交易次数过高？',
  '3. 哪些结果更适合进入准实盘观察？',
  '4. 当前策略是否更适合趋势强的 ETF，还是对宽基 ETF 也有效？',
  '5. 下一轮参数优化应该优先调整什么？'
].join('\n')

const formatReportTrade = (trade: BacktestTradeDTO, index: number) => {
  const pnlText = trade.pnl == null ? '-' : `${formatSignedMoney(trade.pnl)} / ${formatSignedPercent(trade.pnlRate)}`
  return `${index + 1}. ${trade.tradeDate} ${tradeActionName(trade.action)}，价格 ${formatNumber(trade.price)}，数量 ${trade.shares}，成交额 ${formatMoney(trade.amount)}，费用 ${formatMoney(trade.fee)}，盈亏 ${pnlText}，原因：${trade.reason || '-'}`
}

const tradeSample = (trades: BacktestTradeDTO[]) => {
  if (trades.length <= 80) {
    return trades.map(formatReportTrade).join('\n')
  }
  const head = trades.slice(0, 40).map(formatReportTrade).join('\n')
  const tail = trades.slice(-20).map((trade, index) => formatReportTrade(trade, trades.length - 20 + index)).join('\n')
  return `${head}\n\n... 中间 ${trades.length - 60} 条成交记录已省略，后续版本可下载完整 CSV ...\n\n${tail}`
}

const bestTrade = (trades: BacktestTradeDTO[]) =>
  trades.filter((item) => item.pnlRate != null).sort((a, b) => (b.pnlRate || 0) - (a.pnlRate || 0))[0]

const worstTrade = (trades: BacktestTradeDTO[]) =>
  trades.filter((item) => item.pnlRate != null).sort((a, b) => (a.pnlRate || 0) - (b.pnlRate || 0))[0]

const maxDrawdownPoint = (curve: BacktestResultDTO['equityCurve']) =>
  curve.reduce((target, item) => (item.drawdown < target.drawdown ? item : target), curve[0])
