<script setup lang="ts">
import { VCard, VPageHeader, VPagination, VSpace, VEmpty, VButton, VEntity, VEntityField, VStatusDot, Dialog, VLoading, Toast, VEntityContainer, VDropdownItem } from '@halo-dev/components'
import { computed, onMounted, ref, watch } from 'vue'
import { useRouteQuery } from '@vueuse/router'
import axios from 'axios'
import RiRefreshLine from '~icons/ri/refresh-line'
import RiRobot2Line from '~icons/ri/robot-2-line'
import { utils } from '@halo-dev/ui-shared'

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
    stream?: boolean
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
    const params: Record<string, any> = {
      page: page.value,
      size: size.value,
    }

    const { data } = await axios.get<ListResult<AiChatLog>>(
      '/apis/console.api.aimodel-hub.xhhao.com/v1alpha1/aichatlogs',
      { params }
    )
    logs.value = data
  } catch (error) {
    console.error('Failed to fetch logs:', error)
  } finally {
    loading.value = false
  }
}

const fetchStats = async () => {
  try {
    const { data } = await axios.get<Stats>(
      '/apis/console.api.aimodel-hub.xhhao.com/v1alpha1/aichatlogs/stats'
    )
    stats.value = data
  } catch (error) {
    console.error('Failed to fetch stats:', error)
  }
}

const handleRefresh = async () => {
  refreshing.value = true
  await Promise.all([fetchLogs(), fetchStats()])
  refreshing.value = false
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
        await axios.delete(
          `/apis/console.api.aimodel-hub.xhhao.com/v1alpha1/aichatlogs/${log.metadata.name}`
        )
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
      <RiRobot2Line class="mr-2 self-center" />
    </template>
    <template #actions>
      <VButton size="sm" @click="handleRefresh" :loading="refreshing">
        <template #icon>
          <RiRefreshLine />
        </template>
        刷新
      </VButton>
    </template>
  </VPageHeader>

  <div class="m-0 md:m-4">
    <!-- 统计卡片 -->
    <div v-if="stats" class="mb-4 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <VCard :body-class="['!p-4']">
        <div class="text-sm text-gray-500">总调用次数</div>
        <div class="mt-1 text-2xl font-bold">{{ stats.totalCalls }}</div>
        <div class="mt-2 text-xs text-gray-400">
          成功: {{ stats.successCount }} / 失败: {{ stats.failCount }}
        </div>
      </VCard>
      <VCard :body-class="['!p-4']">
        <div class="text-sm text-gray-500">总 Token 使用</div>
        <div class="mt-1 text-2xl font-bold">{{ stats.totalTokens.toLocaleString() }}</div>
        <div class="mt-2 text-xs text-gray-400">
          输入: {{ stats.totalPromptTokens.toLocaleString() }} / 输出: {{ stats.totalCompletionTokens.toLocaleString() }}
        </div>
      </VCard>
      <VCard :body-class="['!p-4']">
        <div class="text-sm text-gray-500">今日调用</div>
        <div class="mt-1 text-2xl font-bold">{{ stats.todayCalls }}</div>
      </VCard>
      <VCard :body-class="['!p-4']">
        <div class="text-sm text-gray-500">今日 Token</div>
        <div class="mt-1 text-2xl font-bold">{{ stats.todayTokens.toLocaleString() }}</div>
      </VCard>
    </div>

    <!-- 日志列表 -->
    <VCard :body-class="['!p-0']">
      <template #header>
        <div class="block w-full bg-gray-50 px-4 py-3">
          <div class="relative flex flex-col items-start sm:flex-row sm:items-center">
            <div class="flex w-full flex-1 sm:w-auto">
              <span class="text-base font-medium">日志列表</span>
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
            <VEntityField
              :title="log.spec.userMessage || '无消息'"
              :description="`${log.spec.provider} / ${log.spec.model}`"
              width="20rem"
            />
          </template>
          <template #end>
            <VEntityField v-if="log.spec.callerPlugin">
              <template #description>
                <span class="text-xs text-gray-500">{{ log.spec.callerPlugin }}</span>
              </template>
            </VEntityField>
            <VEntityField>
              <template #description>
                <span class="rounded bg-gray-100 px-2 py-1 text-xs text-gray-600">
                  {{ log.spec.stream ? '流式' : '普通' }}
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
                <span class="text-xs text-gray-500">
                  {{ log.status.totalTokens }} tokens
                </span>
              </template>
            </VEntityField>
            <VEntityField v-if="log.status">
              <template #description>
                <span class="text-xs text-gray-500">
                  {{ formatDuration(log.status.durationMs) }}
                </span>
              </template>
            </VEntityField>
            <VEntityField>
              <template #description>
                <span class="truncate text-xs tabular-nums text-gray-500">
                  {{ utils.date.format(log.spec.requestTime) }}
                </span>
              </template>
            </VEntityField>
          </template>
          <template #dropdownItems>
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
  </div>
</template>
