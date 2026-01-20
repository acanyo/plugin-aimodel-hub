package com.xhhao.aimodelhub.api.internal;

/**
 * ChatModels 工厂持有者（内部使用）
 * <p>
 * 此类仅供 AI Model Hub 插件内部使用，外部插件无需关心。
 * </p>
 *
 * @author Handsome
 * @since 1.0.0
 */
public final class ChatModelsHolder {

    private static ChatModelFactory factory;

    private ChatModelsHolder() {
    }

    /**
     * 初始化工厂
     *
     * @param chatModelFactory 工厂实例
     */
    public static void init(ChatModelFactory chatModelFactory) {
        factory = chatModelFactory;
    }

    /**
     * 获取工厂实例
     *
     * @return 工厂实例
     */
    public static ChatModelFactory getFactory() {
        return factory;
    }
}
