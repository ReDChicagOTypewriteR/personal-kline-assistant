<template>
  <div class="chart-shell">
    <div v-if="!data.length" class="chart-empty">
      <div class="empty-orbit"></div>
      <strong>暂无 K 线数据</strong>
      <span>请选择 ETF</span>
    </div>
    <div v-show="data.length" class="chart-stage">
      <div class="chart-tape">
        <div>
          <span>最新收盘</span>
          <strong>{{ priceText(chartMeta.latest?.close) }}</strong>
        </div>
        <div>
          <span>涨跌幅</span>
          <strong :class="changeClass">{{ pctText(chartMeta.changePct) }}</strong>
        </div>
        <div>
          <span>最高 / 最低</span>
          <strong>{{ priceText(chartMeta.latest?.high) }} / {{ priceText(chartMeta.latest?.low) }}</strong>
        </div>
        <div>
          <span>成交量</span>
          <strong>{{ volumeText(chartMeta.latest?.volume) }}</strong>
        </div>
        <div>
          <span>成交额</span>
          <strong>{{ amountText(chartMeta.latest?.turnover) }}</strong>
        </div>
        <div>
          <span>样本</span>
          <strong>{{ chartMeta.count }} 根</strong>
        </div>
      </div>
      <div v-if="selectedDiagnostic" class="chart-diagnostic">
        <div>
          <span>日期</span>
          <strong>{{ selectedDiagnostic.date }}</strong>
        </div>
        <div>
          <span>收盘</span>
          <strong :class="selectedDiagnostic.changePct >= 0 ? 'value-up' : 'value-down'">
            {{ priceText(selectedDiagnostic.close) }} / {{ pctText(selectedDiagnostic.changePct) }}
          </strong>
        </div>
        <div>
          <span>量 / 额</span>
          <strong>{{ volumeText(selectedDiagnostic.volume) }} / {{ amountText(selectedDiagnostic.turnover) }}</strong>
        </div>
        <div>
          <span>信号</span>
          <strong :class="signalClass(selectedDiagnostic.signalType)">{{ selectedDiagnostic.signalType || '无信号' }}</strong>
        </div>
      </div>
      <div ref="chartRef" class="kline-chart"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { dispose, init, type ActionCallback, type Chart, type DeepPartial, type IndicatorCreate, type IndicatorFigure, type KLineData, type OverlayCreate, type Period, type Styles } from 'klinecharts'
import type { KLineDataItem, TechnicalSignalDTO } from '@/types'

const VOLUME_PANE_ID = 'volume_pane'
const AMOUNT_PANE_ID = 'amount_pane'
const SIGNAL_OVERLAY_GROUP = 'strategy-signals'
const TRADE_LEVEL_OVERLAY_GROUP = 'trade-levels'

const props = defineProps<{
  data: KLineDataItem[]
  period: string
  symbolCode?: string
  levels?: {
    entry?: number
    stopLoss?: number
    takeProfit?: number
  }
  signals?: TechnicalSignalDTO[]
}>()

const chartRef = ref<HTMLDivElement>()
let chart: Chart | null = null
let resizeObserver: ResizeObserver | null = null
const selectedTimestamp = ref<number>()

type SelectedDiagnostic = {
  date: string
  close: number
  changePct: number
  volume?: number
  turnover?: number
  signalType?: string
}

const normalizedRows = computed(() => normalizeRows())
const chartMeta = computed(() => {
  const rows = normalizedRows.value
  const latest = rows.at(-1)
  const previous = rows.at(-2)
  const changePct = latest && previous?.close
    ? (Number(latest.close) - Number(previous.close)) / Number(previous.close) * 100
    : undefined
  return {
    latest,
    previous,
    changePct,
    count: rows.length
  }
})

