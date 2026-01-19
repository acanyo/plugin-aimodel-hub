<script lang="ts" setup>
import { VCard, VPageHeader, VButton, VSpace, VEmpty, VLoading, Toast } from '@halo-dev/components'
import { FormKit } from '@formkit/vue'
import { ref } from 'vue'
import { axiosInstance } from '@halo-dev/api-client'
import RiSendPlaneLine from '~icons/ri/send-plane-line'
import RiDeleteBinLine from '~icons/ri/delete-bin-line'
import RiTestTubeLine from '~icons/ri/test-tube-line'

const message = ref('你好，请介绍一下你自己')
const provider = ref('openai')
const model = ref('')
const response = ref('')
const loading = ref(false)
const streamResponse = ref('')
const streamLoading = ref(false)

const providers = [
  { label: 'OpenAI', value: 'openai' },
  // 未来可以添加其他供应商
  // { label: 'Claude', value: 'claude' },
  // { label: 'Gemini', value: 'gemini' },
]

const testSimpleChat = async () => {
  if (!message.value.trim()) {
    Toast.warning('请输入消息')
    return
  }

  try {
    loading.value = true
    response.value = ''
    
    const { data } = await axiosInstance.post(
      '/apis/console.api.aimodel-hub.xhhao.com/v1alpha1/testchat/simple',
      {
        message: message.value,
        provider: provider.value,
        model: model.value || undefined,
      }
    )
    
    if (data.success) {
      response.value = data.response
      Toast.success('调用成功，日志已记录')
    } else {
      response.value = `错误: ${data.message}`
      Toast.error(data.message)
    }
  } catch (error: any) {
    response.value = `请求失败: ${error.message}`
    Toast.error('请求失败')
  } finally {
    loading.value = false
  }
}

const testStreamChat = async () => {
  if (!message.value.trim()) {
    Toast.warning('请输入消息')
    return
  }

  try {
    streamLoading.value = true
    streamResponse.value = ''
    response.value = '' // 清空普通对话响应
    
    // 临时存储流式内容
    let tempContent = ''
    
    const fetchResponse = await fetch(
      '/apis/console.api.aimodel-hub.xhhao.com/v1alpha1/testchat/stream',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          message: message.value,
          provider: provider.value,
          model: model.value || undefined,
        }),
      }
    )

    if (!fetchResponse.ok) {
      throw new Error(`HTTP error! status: ${fetchResponse.status}`)
    }

    const reader = fetchResponse.body?.getReader()
    const decoder = new TextDecoder()

    if (!reader) {
      throw new Error('无法读取响应流')
    }

    // 读取所有内容但不显示
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      
      const chunk = decoder.decode(value, { stream: true })
      tempContent += chunk
    }
    
    // 流式读取完成后，一次性显示所有内容（去除 SSE 格式的 data: 前缀）
    // 如果内容包含 "data:" 前缀，说明是 SSE 格式，需要解析
    if (tempContent.includes('data:')) {
      // 按行分割，提取每行的实际内容
      const lines = tempContent.split('\n')
      const actualContent = lines
        .filter(line => line.startsWith('data:'))
        .map(line => line.substring(5).trim()) // 移除 "data:" 前缀
        .join('')
      streamResponse.value = actualContent
    } else {
      // 如果不是 SSE 格式，直接显示
      streamResponse.value = tempContent
    }
    
    Toast.success('流式调用完成，日志已记录')
  } catch (error: any) {
    streamResponse.value = `请求失败: ${error.message}`
    Toast.error('请求失败')
  } finally {
    streamLoading.value = false
  }
}

const clearAll = () => {
  message.value = ''
  provider.value = 'openai'
  model.value = ''
  response.value = ''
  streamResponse.value = ''
}
</script>

