package com.xhhao.aimodelhub;

import com.xhhao.aimodelhub.api.internal.ChatModelFactory;
import com.xhhao.aimodelhub.api.internal.ChatModelsHolder;
import com.xhhao.aimodelhub.api.internal.ImageModelFactory;
import com.xhhao.aimodelhub.api.internal.ImageModelsHolder;
import com.xhhao.aimodelhub.extension.AiChatLog;
import org.springframework.stereotype.Component;
import run.halo.app.extension.SchemeManager;
import run.halo.app.extension.index.IndexSpecs;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

import java.util.Optional;

/**
 * <p>Plugin main class to manage the lifecycle of the plugin.</p>
 * <p>This class must be public and have a public constructor.</p>
 * <p>Only one main class extending {@link BasePlugin} is allowed per plugin.</p>
 *
 * @author Handsome
 * @since 1.0.0
 */
@Component
public class AimodelHubPlugin extends BasePlugin {

    private final SchemeManager schemeManager;
    private final ChatModelFactory chatModelFactory;
    private final ImageModelFactory imageModelFactory;

    public AimodelHubPlugin(PluginContext pluginContext, SchemeManager schemeManager,
                            ChatModelFactory chatModelFactory, ImageModelFactory imageModelFactory) {
        super(pluginContext);
        this.schemeManager = schemeManager;
        this.chatModelFactory = chatModelFactory;
        this.imageModelFactory = imageModelFactory;
    }

    @Override
    public void start() {
        // 注册自定义模型和索引
        schemeManager.register(AiChatLog.class, indexSpecs -> {
            // 按调用者插件索引
            indexSpecs.add(IndexSpecs.<AiChatLog, String>single("spec.callerPlugin", String.class)
                .indexFunc(log -> Optional.ofNullable(log.getSpec())
                    .map(AiChatLog.AiChatLogSpec::getCallerPlugin)
                    .orElse(null)));
            // 按供应商索引
            indexSpecs.add(IndexSpecs.<AiChatLog, String>single("spec.provider", String.class)
                .indexFunc(log -> Optional.ofNullable(log.getSpec())
                    .map(AiChatLog.AiChatLogSpec::getProvider)
                    .orElse(null)));
            // 按模型索引
            indexSpecs.add(IndexSpecs.<AiChatLog, String>single("spec.model", String.class)
                .indexFunc(log -> Optional.ofNullable(log.getSpec())
                    .map(AiChatLog.AiChatLogSpec::getModel)
                    .orElse(null)));
            // 按成功状态索引
            indexSpecs.add(IndexSpecs.<AiChatLog, String>single("status.success", String.class)
                .indexFunc(log -> Optional.ofNullable(log.getStatus())
                    .map(AiChatLog.AiChatLogStatus::getSuccess)
                    .map(String::valueOf)
                    .orElse(null)));
        });
        
        // 初始化静态入口
        ChatModelsHolder.init(chatModelFactory);
        ImageModelsHolder.init(imageModelFactory);
        
        System.out.println("AI Model Hub 插件启动成功！");
    }

    @Override
    public void stop() {
        // 注销自定义模型
        schemeManager.unregister(schemeManager.get(AiChatLog.class));
        System.out.println("AI Model Hub 插件停止！");
    }
}
