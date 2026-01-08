import { rsbuildConfig } from '@halo-dev/ui-plugin-bundler-kit';
import Icons from "unplugin-icons/rspack";
import { pluginSass } from "@rsbuild/plugin-sass";
import type { RsbuildConfig } from "@rsbuild/core";

export default rsbuildConfig({
  manifestPath: '../app/src/main/resources/plugin.yaml',
  rsbuild: {
    resolve: {
      alias: {
        "@": "./src",
      },
    },
    plugins: [pluginSass()],
    tools: {
      rspack: {
        plugins: [Icons({ compiler: "vue3" })],
      },
    },
  },
}) as RsbuildConfig
