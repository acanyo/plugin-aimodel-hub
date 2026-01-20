<script setup lang="ts">
import { VCard, VPageHeader, VPagination, VSpace, VEmpty, VButton, VEntity, VEntityField, VStatusDot, Dialog, VLoading, Toast, VEntityContainer, VDropdownItem, VModal } from '@halo-dev/components'
import { computed, onMounted, ref, watch } from 'vue'
import { useRouteQuery } from '@vueuse/router'
import RiRefreshLine from '~icons/ri/refresh-line'
import RiRobot2Line from '~icons/ri/robot-2-line'
import RiDeleteBin6Line from '~icons/ri/delete-bin-6-line'
import { utils } from '@halo-dev/ui-shared'
import { aiModelHubApiClient } from '@/api'

interface ListResult<T> {
  page: number
  size: number
  total: number
  items: T[]
  first: boolean
  last: boolean
  hasNext: boolean
  hasPrevious: boolean
  totalPages: number
}

type CallType = 'CHAT' | 'STREAM' | 'EMBEDDING' | 'IMAGE'

interface AiChatLog {
  metadata: {
    name: string
    creationTimestamp: string
  }
  spec: {
    callerPlugin?: string
    provider: string
    model: string
    userMessage?: string
    callType?: CallType
    requestTime: string
  }
  status?: {
    promptTokens?: number
    completionTokens?: number
    totalTokens?: number
    durationMs?: number
    success?: boolean
    errorMessage?: string
    responseSummary?: string
  }
}

interface Stats {
  totalCalls: number
  successCount: number
  failCount: number
  totalPromptTokens: number
  totalCompletionTokens: number
  totalTokens: number
  todayCalls: number
  todayTokens: number
}

const logs = ref<ListResult<AiChatLog>>()
const stats = ref<Stats>()
const loading = ref(false)
const refreshing = ref(false)

const page = useRouteQuery<number>('page', 1, { transform: Number })
const size = useRouteQuery<number>('size', 20, { transform: Number })
const total = computed(() => logs.value?.total || 0)

