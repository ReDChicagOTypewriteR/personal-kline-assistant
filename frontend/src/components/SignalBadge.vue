<template>
  <span class="signal-badge" :class="badgeClass">
    <span class="signal-dot"></span>
    {{ displayText }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { signalLabel } from '@/utils/labels'

const props = defineProps<{ signalType?: string }>()

const displayText = computed(() => signalLabel(props.signalType || 'NEUTRAL') || '中性')

const badgeClass = computed(() => {
  switch (props.signalType) {
    case 'BUY_CANDIDATE':
    case 'ALLOW_SIMULATION':
      return 'signal-buy'
    case 'BUY_WATCH':
    case 'WATCH':
    case 'WATCH_ONLY':
      return 'signal-watch'
    case 'AVOID':
    case 'BLOCK':
      return 'signal-avoid'
    case 'SELL_WARNING':
    case 'REDUCE_POSITION':
      return 'signal-sell'
    case 'NO_ACTION':
      return 'signal-muted'
    default:
      return 'signal-neutral'
  }
})
</script>

<style scoped>
.signal-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 22px;
  padding: 0 8px;
  border: 1px solid;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 850;
  letter-spacing: 0;
  white-space: nowrap;
  backdrop-filter: blur(10px);
  line-height: 1;
  box-shadow: none;
}

.signal-dot {
  width: 6px;
  height: 6px;
  border-radius: 999px;
  background: currentColor;
  box-shadow: 0 0 12px currentColor;
}

.signal-buy {
  color: #15803d;
  background: rgba(22, 163, 74, 0.1);
  border-color: rgba(22, 163, 74, 0.24);
}

.signal-watch {
  color: #1c5fb9;
  background: rgba(47, 128, 237, 0.1);
  border-color: rgba(47, 128, 237, 0.24);
}

.signal-avoid {
  color: #fb7185;
  background: rgba(251, 113, 133, 0.12);
  border-color: rgba(251, 113, 133, 0.3);
}

.signal-sell {
  color: #b45309;
  background: rgba(217, 119, 6, 0.1);
  border-color: rgba(217, 119, 6, 0.24);
}

.signal-neutral {
  color: #7a8fa0;
  background: rgba(122, 143, 160, 0.1);
  border-color: rgba(122, 143, 160, 0.2);
}

.signal-muted {
  color: #7a8fa0;
  background: rgba(122, 143, 160, 0.1);
  border-color: rgba(122, 143, 160, 0.2);
}
</style>
