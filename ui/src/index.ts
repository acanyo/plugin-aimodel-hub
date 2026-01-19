import { definePlugin } from '@halo-dev/ui-shared'
import TestChat from './views/TestChat.vue'
import ChatLogList from './views/ChatLogList.vue'
import 'uno.css'
import RiRobot2Line from '~icons/ri/robot-2-line'
import RiTestTubeLine from '~icons/ri/test-tube-line'
import RiFileListLine from '~icons/ri/file-list-line'
import { markRaw } from 'vue'

export default definePlugin({
  components: {},
  routes: [
    {
      parentName: 'Root',
      route: {
        path: '/ai-model-hub',
        name: 'AiModelHub',
        redirect: '/ai-model-hub/logs',
        children: [
          {
            path: 'logs',
            name: 'AiChatLogs',
            component: ChatLogList,
            meta: {
              title: '调用日志',
              searchable: true,
              menu: {
                name: '调用日志',
                icon: markRaw(RiFileListLine),
                priority: 0,
              },
            },
          },
          {
            path: 'test',
            name: 'AiTestChat',
            component: TestChat,
            meta: {
              title: '调用测试',
              searchable: true,
              menu: {
                name: '调用测试',
                icon: markRaw(RiTestTubeLine),
                priority: 1,
              },
            },
          },
        ],
        meta: {
          title: 'AI 模型聚合',
          searchable: true,
          menu: {
            name: 'AI 模型聚合',
            group: 'system',
            icon: markRaw(RiRobot2Line),
            priority: 0,
          },
        },
      },
    },
  ],
  extensionPoints: {},
})
