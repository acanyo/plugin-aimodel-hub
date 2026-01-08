import { definePlugin } from '@halo-dev/ui-shared'
import HomeView from './views/HomeView.vue'
import RiRobot2Line from '~icons/ri/robot-2-line'
import { markRaw } from 'vue'

export default definePlugin({
  components: {},
  routes: [
    {
      parentName: 'Root',
      route: {
        path: '/ai-model-hub',
        name: 'AiModelHub',
        component: HomeView,
        meta: {
          title: 'AI Model Hub',
          searchable: true,
          menu: {
            name: 'AI Model Hub',
            group: 'tool',
            icon: markRaw(RiRobot2Line),
            priority: 0,
          },
        },
      },
    },
  ],
  extensionPoints: {},
})
