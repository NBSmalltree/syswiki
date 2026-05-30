<template>
  <div class="markdown-viewer" v-html="renderedHtml"></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ content: string }>()

const renderedHtml = computed(() => {
  if (!props.content) return ''
  // 简易Markdown渲染（首期不引入重型库）
  let html = props.content
    .replace(/^### (.*$)/gim, '<h3>$1</h3>')
    .replace(/^## (.*$)/gim, '<h2>$1</h2>')
    .replace(/^# (.*$)/gim, '<h1>$1</h1>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/^\- (.*$)/gim, '<li>$1</li>')
    .replace(/\n\n/g, '</p><p>')
    .replace(/\|(.+)\|/g, (match) => {
      const cells = match.split('|').filter(c => c.trim())
      return '<tr>' + cells.map(c => `<td>${c.trim()}</td>`).join('') + '</tr>'
    })
  // 代码块
  html = html.replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code class="lang-$1">$2</code></pre>')
  return '<p>' + html + '</p>'
})
</script>

<style scoped>
.markdown-viewer { line-height: 1.8; font-size: 14px; }
.markdown-viewer :deep(h1) { font-size: 24px; margin: 16px 0 8px; }
.markdown-viewer :deep(h2) { font-size: 20px; margin: 14px 0 6px; border-bottom: 1px solid #eee; padding-bottom: 4px; }
.markdown-viewer :deep(h3) { font-size: 16px; margin: 12px 0 4px; }
.markdown-viewer :deep(code) { background: #f5f5f5; padding: 2px 6px; border-radius: 3px; font-size: 13px; }
.markdown-viewer :deep(pre) { background: #f5f5f5; padding: 12px; border-radius: 4px; overflow-x: auto; }
.markdown-viewer :deep(table) { border-collapse: collapse; width: 100%; margin: 8px 0; }
.markdown-viewer :deep(td), .markdown-viewer :deep(th) { border: 1px solid #ddd; padding: 6px 10px; text-align: left; }
.markdown-viewer :deep(li) { margin-left: 20px; margin-bottom: 4px; }
</style>
