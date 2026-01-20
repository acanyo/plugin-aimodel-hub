<script lang="ts" setup>
import { VCard, VPageHeader, VButton, VSpace, VEmpty, VLoading, Toast, VTabbar } from '@halo-dev/components'
import { FormKit } from '@formkit/vue'
import { ref } from 'vue'
import { axiosInstance } from '@halo-dev/api-client'
import RiSendPlaneLine from '~icons/ri/send-plane-line'
import RiDeleteBinLine from '~icons/ri/delete-bin-line'
import RiTestTubeLine from '~icons/ri/test-tube-line'
import RiImageLine from '~icons/ri/image-line'
import RiMagicLine from '~icons/ri/magic-line'
import RiDownloadLine from '~icons/ri/download-line'
import RiChatSmileLine from '~icons/ri/chat-smile-line'

const activeTab = ref('chat')

const tabItems = [
  { id: 'chat', label: '文字模型' },
  { id: 'image', label: '图像模型' },
]

// 文字对话相关
const message = ref('你好，请介绍一下你自己')
const provider = ref('siliconflow')
const response = ref('')
const loading = ref(false)
const streamResponse = ref('')
const streamLoading = ref(false)

const providers = [
  { label: '硅基流动', value: 'siliconflow' },
  { label: 'OpenAI', value: 'openai' },
]

// 图像生成相关
const imagePrompt = ref('')
const imageLoading = ref(false)
const images = ref<string[]>([])

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

const generateImage = async () => {
  if (!imagePrompt.value.trim()) {
    Toast.warning('请输入图像描述')
    return
  }

  try {
    imageLoading.value = true
    images.value = []
    
    const { data } = await axiosInstance.post(
      '/apis/console.api.aimodel-hub.xhhao.com/v1alpha1/images/generate',
      {
        prompt: imagePrompt.value,
      }
    )
    
    if (data.success) {
      images.value = data.images || []
      Toast.success('图像生成成功')
    } else {
      Toast.error(data.message || '生成失败')
    }
  } catch (error: any) {
    Toast.error('请求失败: ' + error.message)
  } finally {
    imageLoading.value = false
  }
}

const downloadImage = async (url: string, index: number) => {
  try {
    const response = await fetch(url)
    const blob = await response.blob()
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `ai-image-${Date.now()}-${index + 1}.png`
    link.click()
    URL.revokeObjectURL(link.href)
    Toast.success('下载成功')
  } catch (error) {
    Toast.error('下载失败')
  }
}