const fetchLogs = async () => {
  try {
    loading.value = true
    const { data } = await aiModelHubApiClient.chatLogConsole.listAiChatLogs({
      page: String(page.value),
      size: String(size.value),
    })
    logs.value = data as unknown as ListResult<AiChatLog>
  } catch (error) {
    console.error('Failed to fetch logs:', error)
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  try {
    const { data } = await aiModelHubApiClient.chatLogConsole.getAiChatLogStats()
    stats.value = data as unknown as Stats
  } catch (error) {
    console.error('Failed to fetch stats:', error)
  }
}

const handleRefresh = async () => {
  refreshing.value = true
  await Promise.all([fetchLogs(), fetchStats()])
  refreshing.value = false
}

const handleClearLogs = async () => {
  Dialog.warning({
    title: '确认清空',
    description: '确定要清空所有调用日志吗？此操作不可恢复。',
    confirmType: 'danger',
    confirmText: '清空',
    cancelText: '取消',
    onConfirm: async () => {
      try {
        await fetch('/apis/console.api.aimodel-hub.xhhao.com/v1alpha1/aichatlogs/clear', { method: 'DELETE' })
        Toast.success('日志已清空')
        await handleRefresh()
      } catch (error) {
        console.error('Failed to clear logs:', error)
        Toast.error('清空失败')
      }
    },
  })
}

const handleDelete = async (log: AiChatLog) => {
  Dialog.warning({
    title: '确认删除',
    description: '确定要删除这条日志吗？',
    confirmType: 'danger',
    confirmText: '删除',
    cancelText: '取消',
    onConfirm: async () => {
      try {
        await aiModelHubApiClient.chatLog.deleteAiChatLog({
          name: log.metadata.name,
        })
        Toast.success('删除成功')
        await handleRefresh()
      } catch (error) {
        console.error('Failed to delete log:', error)
        Toast.error('删除失败')
      }
    },
  })
}

const formatDuration = (ms?: number) => {
  if (!ms) return '-'
  if (ms < 1000) return `${ms}ms`
  return `${(ms / 1000).toFixed(2)}s`
}

const getCallTypeLabel = (log: AiChatLog) => {
  const callType = log.spec.callType
  if (callType === 'STREAM') return '流式'
  if (callType === 'EMBEDDING') return '嵌入'
  if (callType === 'IMAGE') return '图像'
  return '非流'
}

const getCallTypeClass = (log: AiChatLog) => {
  const callType = log.spec.callType
  if (callType === 'STREAM') return 'bg-teal-100 text-teal-600'
  if (callType === 'EMBEDDING') return 'bg-orange-100 text-orange-600'
  if (callType === 'IMAGE') return 'bg-pink-100 text-pink-600'
  return 'bg-purple-100 text-purple-600'
}

const getResponseContent = (log: AiChatLog) => {
  if (!log.status) return '无返回信息'
  if (log.status.success) {
    return log.status.responseSummary || '调用成功，无详细信息'
  } else {
    return log.status.errorMessage || '调用失败，无错误信息'
  }
}

const selectedLog = ref<AiChatLog | null>(null)
const showResponseModal = ref(false)

const handleViewResponse = (log: AiChatLog) => {
  selectedLog.value = log
  showResponseModal.value = true
}

const extractImageUrls = (text: string): string[] => {
  if (!text) return []
  const urlRegex = /(https?:\/\/[^\s,]+\.(png|jpg|jpeg|gif|webp|svg)[^\s,]*)/gi
  return text.match(urlRegex) || []
}

const isImageLog = (log: AiChatLog | null): boolean => {
  return log?.spec.callType === 'IMAGE'
}

watch([page, size], () => {
  fetchLogs()
})

onMounted(() => {
  fetchLogs()
  fetchStats()
})
</script>

<template>
  <VPageHeader title="AI 调用日志">
    <template #icon>
      <RiRobot2Line class=":uno: mr-2 self-center" />
    </template>
    <template #actions>
      <VSpace>
        <VButton size="sm" type="danger" @click="handleClearLogs">
          <template #icon>
            <RiDeleteBin6Line />
          </template>
          清空日志
        </VButton>
        <VButton size="sm" @click="handleRefresh" :loading="refreshing">
          <template #icon>
            <RiRefreshLine />
          </template>
          刷新
        </VButton>
      </VSpace>
    </template>
  </VPageHeader>

  <div class=":uno: m-0 md:m-4">
    <!-- 统计卡片 -->
    <div v-if="stats" class=":uno: mb-4 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <VCard :body-class="['!p-4']">
        <div class=":uno: text-sm text-gray-500">总调用次数</div>
        <div class=":uno: mt-1 text-2xl font-semibold text-gray-800">{{ stats.totalCalls }}</div>
        <div class=":uno: mt-2 flex items-center gap-3 text-xs">
          <VStatusDot state="success" :text="`${stats.successCount}`" :animate="false" />
          <VStatusDot state="error" :text="`${stats.failCount}`" :animate="false" />
        </div>
      </VCard>
      <VCard :body-class="['!p-4']">
        <div class=":uno: text-sm text-gray-500">总 Token 使用</div>
        <div class=":uno: mt-1 text-2xl font-semibold text-gray-800">{{ stats.totalTokens.toLocaleString() }}</div>
        <div class=":uno: mt-2 text-xs text-gray-400">
          输入 {{ stats.totalPromptTokens.toLocaleString() }} / 输出 {{ stats.totalCompletionTokens.toLocaleString() }}
        </div>
      </VCard>
      <VCard :body-class="['!p-4']">
        <div class=":uno: text-sm text-gray-500">今日调用</div>
        <div class=":uno: mt-1 text-2xl font-semibold text-gray-800">{{ stats.todayCalls }}</div>
        <div class=":uno: mt-2 text-xs text-gray-400">
          占总调用 {{ stats.totalCalls ? ((stats.todayCalls / stats.totalCalls) * 100).toFixed(1) : 0 }}%
        </div>
      </VCard>
      <VCard :body-class="['!p-4']">
        <div class=":uno: text-sm text-gray-500">今日 Token</div>
        <div class=":uno: mt-1 text-2xl font-semibold text-gray-800">{{ stats.todayTokens.toLocaleString() }}</div>
        <div class=":uno: mt-2 text-xs text-gray-400">
          占总量 {{ stats.totalTokens ? ((stats.todayTokens / stats.totalTokens) * 100).toFixed(1) : 0 }}%
        </div>
      </VCard>
    </div>

    <!-- 日志列表 -->
    <VCard :body-class="['!p-0']">
      <template #header>
        <div class=":uno: block w-full bg-gray-50 px-4 py-3">
          <div class=":uno: relative flex flex-col items-start sm:flex-row sm:items-center">
            <div class=":uno: flex w-full flex-1 sm:w-auto">
              <span class=":uno: text-base font-medium">日志列表</span>
            </div>
          </div>
        </div>
      </template>

      <VLoading v-if="loading" />
      
      <VEmpty v-else-if="!logs?.items?.length" message="暂无日志" title="当前没有调用记录">
        <template #actions>
          <VSpace>
            <VButton @click="handleRefresh">刷新</VButton>
          </VSpace>
        </template>
      </VEmpty>

      <VEntityContainer v-else>
        <VEntity v-for="log in logs?.items" :key="log.metadata.name">
          <template #start>
            <div 
              v-tooltip="'点击查看返回结果'" 
              @click="handleViewResponse(log)" 
              class=":uno: cursor-pointer"
            >
              <VEntityField
                :title="log.spec.userMessage || '无消息'"
                :description="`${log.spec.provider} / ${log.spec.model}`"
                width="20rem"
              />
            </div>
          </template>
          <template #end>
            <VEntityField>
              <template #description>
                <span :class="[':uno: rounded px-2 py-1 text-xs', getCallTypeClass(log)]">
                  {{ getCallTypeLabel(log) }}
                </span>
              </template>
            </VEntityField>
            <VEntityField>
              <template #description>
                <VStatusDot
                  :state="log.status?.success ? 'success' : 'error'"
                  :text="log.status?.success ? '成功' : '失败'"
                  :animate="false"
                />
              </template>
            </VEntityField>
            <VEntityField v-if="log.status">
              <template #description>
                <span class=":uno: text-xs text-gray-500">
                  {{ log.status.totalTokens }} tokens
                </span>
              </template>
            </VEntityField>
            <VEntityField v-if="log.status">
              <template #description>
                <span class=":uno: text-xs text-gray-500">
                  {{ formatDuration(log.status.durationMs) }}
                </span>
              </template>
            </VEntityField>
            <VEntityField>
              <template #description>
                <span class=":uno: truncate text-xs tabular-nums text-gray-500">
                  {{ utils.date.format(log.spec.requestTime) }}
                </span>
              </template>
            </VEntityField>
          </template>
          <template #dropdownItems>
            <VDropdownItem @click="handleViewResponse(log)">
              查看返回日志
            </VDropdownItem>
            <VDropdownItem type="danger" @click="handleDelete(log)">
              删除
            </VDropdownItem>
          </template>
        </VEntity>
      </VEntityContainer>

      <template #footer>
        <VPagination
          v-model:page="page"
          v-model:size="size"
          page-label="页"
          size-label="条 / 页"
          :total-label="`共 ${total} 项数据`"
          :total="total"
          :size-options="[20, 50, 100]"
        />
      </template>
    </VCard>

    <!-- 返回结果弹窗 -->
    <VModal
      v-model:visible="showResponseModal"
      :title="selectedLog?.status?.success ? '返回结果' : '错误信息'"
      :width="600"
    >
      <div v-if="selectedLog" class=":uno: space-y-4">
        <!-- 图像类型：展示图片 -->
        <template v-if="isImageLog(selectedLog) && selectedLog.status?.success">
          <div class=":uno: text-sm text-gray-600 mb-2">
            生成的图像：
          </div>
          <div class=":uno: grid grid-cols-1 gap-4">
            <div
              v-for="(url, index) in extractImageUrls(selectedLog.status?.responseSummary || '')"
              :key="index"
              class=":uno: overflow-hidden rounded-lg border border-gray-200"
            >
              <img
                :src="url"
                :alt="`生成图像 ${index + 1}`"
                class=":uno: w-full h-auto max-h-96 object-contain bg-gray-50"
                loading="lazy"
              />
              <div class=":uno: p-2 bg-gray-50 border-t border-gray-200">
                <a
                  :href="url"
                  target="_blank"
                  class=":uno: text-xs text-blue-600 hover:underline break-all"
                >
                  {{ url }}
                </a>
              </div>
            </div>
          </div>
          <div
            v-if="extractImageUrls(selectedLog.status?.responseSummary || '').length === 0"
            class=":uno: text-gray-500 text-sm"
          >
            {{ selectedLog.status?.responseSummary || '无图像信息' }}
          </div>
        </template>

        <!-- 文字类型或失败：展示文本 -->
        <template v-else>
          <div class=":uno: rounded-lg bg-gray-50 p-4">
            <pre class=":uno: whitespace-pre-wrap break-words text-sm text-gray-700 font-mono">{{ getResponseContent(selectedLog) }}</pre>
          </div>
        </template>

        <!-- 调用信息 -->
        <div class=":uno: border-t border-gray-200 pt-4 mt-4">
          <div class=":uno: grid grid-cols-2 gap-2 text-xs text-gray-500">
            <div>供应商：{{ selectedLog.spec.provider }}</div>
            <div>模型：{{ selectedLog.spec.model }}</div>
            <div>耗时：{{ formatDuration(selectedLog.status?.durationMs) }}</div>
            <div>Tokens：{{ selectedLog.status?.totalTokens || 0 }}</div>
          </div>
        </div>
      </div>
      <template #footer>
        <VButton @click="showResponseModal = false">关闭</VButton>
      </template>
    </VModal>
  </div>
</template>