const pricePrecision = computed(() => inferPricePrecision(normalizedRows.value))
const changeClass = computed(() => {
  const value = chartMeta.value.changePct
  if (value == null) return ''
  return value > 0 ? 'value-up' : value < 0 ? 'value-down' : 'muted'
})
const signalByTimestamp = computed(() => {
  const map = new Map<number, TechnicalSignalDTO>()
  ;(props.signals || []).forEach((signal) => {
    if (!signal.tradeDate) return
    map.set(normalizeTradeDateTimestamp(signal.tradeDate), signal)
  })
  return map
})
const selectedDiagnostic = computed<SelectedDiagnostic | undefined>(() => {
  const rows = normalizedRows.value
  if (!rows.length) return undefined
  const timestamp = selectedTimestamp.value ?? rows.at(-1)?.timestamp
  const index = rows.findIndex((item) => item.timestamp === timestamp)
  const row = index >= 0 ? rows[index] : rows.at(-1)
  if (!row) return undefined
  const previous = index > 0 ? rows[index - 1] : undefined
  const changePct = previous?.close ? (Number(row.close) - Number(previous.close)) / Number(previous.close) * 100 : 0
  return {
    date: formatTimestamp(row.timestamp),
    close: Number(row.close),
    changePct,
    volume: row.volume == null ? undefined : Number(row.volume),
    turnover: row.turnover == null ? undefined : Number(row.turnover),
    signalType: signalByTimestamp.value.get(row.timestamp)?.signalType
  }
})

const handleCandleClick: ActionCallback = (payload?: unknown) => {
  const data = payload as { data?: KLineData; kLineData?: KLineData; timestamp?: number; dataIndex?: number } | undefined
  const rows = normalizedRows.value
  const row = data?.data || data?.kLineData || (typeof data?.dataIndex === 'number' ? rows[data.dataIndex] : undefined)
  const timestamp = row?.timestamp ?? data?.timestamp
  if (Number.isFinite(timestamp)) {
    selectedTimestamp.value = Number(timestamp)
  }
}

const initChart = async () => {
  await nextTick()
  if (!chartRef.value || chart) return
  chart = init(chartRef.value, {
    locale: 'zh-CN',
    timezone: 'Asia/Shanghai',
    styles: terminalKlineStyles,
    zoomAnchor: 'last_bar',
    thousandsSeparator: { sign: ',' }
  })
  chart?.setSymbol({
    ticker: props.symbolCode || 'PERSONAL-KLINE',
    pricePrecision: pricePrecision.value,
    volumePrecision: 0
  })
  chart?.setPeriod(toChartPeriod(props.period))
  chart?.setOffsetRightDistance(64)
  chart?.setBarSpace(defaultBarSpace(normalizedRows.value.length))
  chart?.setScrollEnabled(true)
  chart?.setZoomEnabled(true)
  chart?.setPaneOptions({ id: 'candle_pane', minHeight: 300, dragEnabled: true, order: 0 })
  chart?.createIndicator({
    name: 'MA',
    calcParams: [5, 10, 20, 60],
    styles: {
      lines: [
        { color: '#2f80ed', size: 1.45 },
        { color: '#14b8a6', size: 1.45 },
        { color: '#f59e0b', size: 1.35 },
        { color: '#7c3aed', size: 1.35 }
      ]
    }
  }, true)
  chart?.createIndicator({
    name: 'VOL',
    shortName: '成交量',
    calcParams: [5, 10, 20],
    styles: {
      lines: [
        { color: '#f59e0b', size: 1.35 },
        { color: '#2f80ed', size: 1.35 },
        { color: '#7a8fa0', size: 1.2 }
      ],
      bars: [
        {
          upColor: 'rgba(52, 211, 153, 0.78)',
          downColor: 'rgba(251, 113, 133, 0.78)',
          noChangeColor: 'rgba(161, 161, 170, 0.45)'
        }
      ]
    }
  }, false, createSubPaneOptions(VOLUME_PANE_ID, 1))
  chart?.createIndicator(createAmountIndicator() as IndicatorCreate, false, createSubPaneOptions(AMOUNT_PANE_ID, 2))
  chart?.subscribeAction('onCandleBarClick', handleCandleClick)

  resizeObserver = new ResizeObserver(() => chart?.resize())
  resizeObserver.observe(chartRef.value)
}

const render = async () => {
  await initChart()
  if (!chart) return
  chart.setSymbol({
    ticker: props.symbolCode || 'PERSONAL-KLINE',
    pricePrecision: pricePrecision.value,
    volumePrecision: 0
  })
  chart.setPeriod(toChartPeriod(props.period))
  chart.setBarSpace(defaultBarSpace(normalizedRows.value.length))
  chart.setOffsetRightDistance(64)
  chart.setScrollEnabled(true)
  chart.setZoomEnabled(true)
  chart.setPaneOptions({ id: 'candle_pane', minHeight: 300, dragEnabled: true, order: 0 })
  chart.setPaneOptions(createSubPaneOptions(VOLUME_PANE_ID, 1))
  chart.setPaneOptions(createSubPaneOptions(AMOUNT_PANE_ID, 2))
  chart.setDataLoader({
    getBars: ({ callback }) => {
      callback(normalizedRows.value, { backward: false, forward: false })
    }
  })
  chart.resetData()
  chart.scrollToRealTime(120)
  syncTradeLevelOverlays()
  syncSignalOverlays()
  chart.resize()
}