const clearAll = () => {
  if (activeTab.value === 'chat') {
    message.value = ''
    provider.value = 'siliconflow'
    response.value = ''
    streamResponse.value = ''
  } else {
    imagePrompt.value = ''
    images.value = []
  }
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
    <!-- Tab 切换 -->
    <VTabbar v-model:activeId="activeTab" :items="tabItems" type="outline" class=":uno: mb-4" />

    <!-- 文字模型测试 -->
    <div v-if="activeTab === 'chat'" class=":uno: grid grid-cols-1 gap-4 lg:grid-cols-2">
      <VCard :body-class="['!p-6']">
        <template #header>
          <div class=":uno: block w-full bg-gray-50 px-4 py-3">
            <span class=":uno: text-base font-medium">测试配置</span>
          </div>
        </template>

        <div class=":uno: space-y-5">
          <FormKit
            v-model="message"
            type="textarea"
            label="消息内容"
            placeholder="输入你想问的问题..."
            rows="6"
            validation="required"
            validation-visibility="blur"
          />

          <FormKit
            v-model="provider"
            type="select"
            label="AI 供应商"
            :options="providers"
          />

          <div class=":uno: rounded-lg bg-blue-50 p-4">
            <p class=":uno: text-sm text-blue-800">
              测试结果会自动记录到日志中，可在"日志"页面查看详细记录。
            </p>
          </div>

          <div class=":uno: flex flex-col gap-3 sm:flex-row">
            <VButton class="flex-1" type="secondary" :loading="loading" @click="testSimpleChat">
              <template #icon><RiSendPlaneLine /></template>
              普通对话
            </VButton>
            <VButton class="flex-1" :loading="streamLoading" @click="testStreamChat">
              <template #icon><RiSendPlaneLine /></template>
              流式对话
            </VButton>
          </div>
        </div>
      </VCard>

      <VCard :body-class="['!p-6']">
        <template #header>
          <div class=":uno: block w-full bg-gray-50 px-4 py-3">
            <span class=":uno: text-base font-medium">响应结果</span>
          </div>
        </template>

        <div v-if="loading || streamLoading" class=":uno: flex min-h-[400px] items-center justify-center">
          <div class=":uno: text-center">
            <VLoading />
            <p class=":uno: mt-4 text-sm text-gray-500">
              {{ loading ? '正在调用 AI...' : '正在接收流式响应...' }}
            </p>
          </div>
        </div>

        <div v-else-if="response || streamResponse" class=":uno: space-y-4">
          <div class=":uno: flex items-center gap-2">
            <span class=":uno: rounded-full bg-blue-100 px-3 py-1 text-xs font-medium text-blue-800">
              {{ response ? '普通对话' : '流式对话' }}
            </span>
          </div>
          <div class=":uno: min-h-[300px] whitespace-pre-wrap break-words rounded-lg border border-gray-200 bg-white p-4 text-sm text-gray-900">
            {{ response || streamResponse }}
          </div>
        </div>

        <div v-else class=":uno: flex min-h-[400px] items-center justify-center">
          <VEmpty title="等待测试" message="在左侧输入消息并点击测试按钮开始" />
        </div>
      </VCard>
    </div>

    <!-- 图像模型测试 -->
    <div v-if="activeTab === 'image'" class=":uno: grid grid-cols-1 gap-4 lg:grid-cols-2">
      <VCard :body-class="['!p-6']">
        <template #header>
          <div class=":uno: block w-full bg-gray-50 px-4 py-3">
            <span class=":uno: text-base font-medium">生成配置</span>
          </div>
        </template>

        <div class=":uno: space-y-5">
          <FormKit
            v-model="imagePrompt"
            type="textarea"
            label="图像描述"
            placeholder="描述你想要生成的图像，越详细越好...&#10;例如：一只可爱的橘猫躺在阳光下的窗台上"
            rows="6"
            validation="required"
            validation-visibility="blur"
          />

          <div class=":uno: rounded-lg bg-amber-50 p-4">
            <p class=":uno: text-sm text-amber-800">
              图像生成需要消耗 OpenAI API 额度，DALL-E 3 每张约 $0.04-0.12。
            </p>
          </div>

          <VButton class="w-full" type="primary" :loading="imageLoading" @click="generateImage">
            <template #icon><RiMagicLine v-if="!imageLoading" /></template>
            {{ imageLoading ? '生成中...' : '生成图像' }}
          </VButton>
        </div>
      </VCard>

      <VCard :body-class="['!p-6']">
        <template #header>
          <div class=":uno: block w-full bg-gray-50 px-4 py-3">
            <span class=":uno: text-base font-medium">生成结果</span>
          </div>
        </template>

        <div v-if="imageLoading" class=":uno: flex min-h-[400px] flex-col items-center justify-center">
          <VLoading />
          <p class=":uno: mt-4 text-gray-500">正在生成图像，请稍候...</p>
          <p class=":uno: mt-2 text-sm text-gray-400">通常需要 10-30 秒</p>
        </div>

        <div v-else-if="images.length > 0" class=":uno: space-y-4">
          <div v-for="(url, index) in images" :key="index" class=":uno: relative group">
            <img :src="url" :alt="`生成的图像 ${index + 1}`" class=":uno: w-full rounded-lg shadow-md" />
            <div class=":uno: absolute bottom-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity">
              <VButton size="sm" type="secondary" @click="downloadImage(url, index)">
                <template #icon><RiDownloadLine /></template>
                下载
              </VButton>
            </div>
          </div>
        </div>

        <div v-else class=":uno: flex min-h-[400px] items-center justify-center">
          <VEmpty title="等待生成" message="在左侧输入描述并点击生成按钮" />
        </div>
      </VCard>
    </div>
  </div>
</template>
