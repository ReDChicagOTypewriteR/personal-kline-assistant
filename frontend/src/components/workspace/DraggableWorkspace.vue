<template>
  <div class="workspace-shell">
    <div class="workspace-toolbar">
      <span>工作台布局</span>
      <div class="workspace-toolbar-actions">
        <a-button size="small" @click="resetLayout">
          <template #icon><ReloadOutlined /></template>
          恢复默认
        </a-button>
      </div>
    </div>

    <div ref="containerRef" class="draggable-workspace" :style="{ height: `${containerHeight}px` }">
      <section
        v-for="item in layout"
        :key="item.id"
        class="workspace-item"
        :class="{ dragging: active?.id === item.id && active.mode === 'drag', resizing: active?.id === item.id && active.mode === 'resize' }"
        :style="itemStyle(item)"
      >
        <header class="workspace-item-head" @pointerdown="startDrag($event, item)">
          <span>{{ item.title }}</span>
        </header>
        <div class="workspace-item-body">
          <slot :name="item.id"></slot>
        </div>
        <button class="workspace-resize-edge workspace-resize-top" aria-label="向上调整卡片高度" @pointerdown="startResize($event, item, 'top')"></button>
        <button class="workspace-resize-edge workspace-resize-bottom" aria-label="向下调整卡片高度" @pointerdown="startResize($event, item, 'bottom')"></button>
        <button class="workspace-resize-handle" aria-label="调整卡片宽高" @pointerdown="startResize($event, item, 'bottom-right')"></button>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { ReloadOutlined } from '@ant-design/icons-vue'

interface WorkspaceItem {
  id: string
  title: string
  x: number
  y: number
  w: number
  h: number
  minW?: number
  minH?: number
}

type InteractionMode = 'drag' | 'resize'
type ResizeDirection = 'top' | 'bottom' | 'bottom-right'

interface ActiveInteraction {
  id: string
  mode: InteractionMode
  direction?: ResizeDirection
  startX: number
  startY: number
  startItem: WorkspaceItem
}

const props = withDefaults(defineProps<{
  storageKey: string
  items: WorkspaceItem[]
  columns?: number
  rowHeight?: number
  gap?: number
}>(), {
  columns: 12,
  rowHeight: 82,
  gap: 14
})

const emit = defineEmits<{
  layoutChange: [items: WorkspaceItem[]]
}>()

const containerRef = ref<HTMLElement>()
const containerWidth = ref(0)
const layout = ref<WorkspaceItem[]>([])
const active = ref<ActiveInteraction>()
let resizeObserver: ResizeObserver | undefined

const columnWidth = computed(() => {
  if (!containerWidth.value) return 0
  return (containerWidth.value - props.gap * (props.columns - 1)) / props.columns
})

const containerHeight = computed(() => {
  const bottom = layout.value.reduce((max, item) => Math.max(max, item.y + item.h), 0)
  return Math.max(bottom * props.rowHeight + Math.max(0, bottom - 1) * props.gap, props.rowHeight)
})

const itemStyle = (item: WorkspaceItem) => {
  const col = columnWidth.value
  const left = item.x * (col + props.gap)
  const top = item.y * (props.rowHeight + props.gap)
  const width = item.w * col + (item.w - 1) * props.gap
  const height = item.h * props.rowHeight + (item.h - 1) * props.gap
  return {
    left: `${left}px`,
    top: `${top}px`,
    width: `${width}px`,
    height: `${height}px`
  }
}

const loadLayout = () => {
  const fallback = cloneLayout(props.items)
  try {
    const raw = localStorage.getItem(props.storageKey)
    if (!raw) {
      layout.value = fallback
      return
    }
    const saved = JSON.parse(raw) as WorkspaceItem[]
    const byId = new Map(saved.map((item) => [item.id, item]))
    layout.value = fallback.map((item) => ({ ...item, ...(byId.get(item.id) || {}) }))
  } catch {
    layout.value = fallback
  }
}

const saveLayout = async () => {
  localStorage.setItem(props.storageKey, JSON.stringify(layout.value))
  emit('layoutChange', cloneLayout(layout.value))
  await nextTick()
  window.dispatchEvent(new Event('resize'))
}

const resetLayout = async () => {
  layout.value = cloneLayout(props.items)
  await saveLayout()
}

const startDrag = (event: PointerEvent, item: WorkspaceItem) => {
  if (event.button !== 0) return
  const target = event.target as HTMLElement
  if (target.closest('button, a, input, textarea, select, .ant-table, .workspace-resize-handle, .workspace-resize-edge')) return
  event.preventDefault()
  active.value = { id: item.id, mode: 'drag', startX: event.clientX, startY: event.clientY, startItem: { ...item } }
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', endInteraction, { once: true })
}

const startResize = (event: PointerEvent, item: WorkspaceItem, direction: ResizeDirection) => {
  if (event.button !== 0) return
  event.preventDefault()
  event.stopPropagation()
  active.value = { id: item.id, mode: 'resize', direction, startX: event.clientX, startY: event.clientY, startItem: { ...item } }
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', endInteraction, { once: true })
}

const onPointerMove = (event: PointerEvent) => {
  if (!active.value || !columnWidth.value) return
  const index = layout.value.findIndex((item) => item.id === active.value?.id)
  if (index < 0) return
  const colStep = columnWidth.value + props.gap
  const rowStep = props.rowHeight + props.gap
  const dx = Math.round((event.clientX - active.value.startX) / colStep)
  const dy = Math.round((event.clientY - active.value.startY) / rowStep)
  const next = { ...layout.value[index] }

  if (active.value.mode === 'drag') {
    next.x = clamp(active.value.startItem.x + dx, 0, props.columns - next.w)
    next.y = Math.max(0, active.value.startItem.y + dy)
  } else {
    applyResize(next, active.value, dx, dy)
  }

  const nextLayout = cloneLayout(layout.value)
  nextLayout.splice(index, 1, next)
  layout.value = resolveCollisions(nextLayout, next.id)
}

