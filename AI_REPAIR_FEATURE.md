# AI 内容修复功能

## 功能概述

使用 AI 自动纠正小说中的错乱文字，支持多种 AI 提供商。

## 支持的提供商

- OpenAI (GPT-3.5/4)
- Google Gemini
- Anthropic Claude
- DeepSeek (国产)
- 自定义 API (Moonshot、通义千问、文心一言等)

## 核心特性

- 智能缓存机制，减少 API 调用
- 自动重试机制，提高成功率
- 可配置参数（模型、温度、上下文等）
- 优雅的错误处理

## 文件结构

```
app/src/main/java/io/legado/app/ai/repair/
├── AIProvider.kt              # 提供商接口
├── OpenAIProvider.kt          # OpenAI 实现
├── GeminiProvider.kt          # Gemini 实现
├── CustomProvider.kt          # 自定义 API 实现
├── AIContentRepairService.kt  # 核心服务
└── RepairCacheManager.kt      # 缓存管理
```

## 配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| aiContentRepair | 功能开关 | false |
| aiRepairProviderType | AI 提供商 | OPENAI |
| aiRepairApiKey | API 密钥 | - |
| aiRepairApiUrl | 自定义 API 地址 | - |
| aiRepairModel | 模型名称 | - |
| aiContentRepairCache | 启用缓存 | true |
| aiRepairTemperature | 温度参数 | 0.2 |
| aiRepairMaxTokens | 最大输出长度 | 512 |
| aiRepairContextLength | 上下文长度 | 4000 |

## 使用方法

1. 在设置中开启 "AI 内容修复"
2. 选择 AI 提供商
3. 输入 API Key
4. （可选）配置其他参数
5. 开始阅读，错乱文字将自动修复

## 推荐配置

### 国内用户
- 提供商：DeepSeek 或自定义 API
- 模型：deepseek-chat 或 moonshot-v1-8k
- 温度：0.2
- 启用缓存：是

### 国际用户
- 提供商：OpenAI
- 模型：gpt-3.5-turbo
- 温度：0.2
- 启用缓存：是
