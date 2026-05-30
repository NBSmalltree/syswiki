<template>
  <div class="md-editor-wrapper">
    <MdEditor v-model="content" :theme="'light'" :preview="true"
              :toolbars="toolbars" @on-change="handleChange" style="height:100%" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'

const props = defineProps<{ modelValue: string; height?: number }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const toolbars = [
  'bold', 'italic', 'strikethrough', 'title', 'quote',
  'unorderedList', 'orderedList', 'code', 'link',
  'table', 'preview', 'fullscreen'
]

const content = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const handleChange = (v: string) => { emit('update:modelValue', v) }
</script>

<style scoped>
.md-editor-wrapper { border: 1px solid #e4e7ed; border-radius: 4px; overflow: hidden; }
</style>
