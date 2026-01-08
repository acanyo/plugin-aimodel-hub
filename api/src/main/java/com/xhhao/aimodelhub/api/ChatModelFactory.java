package com.xhhao.aimodelhub.api;

/**
 * 聊天模型工厂接口（对外 API）
 * <p>
 * 其他插件通过此接口获取 AI 模型客户端。
 * </p>
 *
 * <pre>{@code
 * // 单轮对话 / 手动管理多轮
 * ChatModel model = factory.openai();
 * model.chat("你好");
 *
 * // 自动管理多轮对话
 * ChatModel chat = factory.openaiWithMemory("你是助手");
 * chat.chat("你好");   // 第一轮
 * chat.chat("再见");   // 第二轮，自动带上下文
 * }</pre>
 *
 * @author Handsome
 */
public interface ChatModelFactory {

    /**
     * 获取 OpenAI 模型（使用插件配置）
     */
    ChatModel openai();

    /**
     * 获取 OpenAI 模型（指定模型名）
     */
    ChatModel openai(String modelName);

    /**
     * 获取 OpenAI 模型（自动管理对话历史）
     *
     * @param systemPrompt 系统提示词（角色设定）
     * @return 自动管理上下文的模型
     */
    ChatModel openaiWithMemory(String systemPrompt);
}