const syncTradeLevelOverlays = () => {
  if (!chart) return
  chart.removeOverlay({ groupId: TRADE_LEVEL_OVERLAY_GROUP })
  if (!normalizedRows.value.length) return
  const levels = [
    { label: '入场', value: props.levels?.entry, color: '#2f80ed' },
    { label: '止损', value: props.levels?.stopLoss, color: '#fb7185' },
    { label: '止盈', value: props.levels?.takeProfit, color: '#4ade80' }
  ].filter((item) => Number.isFinite(item.value))

  levels.forEach((item) => {
    chart?.createOverlay({
      name: 'simpleTag',
      groupId: TRADE_LEVEL_OVERLAY_GROUP,
      lock: true,
      zLevel: 3,
      points: [{ value: Number(item.value) }],
      extendData: item.label,
      styles: {
        line: {
          style: 'dashed',
          size: 1,
          color: item.color,
          dashedValue: [6, 4],
          smooth: false
        },
        text: {
          style: 'stroke_fill',
          color: '#050505',
          size: 12,
          family: 'Inter, PingFang SC, Arial',
          weight: 800,
          borderStyle: 'solid',
          borderDashedValue: [0, 0],
          borderSize: 1,
          borderColor: item.color,
          borderRadius: 8,
          backgroundColor: item.color,
          paddingLeft: 6,
          paddingTop: 3,
          paddingRight: 6,
          paddingBottom: 3
        }
      }
    })
  })
}

const syncSignalOverlays = () => {
  if (!chart) return
  chart.removeOverlay({ groupId: SIGNAL_OVERLAY_GROUP })
  const rows = normalizedRows.value
  if (!rows.length || !props.signals?.length) return
  const timestamps = new Set(rows.map((item) => item.timestamp))
  const overlays: OverlayCreate[] = props.signals
    .filter((signal) => signal.tradeDate && timestamps.has(normalizeTradeDateTimestamp(signal.tradeDate)))
    .map((signal) => {
      const style = signalOverlayStyle(signal.signalType)
      const value = Number(signal.closePrice ?? rows.find((item) => item.timestamp === normalizeTradeDateTimestamp(signal.tradeDate))?.close)
      return {
        name: 'simpleAnnotation',
        groupId: SIGNAL_OVERLAY_GROUP,
        paneId: 'candle_pane',
        lock: true,
        zLevel: 5,
        points: [{ timestamp: normalizeTradeDateTimestamp(signal.tradeDate), value }],
        extendData: signalOverlayLabel(signal),
        styles: {
          line: { style: 'dashed' as const, size: 1, color: style.color, dashedValue: [3, 3] },
          polygon: { color: style.color },
          text: {
            color: style.textColor,
            size: 11,
            family: 'Inter, PingFang SC, Arial',
            weight: 900,
            backgroundColor: style.backgroundColor,
            borderColor: style.color,
            borderSize: 1,
            borderRadius: 8,
            paddingLeft: 5,
            paddingTop: 2,
            paddingRight: 5,
            paddingBottom: 2
          }
        }
      }
    })
  if (overlays.length) {
    chart.createOverlay(overlays)
  }
}

const normalizeRows = () =>
  props.data
    .map((item) => ({
      timestamp: normalizeTimestamp(item.timestamp),
      open: Number(item.open),
      close: Number(item.close),
      high: Number(item.high),
      low: Number(item.low),
      volume: item.volume == null ? undefined : Number(item.volume),
      turnover: item.turnover == null ? undefined : Number(item.turnover)
    }))
    .filter((item) =>
      Number.isFinite(item.timestamp) &&
      Number.isFinite(item.open) &&
      Number.isFinite(item.close) &&
      Number.isFinite(item.high) &&
      Number.isFinite(item.low)
    )
    .map((item) => ({
      ...item,
      high: Math.max(item.open, item.close, item.high),
      low: Math.min(item.open, item.close, item.low)
    }))
    .sort((left, right) => left.timestamp - right.timestamp) as KLineData[]

