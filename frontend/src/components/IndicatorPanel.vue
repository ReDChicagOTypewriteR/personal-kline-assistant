<template>
  <div class="indicator-grid">
    <div v-for="item in items" :key="item.label" class="indicator-item" :class="item.state">
      <div class="indicator-label">{{ item.label }}</div>
      <strong>{{ format(item.value, item.suffix) }}</strong>
      <span>{{ item.note }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatNumber } from '@/utils/formatters'
import type { IndicatorSnapshotDTO } from '@/types'

const props = defineProps<{
  indicator?: IndicatorSnapshotDTO
  closePrice?: number
}>()

interface IndicatorItem {
  label: string
  value?: number
  note: string
  state: string
  suffix?: string
}

const items = computed<IndicatorItem[]>(() => [
  maItem('MA5', props.indicator?.ma5),
  maItem('MA10', props.indicator?.ma10),
  maItem('MA20', props.indicator?.ma20),
  maItem('MA60', props.indicator?.ma60),
  rsiItem(props.indicator?.rsi14),
  { label: 'ATR14', value: props.indicator?.atr14, note: 'Volatility', state: 'state-blue' },
  volumeItem(props.indicator?.volumeRatio),
  distanceItem(props.indicator?.distanceToMa20)
])

const maItem = (label: string, value?: number) => {
  const strong = props.closePrice != null && value != null && props.closePrice > value
  return {
    label,
    value,
    note: strong ? 'Above price support' : 'Trend reference',
    state: strong ? 'state-good' : 'state-blue'
  }
}

const rsiItem = (value?: number) => {
  if (value == null) return { label: 'RSI14', value, note: 'Waiting data', state: 'state-neutral' }
  if (value > 75) return { label: 'RSI14', value, note: 'Overheated', state: 'state-danger' }
  if (value >= 45 && value <= 70) return { label: 'RSI14', value, note: 'Healthy', state: 'state-good' }
  if (value < 40) return { label: 'RSI14', value, note: 'Weak momentum', state: 'state-danger' }
  return { label: 'RSI14', value, note: 'Neutral', state: 'state-blue' }
}

const volumeItem = (value?: number) => {
  if (value == null) return { label: 'Volume Ratio', value, note: 'Waiting data', state: 'state-neutral' }
  return value >= 1
    ? { label: 'Volume Ratio', value, note: 'Active volume', state: 'state-good' }
    : { label: 'Volume Ratio', value, note: 'Low momentum', state: 'state-warn' }
}

const distanceItem = (value?: number) => {
  if (value == null) return { label: 'Distance MA20', value, note: 'Waiting data', state: 'state-neutral', suffix: '%' }
  const abs = Math.abs(value)
  if (abs > 12) return { label: 'Distance MA20', value, note: 'Extended risk', state: 'state-danger', suffix: '%' }
  if (abs > 8) return { label: 'Distance MA20', value, note: 'Chasing risk', state: 'state-warn', suffix: '%' }
  return { label: 'Distance MA20', value, note: 'Near trend mean', state: 'state-good', suffix: '%' }
}

const format = (value?: number, suffix = '') => {
  const text = formatNumber(value)
  return text === '-' ? text : `${text}${suffix}`
}
</script>

<style scoped>
.indicator-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.indicator-item {
  position: relative;
  min-height: 78px;
  padding: 10px;
  overflow: hidden;
  border-radius: 12px;
  border: 1px solid rgba(129, 153, 174, 0.32);
  background: rgba(246, 250, 253, 0.92);
}

.indicator-item::after {
  display: none;
}

.indicator-label {
  color: #1c5fb9;
  font-size: 12px;
  font-weight: 800;
}

.indicator-item strong {
  display: block;
  color: currentColor;
  font-size: 18px;
  margin-top: 6px;
  text-shadow: none;
}

.indicator-item span {
  display: block;
  color: #7a8fa0;
  font-size: 12px;
  margin-top: 4px;
}

.state-good {
  color: #16a34a;
}

.state-blue {
  color: #1c5fb9;
}

.state-warn {
  color: #fbbf24;
}

.state-danger {
  color: #fb7185;
}

.state-neutral {
  color: #7a8fa0;
}

@media (max-width: 1200px) {
  .indicator-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 780px) {
  .indicator-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