const endInteraction = async () => {
  window.removeEventListener('pointermove', onPointerMove)
  active.value = undefined
  await saveLayout()
}

const updateWidth = () => {
  containerWidth.value = containerRef.value?.clientWidth || 0
}

const applyResize = (item: WorkspaceItem, interaction: ActiveInteraction, dx: number, dy: number) => {
  const minW = item.minW || 3
  const minH = item.minH || 3
  if (interaction.direction === 'top') {
    const maxY = interaction.startItem.y + interaction.startItem.h - minH
    item.y = clamp(interaction.startItem.y + dy, 0, maxY)
    item.h = interaction.startItem.h + interaction.startItem.y - item.y
    return
  }
  if (interaction.direction === 'bottom') {
    item.h = Math.max(minH, interaction.startItem.h + dy)
    return
  }
  item.w = clamp(interaction.startItem.w + dx, minW, props.columns - item.x)
  item.h = Math.max(minH, interaction.startItem.h + dy)
}

const cloneLayout = (items: WorkspaceItem[]) => items.map((item) => ({ ...item }))
const clamp = (value: number, min: number, max: number) => Math.max(min, Math.min(value, max))
const overlaps = (a: WorkspaceItem, b: WorkspaceItem) =>
  a.x < b.x + b.w &&
  a.x + a.w > b.x &&
  a.y < b.y + b.h &&
  a.y + a.h > b.y

const resolveCollisions = (items: WorkspaceItem[], activeId: string) => {
  const next = cloneLayout(items)
  let changed = true
  let guard = 0
  while (changed && guard < 200) {
    changed = false
    guard += 1
    next.forEach((item) => {
      if (item.id === activeId) return
      const blockers = next.filter((other) => other.id !== item.id && overlaps(item, other))
      if (!blockers.length) return
      const bottom = Math.max(...blockers.map((other) => other.y + other.h))
      if (item.y < bottom) {
        item.y = bottom
        changed = true
      }
    })
  }
  return next
}

onMounted(() => {
  loadLayout()
  updateWidth()
  if (containerRef.value) {
    resizeObserver = new ResizeObserver(updateWidth)
    resizeObserver.observe(containerRef.value)
  }
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  window.removeEventListener('pointermove', onPointerMove)
})
</script>

<style scoped>
.workspace-shell {
  display: grid;
  gap: 8px;
}

.workspace-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 36px;
  padding: 0 4px;
  color: var(--text-primary);
  font-weight: 900;
}

.workspace-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.draggable-workspace {
  position: relative;
  min-height: 200px;
}

.workspace-item {
  position: absolute;
  display: grid;
  grid-template-rows: 34px minmax(0, 1fr);
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  background: rgba(16, 16, 16, 0.82);
  box-shadow: var(--shadow-card);
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}

.workspace-item.dragging,
.workspace-item.resizing {
  z-index: 20;
  border-color: rgba(255, 255, 255, 0.34);
  box-shadow: 0 22px 58px rgba(0, 0, 0, 0.48);
  user-select: none;
}

.workspace-item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
  padding: 0 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  color: var(--text-primary);
  cursor: grab;
  touch-action: none;
}

.workspace-item-head:active {
  cursor: grabbing;
}

.workspace-item-head span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 900;
}

.workspace-item-head small {
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.workspace-item-body {
  min-width: 0;
  min-height: 0;
  overflow: auto;
  padding: 10px;
}

.workspace-resize-handle {
  position: absolute;
  right: 8px;
  bottom: 8px;
  width: 18px;
  height: 18px;
  border: 0;
  border-right: 2px solid rgba(255, 255, 255, 0.5);
  border-bottom: 2px solid rgba(255, 255, 255, 0.5);
  background: transparent;
  cursor: nwse-resize;
  touch-action: none;
}

.workspace-resize-edge {
  position: absolute;
  left: 56px;
  right: 56px;
  height: 10px;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: ns-resize;
  touch-action: none;
}

.workspace-resize-edge::after {
  content: "";
  position: absolute;
  left: 50%;
  width: 46px;
  height: 3px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.22);
  opacity: 0;
  transform: translateX(-50%);
  transition: opacity 0.16s ease, background-color 0.16s ease;
}

.workspace-resize-top {
  top: 0;
}

.workspace-resize-top::after {
  top: 3px;
}

.workspace-resize-bottom {
  bottom: 0;
}

.workspace-resize-bottom::after {
  bottom: 3px;
}

.workspace-item:hover .workspace-resize-edge::after,
.workspace-item.resizing .workspace-resize-edge::after {
  opacity: 1;
}

.workspace-resize-edge:hover::after {
  background: rgba(255, 255, 255, 0.46);
}

@media (max-width: 980px) {
  .workspace-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .draggable-workspace {
    display: grid;
    gap: 10px;
    height: auto !important;
  }

  .workspace-item {
    position: relative;
    left: auto !important;
    top: auto !important;
    width: auto !important;
    height: auto !important;
    min-height: 240px;
  }

  .workspace-item-head {
    cursor: default;
  }

  .workspace-resize-handle,
  .workspace-resize-edge {
    display: none;
  }
}
</style>
