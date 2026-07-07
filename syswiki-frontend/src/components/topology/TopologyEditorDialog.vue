<template>
  <el-dialog
    :model-value="modelValue"
    :title="isEdit ? '编辑拓扑连接' : '新增拓扑连接'"
    width="600px"
    :close-on-click-modal="false"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="起始节点" prop="fromNode">
        <el-input v-model="form.fromNode" placeholder="例如：���心路由网关" />
      </el-form-item>
      <el-form-item label="目标节点" prop="toNode">
        <el-input v-model="form.toNode" placeholder="例如：跨境支付系统" />
      </el-form-item>
      <el-form-item label="通信协议" prop="protocol">
        <el-select v-model="form.protocol" placeholder="选择协议" style="width:100%">
          <el-option label="HTTP" value="HTTP" />
          <el-option label="HTTPS" value="HTTPS" />
          <el-option label="TCP" value="TCP" />
          <el-option label="RPC" value="RPC" />
          <el-option label="MQ" value="MQ" />
        </el-select>
      </el-form-item>
      <el-form-item label="接口名称">
        <el-input v-model="form.interfaceName" placeholder="例如：transQuery" />
      </el-form-item>
      <el-form-item label="接口详情">
        <el-input v-model="form.interfaceDetails" type="textarea" :rows="6"
                  placeholder="输入接口的请求/响应报文定义（支持 Markdown）" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSubmit">
        {{ isEdit ? '保存' : '添加' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { TopologyLink } from '@/types/topology'

const props = defineProps<{
  modelValue: boolean
  link?: TopologyLink | null  // 编辑模式下传入已有连接
}>()

const emit = defineEmits<{
  'update:modelValue': [val: boolean]
  save: [data: {
    fromNode: string
    toNode: string
    protocol: string
    interfaceName: string
    interfaceDetails: string
  }]
}>()

const formRef = ref<FormInstance>()
const saving = ref(false)

const isEdit = computed(() => !!props.link)

// 表单校验规则
const validateNodesDifferent = (_rule: any, _value: any, callback: any) => {
  if (form.fromNode && form.toNode && form.fromNode === form.toNode) {
    callback(new Error('起始节点与目标节点不能相同'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  fromNode: [
    { required: true, message: '请输入起始节点', trigger: 'blur' },
    { validator: validateNodesDifferent, trigger: 'blur' }
  ],
  toNode: [
    { required: true, message: '请输入目标节点', trigger: 'blur' },
    { validator: validateNodesDifferent, trigger: 'blur' }
  ],
  protocol: [
    { required: true, message: '请选择通信协议', trigger: 'change' }
  ]
}

const form = ref({
  fromNode: '',
  toNode: '',
  protocol: '',
  interfaceName: '',
  interfaceDetails: ''
})

// 弹窗打开时填充初始值
watch(() => props.modelValue, (val) => {
  if (val) {
    if (props.link) {
      form.value = {
        fromNode: props.link.fromNode,
        toNode: props.link.toNode,
        protocol: props.link.protocol || '',
        interfaceName: props.link.interfaceName || '',
        interfaceDetails: props.link.interfaceDetails || ''
      }
    } else {
      form.value = {
        fromNode: '',
        toNode: '',
        protocol: '',
        interfaceName: '',
        interfaceDetails: ''
      }
    }
    // 清除校验状态
    formRef.value?.clearValidate()
  }
})

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  formRef.value.validate(() => {})
  emit('save', { ...form.value })
}

// 暴露 saving 状态供父组件控制
defineExpose({ setSaving: (val: boolean) => { saving.value = val } })
</script>
