package com.xhhao.aimodelhub.api.internal;

/**
 * ImageModels 工厂持有者（内部使用）
 * <p>
 * 此类仅供 AI Model Hub 插件内部使用，外部插件无需关心。
 * </p>
 *
 * @author Handsome
 * @since 1.0.0
 */
public final class ImageModelsHolder {

    private static ImageModelFactory factory;

    private ImageModelsHolder() {
    }

    /**
     * 初始化工厂
     *
     * @param imageModelFactory 工厂实例
     */
    public static void init(ImageModelFactory imageModelFactory) {
        factory = imageModelFactory;
    }

    /**
     * 获取工厂实例
     *
     * @return 工厂实例
     */
    public static ImageModelFactory getFactory() {
        return factory;
    }
}