<template>
  <VPageHeader title="AI 调用测试">
    <template #icon>
      <RiTestTubeLine class=":uno: mr-2 self-center" />
    </template>
    <template #actions>
      <VButton size="sm" type="danger" @click="clearAll">
        <template #icon>
          <RiDeleteBinLine />
        </template>
        清空
      </VButton>
    </template>
  </VPageHeader>

  <div class=":uno: m-0 md:m-4">
    <div class=":uno: grid grid-cols-1 gap-4 lg:grid-cols-2">
      <!-- 左侧：输入区域 -->
      <VCard :body-class="['!p-6']">
        <template #header>
          <div class=":uno: block w-full bg-gray-50 px-4 py-3">
            <span class=":uno: text-base font-medium">测试配置</span>
          </div>
        </template>

        <div class=":uno: space-y-5">
          <!-- 消息内容 -->
          <FormKit
            v-model="message"
            type="textarea"
            label="消息内容"
            placeholder="输入你想问的问题..."
            rows="6"
            validation="required"
            validation-visibility="blur"
          />

          <!-- AI 供应商 -->
          <FormKit
            v-model="provider"
            type="select"
            label="AI 供应商"
            :options="providers"
          />

          <!-- 模型名称 -->
          <FormKit
            v-model="model"
            type="text"
            label="模型名称"
            placeholder="如: gpt-4o-mini"
          />

          <!-- 提示信息 -->
          <div class=":uno: rounded-lg bg-blue-50 p-4">
            <div class=":uno: flex">
              <div class=":uno: flex-shrink-0">
                <svg class=":uno: h-5 w-5 text-blue-500" viewBox="0 0 20 20" fill="currentColor">
                  <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd" />
                </svg>
              </div>
              <div class=":uno: ml-3">
                <p class=":uno: text-sm text-blue-800">
                  测试结果会自动记录到日志中，可在"日志"页面查看详细记录。
                </p>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class=":uno: flex flex-col gap-3 sm:flex-row">
            <VButton
              class="flex-1"
              type="secondary"
              :loading="loading"
              @click="testSimpleChat"
            >
              <template #icon>
                <RiSendPlaneLine />
              </template>
              普通对话
            </VButton>
            <VButton
              class="flex-1"
              :loading="streamLoading"
              @click="testStreamChat"
            >
              <template #icon>
                <RiSendPlaneLine />
              </template>
              流式对话
            </VButton>
          </div>
        </div>
      </VCard>

      <!-- 右侧：响应区域 -->
      <VCard :body-class="['!p-6']">
        <template #header>
          <div class=":uno: block w-full bg-gray-50 px-4 py-3">
            <span class=":uno: text-base font-medium">响应结果</span>
          </div>
        </template>

        <!-- 加载状态 -->
        <div v-if="loading || streamLoading" class=":uno: flex min-h-[400px] items-center justify-center">
          <div class=":uno: text-center">
            <VLoading />
            <p class=":uno: mt-4 text-sm text-gray-500">
              {{ loading ? '正在调用 AI...' : '正在接收流式响应...' }}
            </p>
          </div>
        </div>

        <!-- 响应内容 -->
        <div v-else-if="response || streamResponse" class=":uno: space-y-4">
          <!-- 响应类型标签 -->
          <div class=":uno: flex items-center gap-2">
            <span class=":uno: rounded-full bg-blue-100 px-3 py-1 text-xs font-medium text-blue-800">
              {{ response ? '普通对话' : '流式对话' }}
            </span>
            <span class=":uno: text-xs text-gray-500">
              响应完成
            </span>
          </div>
          
          <!-- 响应内容 -->
          <div class=":uno: min-h-[300px] whitespace-pre-wrap break-words rounded-lg border border-gray-200 bg-white p-4 text-sm text-gray-900">
            {{ response || streamResponse }}
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else class=":uno: flex min-h-[400px] items-center justify-center">
          <VEmpty
            title="等待测试"
            message="在左侧输入消息并点击测试按钮开始"
          />
        </div>
      </VCard>
    </div>
  </div>
</template>
