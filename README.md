# AI Model Hub

为 Halo 插件生态提供统一的 AI 模型调用能力。

## 🌐 演示与交流

- **演示站点1**：[https://www.xhhao.com/](https://www.xhhao.com/)
- **文档**：[https://docs.lik.cc/](https://docs.lik.cc/)
- **QQ 交流群**：[![QQ群](https://www.xhhao.com/upload/iShot_2025-03-03_16.03.00.png)](https://www.xhhao.com/upload/iShot_2025-03-03_16.03.00.png)


## 🎯 开发者方便在哪里

- **一行代码调用 AI** - 无需处理 HTTP 请求、认证、错误重试
- **统一 API** - 切换供应商（OpenAI/SiliconFlow/智谱）无需改代码
- **开箱即用** - 无需自己配置 API Key，使用用户在后台配置的密钥

```java
// 就这么简单
ChatModels.chat("你好").subscribe(System.out::println);

// 流式输出
ChatModels.chatStream("写一首诗").subscribe(System.out::print);

// 图像生成
ImageModels.generate("一只猫").subscribe(url -> System.out.println(url));
```

## 用户方便在哪里

- **统一配置** - 只需在 AI Model Hub 配置一次 API Key，所有依赖插件共享
- **费用可控** - 后台查看所有插件的 Token 使用量和调用统计
- **灵活切换** - 随时切换 AI 供应商，无需等待插件更新

## 支持的供应商

| 供应商 | 聊天 | 图像生成 |
| ------ | ---- | -------- |
| OpenAI | ✅ | ✅ |
| SiliconFlow | ✅ | ✅ |
| 智谱 AI | ✅ | ✅ |

## 快速开始

### 1. 添加依赖

```gradle
dependencies {
    implementation 'com.xhhao.aimodelhub:aimodel-hub-api:1.0.0'
}
```

```yaml
# plugin.yaml
spec:
  pluginDependencies:
    aimodel-hub: ">=1.0.0"
```

### 2. 使用

```java
import com.xhhao.aimodelhub.api.ChatModels;

// 对话
ChatModels.chat("翻译成英文：你好").subscribe(System.out::println);

// 多轮对话
List<ChatMessage> messages = List.of(
    ChatMessage.system("你是助手"),
    ChatMessage.user("你好")
);
ChatModels.chat(messages).subscribe(System.out::println);
```

## 许可证

[GPL-3.0](./LICENSE)