const normalizeTimestamp = (value: number) => {
  const timestamp = Number(value)
  return timestamp > 0 && timestamp < 1000000000000 ? timestamp * 1000 : timestamp
}

const normalizeTradeDateTimestamp = (value: string) => new Date(`${value}T00:00:00+08:00`).getTime()

const formatTimestamp = (timestamp: number) => {
  const date = new Date(timestamp)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

const inferPricePrecision = (rows: KLineData[]) => {
  const values = rows.flatMap((item) => [item.open, item.close, item.high, item.low])
  const maxDecimals = values.reduce((max, value) => {
    const text = String(value)
    const decimals = text.includes('.') ? text.split('.')[1].replace(/0+$/, '').length : 0
    return Math.max(max, decimals)
  }, 0)
  return Math.min(Math.max(maxDecimals, 3), 4)
}

const defaultBarSpace = (count: number) => {
  if (count <= 80) return 12
  if (count <= 180) return 9
  return 7
}

const priceText = (value?: number) => Number.isFinite(value) ? Number(value).toFixed(pricePrecision.value) : '-'

const pctText = (value?: number) => Number.isFinite(value) ? `${Number(value) > 0 ? '+' : ''}${Number(value).toFixed(2)}%` : '-'

const signalClass = (value?: string) => {
  if (value === 'BUY_CANDIDATE') return 'value-up'
  if (value === 'SELL_WARNING' || value === 'AVOID') return 'value-down'
  if (value === 'WATCH') return 'value-blue'
  return 'muted'
}

const signalOverlayLabel = (signal: TechnicalSignalDTO) => {
  if (signal.signalType === 'BUY_CANDIDATE') return `买 ${signal.technicalScore ?? '-'}`
  if (signal.signalType === 'SELL_WARNING') return `卖 ${signal.technicalScore ?? '-'}`
  if (signal.signalType === 'AVOID') return `避 ${signal.technicalScore ?? '-'}`
  if (signal.signalType === 'WATCH') return `观 ${signal.technicalScore ?? '-'}`
  return `${signal.signalType || '信号'} ${signal.technicalScore ?? '-'}`
}

const signalOverlayStyle = (signalType?: string) => {
  if (signalType === 'BUY_CANDIDATE') {
    return { color: '#16a34a', backgroundColor: 'rgba(22, 163, 74, 0.12)', textColor: '#14532d' }
  }
  if (signalType === 'SELL_WARNING' || signalType === 'AVOID') {
    return { color: '#e11d48', backgroundColor: 'rgba(225, 29, 72, 0.1)', textColor: '#881337' }
  }
  if (signalType === 'WATCH') {
    return { color: '#2f80ed', backgroundColor: 'rgba(47, 128, 237, 0.1)', textColor: '#1c5fb9' }
  }
  return { color: '#7a8fa0', backgroundColor: 'rgba(122, 143, 160, 0.12)', textColor: '#4d6475' }
}

const volumeText = (value?: number) => {
  if (!Number.isFinite(value)) return '-'
  const number = Number(value)
  if (number >= 100000000) return `${(number / 100000000).toFixed(2)}亿`
  if (number >= 10000) return `${(number / 10000).toFixed(2)}万`
  return number.toLocaleString('zh-CN')
}

const amountText = (value?: number) => {
  if (!Number.isFinite(value)) return '-'
  const number = Number(value)
  if (number >= 100000000) return `${(number / 100000000).toFixed(2)}亿`
  if (number >= 10000) return `${(number / 10000).toFixed(2)}万`
  return number.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

const createSubPaneOptions = (id: string, order: number) => ({
  id,
  order,
  height: 128,
  minHeight: 82,
  dragEnabled: true,
  axis: {
    inside: true,
    position: 'right' as const,
    scrollZoomEnabled: false
  }
})

type AmountIndicatorData = {
  amount: number
  ma1?: number
  ma2?: number
  ma3?: number
  open: number
  close: number
}

const amountFigure = (): IndicatorFigure<AmountIndicatorData> => ({
  key: 'amount',
  title: '成交额: ',
  type: 'bar',
  baseValue: 0,
  styles: ({ data, indicator, defaultStyles }) => {
    const current = data.current
    const bars = indicator.styles?.bars ?? defaultStyles?.bars
    let color = bars?.[0]?.noChangeColor ?? 'rgba(161, 161, 170, 0.48)'
    if (current) {
      if (current.close > current.open) color = bars?.[0]?.upColor ?? '#34d399'
      if (current.close < current.open) color = bars?.[0]?.downColor ?? '#fb7185'
    }
    return { color }
  }
})

const dataAmount = (item?: KLineData) => {
  if (!item) return 0
  return Number(item.turnover ?? 0) || Number(item.close ?? 0) * Number(item.volume ?? 0)
}

const createAmountIndicator = (): IndicatorCreate<AmountIndicatorData, number> => ({
  name: 'AMOUNT',
  shortName: '成交额',
  series: 'volume',
  calcParams: [5, 10, 20],
  precision: 2,
  shouldFormatBigNumber: true,
  minValue: 0,
  figures: [
    { key: 'ma1', title: 'MA5: ', type: 'line' },
    { key: 'ma2', title: 'MA10: ', type: 'line' },
    { key: 'ma3', title: 'MA20: ', type: 'line' },
    amountFigure()
  ],
  regenerateFigures: (params) => [
    ...params.map((param, index) => ({ key: `ma${index + 1}`, title: `MA${param}: `, type: 'line' })),
    amountFigure()
  ],
  styles: {
    lines: [
      { color: '#f59e0b', size: 1.35 },
      { color: '#2f80ed', size: 1.35 },
      { color: '#7a8fa0', size: 1.2 }
    ],
    bars: [
      {
        upColor: 'rgba(52, 211, 153, 0.78)',
        downColor: 'rgba(251, 113, 133, 0.78)',
        noChangeColor: 'rgba(161, 161, 170, 0.45)'
      }
    ]
  },
  calc: (dataList, indicator) => {
    const sums: number[] = []
    return dataList.map((item, index) => {
      const amount = dataAmount(item)
      const result: AmountIndicatorData = {
        amount,
        open: item.open,
        close: item.close
      }
      indicator.calcParams.forEach((periodValue, paramIndex) => {
        sums[paramIndex] = (sums[paramIndex] ?? 0) + amount
        if (index >= periodValue - 1) {
          const key = `ma${paramIndex + 1}` as keyof Pick<AmountIndicatorData, 'ma1' | 'ma2' | 'ma3'>
          result[key] = sums[paramIndex] / periodValue
          sums[paramIndex] -= dataAmount(dataList[index - (periodValue - 1)])
        }
      })
      return result
    })
  }
})

const toChartPeriod = (value: string): Period => {
  switch (value) {
    case '1m':
      return { type: 'minute', span: 1 }
    case '5m':
      return { type: 'minute', span: 5 }
    case '15m':
      return { type: 'minute', span: 15 }
    case '1h':
      return { type: 'hour', span: 1 }
    case '2h':
      return { type: 'hour', span: 2 }
    case '4h':
      return { type: 'hour', span: 4 }
    case 'W':
      return { type: 'week', span: 1 }
    case 'M':
      return { type: 'month', span: 1 }
    case 'Y':
      return { type: 'year', span: 1 }
    default:
      return { type: 'day', span: 1 }
  }
}

const terminalKlineStyles: DeepPartial<Styles> = {
  grid: {
    show: true,
    horizontal: {
      show: true,
      style: 'dashed',
      size: 1,
      color: 'rgba(148, 173, 198, 0.22)',
      dashedValue: [4, 4]
    },
    vertical: {
      show: true,
      style: 'dashed',
      size: 1,
      color: 'rgba(148, 173, 198, 0.16)',
      dashedValue: [4, 4]
    }
  },
  candle: {
    type: 'candle_solid',
    bar: {
      compareRule: 'current_open',
      upColor: '#34d399',
      downColor: '#fb7185',
      noChangeColor: '#7a8fa0',
      upBorderColor: '#34d399',
      downBorderColor: '#fb7185',
      noChangeBorderColor: '#7a8fa0',
      upWickColor: '#34d399',
      downWickColor: '#fb7185',
      noChangeWickColor: '#7a8fa0'
    },
    priceMark: {
      high: { color: '#4d6475' },
      low: { color: '#4d6475' },
      last: {
        show: true,
        upColor: '#34d399',
        downColor: '#fb7185',
        noChangeColor: '#7a8fa0',
        line: {
          show: true,
          style: 'dashed',
          size: 1,
          dashedValue: [4, 4]
        },
        text: {
          show: true,
          color: '#050505',
          size: 12,
          borderColor: 'rgba(47, 128, 237, 0.22)',
          borderSize: 1,
          borderRadius: 8,
          paddingLeft: 6,
          paddingTop: 3,
          paddingRight: 6,
          paddingBottom: 3
        }
      }
    },
    tooltip: {
      showRule: 'follow_cross',
      showType: 'rect',
      offsetLeft: 8,
      offsetTop: 8,
      offsetRight: 8,
      offsetBottom: 8,
      title: {
        show: true,
        color: '#4d6475',
        size: 12,
        family: 'Inter, PingFang SC, Arial',
        weight: 800,
        marginLeft: 0,
        marginTop: 0,
        marginRight: 0,
        marginBottom: 8,
        template: '{ticker} · {period}'
      },
      legend: {
        color: '#123044',
        size: 12,
        family: 'Inter, PingFang SC, Arial',
        weight: 700,
        marginLeft: 0,
        marginTop: 4,
        marginRight: 0,
        marginBottom: 0,
        defaultValue: '-',
        template: [
          { title: '时间', value: '{time}' },
          { title: '开', value: '{open}' },
          { title: '高', value: '{high}' },
          { title: '低', value: '{low}' },
          { title: '收', value: '{close}' },
          { title: '量', value: '{volume}' }
        ]
      },
      rect: {
        position: 'pointer',
        paddingLeft: 12,
        paddingTop: 10,
        paddingRight: 12,
        paddingBottom: 10,
        offsetLeft: 12,
        offsetTop: 12,
        offsetRight: 12,
        offsetBottom: 12,
        borderRadius: 14,
        borderSize: 1,
        borderColor: 'rgba(148, 173, 198, 0.36)',
        color: 'rgba(255, 255, 255, 0.98)'
      }
    }
  },
  indicator: {
    ohlc: {
      compareRule: 'current_open',
      upColor: '#34d399',
      downColor: '#fb7185',
      noChangeColor: '#7a8fa0'
    },
    tooltip: {
      showRule: 'follow_cross',
      showType: 'standard',
      title: {
        show: true,
        showName: true,
        showParams: true,
        color: '#4d6475',
        size: 12,
        family: 'Inter, PingFang SC, Arial',
        weight: 800,
        marginLeft: 0,
        marginTop: 0,
        marginRight: 0,
        marginBottom: 4
      },
      legend: {
        color: '#123044',
        size: 12,
        family: 'Inter, PingFang SC, Arial',
        weight: 700,
        marginLeft: 0,
        marginTop: 2,
        marginRight: 8,
        marginBottom: 0
      }
    },
    lastValueMark: {
      show: false
    }
  },
  xAxis: {
    show: true,
    size: 'auto',
    axisLine: { show: true, color: 'rgba(148, 173, 198, 0.34)', size: 1 },
    tickLine: { show: false },
    tickText: {
      show: true,
      color: '#7a8fa0',
      size: 11,
      family: 'Inter, PingFang SC, Arial',
      weight: 700,
      marginStart: 6,
      marginEnd: 6
    }
  },
  yAxis: {
    show: true,
    size: 'auto',
    axisLine: { show: true, color: 'rgba(148, 173, 198, 0.3)', size: 1 },
    tickLine: { show: false },
    tickText: {
      show: true,
      color: '#7a8fa0',
      size: 11,
      family: 'Inter, PingFang SC, Arial',
      weight: 700,
      marginStart: 6,
      marginEnd: 6
    }
  },
  separator: {
    size: 5,
    color: 'rgba(148, 173, 198, 0.22)',
    fill: true,
    activeBackgroundColor: 'rgba(47, 128, 237, 0.12)'
  },
  crosshair: {
    show: true,
    horizontal: {
      show: true,
      line: {
        show: true,
        style: 'solid',
        size: 1,
        color: 'rgba(47, 128, 237, 0.42)',
        dashedValue: [0, 0]
      },
      text: {
        show: true,
        color: '#ffffff',
        size: 12,
        family: 'Inter, PingFang SC, Arial',
        weight: 800,
        borderStyle: 'solid',
        borderDashedValue: [0, 0],
        borderSize: 1,
        borderColor: 'rgba(47, 128, 237, 0.28)',
        borderRadius: 8,
        backgroundColor: '#2f80ed',
        paddingLeft: 6,
        paddingTop: 3,
        paddingRight: 6,
        paddingBottom: 3
      }
    },
    vertical: {
      show: true,
      line: {
        show: true,
        style: 'solid',
        size: 1,
        color: 'rgba(47, 128, 237, 0.42)',
        dashedValue: [0, 0]
      },
      text: {
        show: true,
        color: '#ffffff',
        size: 12,
        family: 'Inter, PingFang SC, Arial',
        weight: 800,
        borderStyle: 'solid',
        borderDashedValue: [0, 0],
        borderSize: 1,
        borderColor: 'rgba(47, 128, 237, 0.28)',
        borderRadius: 8,
        backgroundColor: '#2f80ed',
        paddingLeft: 6,
        paddingTop: 3,
        paddingRight: 6,
        paddingBottom: 3
      }
    }
  }
}

onMounted(render)

onBeforeUnmount(() => {
  chart?.unsubscribeAction('onCandleBarClick', handleCandleClick)
  resizeObserver?.disconnect()
  if (chartRef.value) {
    dispose(chartRef.value)
  }
  chart = null
})

watch(() => [props.data, props.period, props.symbolCode, props.levels, props.signals], () => {
  selectedTimestamp.value = undefined
  render()
}, { deep: true })
</script>

<style scoped>
.chart-shell {
  position: relative;
  min-height: 780px;
  width: 100%;
}

.chart-stage {
  min-height: 780px;
  border-radius: 18px;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(246, 250, 253, 0.92)),
    #ffffff;
  border: 1px solid rgba(129, 153, 174, 0.3);
}

.chart-tape {
  display: grid;
  grid-template-columns: repeat(6, minmax(120px, 1fr));
  gap: 1px;
  background: rgba(246, 250, 253, 0.92);
  border-bottom: 1px solid rgba(129, 153, 174, 0.28);
}

.chart-tape > div {
  min-width: 0;
  padding: 10px 12px;
  background: #ffffff;
}

.chart-tape span {
  display: block;
  color: #7a8fa0;
  font-size: 11px;
  line-height: 1;
  margin-bottom: 7px;
}

.chart-tape strong {
  display: block;
  color: #123044;
  font-size: 15px;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.chart-diagnostic {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 1px;
  background: rgba(246, 250, 253, 0.92);
  border-bottom: 1px solid rgba(129, 153, 174, 0.28);
}

.chart-diagnostic > div {
  min-width: 0;
  padding: 8px 12px;
  background: #ffffff;
}

.chart-diagnostic span {
  display: block;
  color: #7a8fa0;
  font-size: 11px;
  line-height: 1;
  margin-bottom: 6px;
}

.chart-diagnostic strong {
  display: block;
  color: #123044;
  font-size: 13px;
  line-height: 1.15;
  font-weight: 900;
  font-variant-numeric: tabular-nums;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.kline-chart {
  width: 100%;
  height: 650px;
  overflow: hidden;
  background: rgba(246, 250, 253, 0.9);
}

.chart-empty {
  height: 620px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 10px;
  color: #123044;
  background: rgba(246, 250, 253, 0.92);
  border: 1px dashed rgba(129, 153, 174, 0.38);
  border-radius: 18px;
}

.chart-empty strong {
  color: #123044;
  font-size: 18px;
}

.chart-empty span {
  color: #7a8fa0;
  font-size: 13px;
}

.empty-orbit {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  border: 1px solid rgba(47, 128, 237, 0.18);
  background:
    linear-gradient(90deg, transparent 8px, rgba(47, 128, 237, 0.16) 9px, transparent 10px),
    linear-gradient(0deg, transparent 8px, rgba(20, 184, 166, 0.12) 9px, transparent 10px),
    #eef6fb;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

@media (max-width: 1200px) {
  .chart-shell,
  .chart-stage,
  .chart-empty {
    height: 650px;
    min-height: 650px;
  }

  .kline-chart {
    height: 506px;
  }

  .chart-tape,
  .chart-diagnostic {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 780px) {
  .chart-shell,
  .chart-stage,
  .chart-empty {
    height: 560px;
    min-height: 560px;
  }

  .kline-chart {
    height: 308px;
  }

  .chart-tape,
  .chart-diagnostic {
    grid-template-columns: 1fr;
  }
}
</style>
