export const formatNumber = (value?: number, digits = 2) =>
  value === undefined || value === null || !Number.isFinite(Number(value))
    ? '-'
    : Number(value).toFixed(digits)

export const formatInteger = (value?: number) =>
  value === undefined || value === null || !Number.isFinite(Number(value))
    ? '-'
    : String(Math.trunc(Number(value)))

export const formatMoney = (value?: number) =>
  value === undefined || value === null || !Number.isFinite(Number(value))
    ? '-'
    : Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

export const formatPercent = (value?: number) =>
  value === undefined || value === null || !Number.isFinite(Number(value))
    ? '-'
    : `${Number(value).toFixed(2)}%`

export const formatSignedMoney = (value?: number) => {
  if (value === undefined || value === null || !Number.isFinite(Number(value))) return '-'
  const abs = formatMoney(Math.abs(Number(value)))
  return Number(value) >= 0 ? `+${abs}` : `-${abs}`
}

export const formatSignedPercent = (value?: number) => {
  if (value === undefined || value === null || !Number.isFinite(Number(value))) return '-'
  return Number(value) >= 0 ? `+${formatPercent(value)}` : formatPercent(value)
}

export const classBySign = (value?: number) =>
  value === undefined || value === null || Number(value) >= 0 ? 'value-up' : 'value-down'

export const riskScoreClass = (value?: number) => {
  if (value === undefined || value === null) return ''
  if (value >= 70) return 'value-down'
  if (value >= 40) return 'value-warn'
  return 'value-up'
}

export const rsiClass = (value?: number) => {
  if (value === undefined || value === null) return ''
  if (value > 75 || value < 40) return 'value-down'
  if (value >= 45 && value <= 70) return 'value-up'
  return 'value-warn'
}

export const diffRate = (left?: number, right?: number) =>
  left === undefined || left === null || right === undefined || right === null
    ? undefined
    : Number(left) - Number(right)
