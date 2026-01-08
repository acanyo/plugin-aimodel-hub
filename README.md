# AI Model Hub

AI Model Hub - 为 Halo 插件生态提供统一的 AI 模型调用能力

## 简介

这是一个 Halo 插件，为其他 Halo 插件提供统一的 AI 模型调用接口。其他插件通过 Maven 依赖 API 模块，即可轻松集成 AI 能力。

## 特性

- 🔌 **插件依赖调用**: API 模块独立发布，其他插件通过依赖注入使用
- 🚀 **多 Provider 支持**: 支持 OpenAI、Claude、Gemini 等（持续扩展中）
- 🌊 **响应式编程**: 基于 Project Reactor，支持流式和非流式
- 💬 **多轮对话**: 内置多轮对话支持，上下文自动管理
- 🎯 **简单易用**: LangChain4j 风格 API，开箱即用

## 开发环境

- Java 21+
- Node.js 18+
- pnpm

## 开发

```bash
# 启动插件开发服务器
./gradlew haloServer

# 开发前端
cd ui
pnpm install
pnpm dev
```

## 构建

```bash
./gradlew build
```

构建完成后，可以在 `app/build/libs` 目录找到插件 jar 文件。

## 快速开始

### 1. 添加依赖

在你的 Halo 插件项目中添加依赖：

```gradle
dependencies {
    implementation 'com.xhhao.aimodelhub:aimodel-hub-api:1.0.0-SNAPSHOT'
}
```

在 `plugin.yaml` 中声明插件依赖：

```yaml
spec:
  pluginDependencies:
    aimodel-hub: ">=1.0.0"
```

### 2. 使用示例

```java
@Component
@RequiredArgsConstructor
public class ArticleService {
    
    private final ChatModelFactory chatModelFactory;
    
    /**
     * 单轮对话
     */
    public Mono<String> generateTitle(String content) {
        ChatModel model = chatModelFactory.openai();
        return model.chat(
            "你是一个专业的内容编辑",
            "为以下内容生成标题：" + content
        );
    }
    
    /**
     * 多轮对话
     */
    public Mono<String> multiTurnChat() {
        ChatModel model = chatModelFactory.openai();
        
        List<ChatMessage> messages = List.of(
            ChatMessage.system("你是一个 Java 助手"),
            ChatMessage.user("java 是啥"),
            ChatMessage.assistant("Java 是一个开发语言"),
            ChatMessage.user("java 如何用")
        );
        
        return model.chat(messages);
    }
    
    /**
     * 流式输出
     */
    public Flux<String> writeArticle(String topic) {
        ChatModel model = chatModelFactory.openai("gpt-4o");
        return model.chatStream("写一篇关于 " + topic + " 的文章");
    }
}
```

## 文档

- [开发文档](./DEVELOPMENT.md) - 完整的 API 文档和架构说明
- [多轮对话指南](./MULTI_TURN_CHAT.md) - 多轮对话使用教程

## 支持的 AI 供应商

- ✅ OpenAI（支持自定义 base URL）
- 🚧 Claude（计划中）
- 🚧 Gemini（计划中）
- 🚧 智谱 AI（计划中）

## 许可证

[GPL-3.0](./LICENSE) © Handsome
