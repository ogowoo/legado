# Legado AI 内容修复功能

## 功能概述

使用 AI 自动纠正小说中的错乱文字，支持多种 AI 提供商。

## 支持的 AI 提供商

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

## 使用方法

1. 在设置中开启 "AI 内容修复"
2. 选择 AI 提供商
3. 输入 API Key
4. （可选）配置其他参数
5. 开始阅读，错乱文字将自动修复

## 文件结构

```
app/src/main/java/io/legado/app/ai/
├── AIProvider.kt              # AI 提供商接口
├── AIProviderFactory.kt       # 工厂模式
├── ContentRepairService.kt    # 核心服务
├── RepairCacheManager.kt      # 缓存管理
└── provider/
    ├── OpenAIProvider.kt
    ├── GeminiProvider.kt
    ├── ClaudeProvider.kt
    ├── DeepSeekProvider.kt
    └── CustomProvider.kt
```
