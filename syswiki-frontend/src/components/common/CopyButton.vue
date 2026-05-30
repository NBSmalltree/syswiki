<template>
  <el-tooltip :content="copied ? '已复制' : '点击复制'" placement="top">
    <el-button size="small" @click="handleCopy">
      <el-icon><CopyDocument /></el-icon>
      {{ label || '复制' }}
    </el-button>
  </el-tooltip>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps<{ text: string; label?: string }>()
const copied = ref(false)

const handleCopy = async () => {
  try {
    await navigator.clipboard.writeText(props.text)
    copied.value = true
    ElMessage.success('已复制到剪贴板')
    setTimeout(() => { copied.value = false }, 2000)
  } catch {
    ElMessage.error('复制失败')
  }
}
</script>
