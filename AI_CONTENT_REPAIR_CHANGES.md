# AI 内容修正功能修改总结

## 修改概述

将原有的单一 OpenAI 支持扩展为支持多个国内 AI 提供商，让用户可以使用国内免费的 AI 服务进行文本校对。

## 支持的 AI 提供商

| 提供商 | 免费额度 | 推荐模型 | 特点 |
|--------|----------|----------|------|
| **阿里云 DashScope (通义千问)** | 100万 Token | qwen-turbo | ⭐ 推荐，稳定，中文好 |
| **智谱 AI (GLM)** | 1000万 Token | glm-4-flash | ⭐ 免费额度最多 |
| **DeepSeek** | 无免费额度 | deepseek-chat | 价格极低，中文好 |
| **百度千帆 (文心一言)** | 有免费额度 | ernie-speed-128k | 百度生态 |
| **OpenAI 官方** | 需付费 | gpt-3.5-turbo | 原始实现 |
| **自定义 OpenAI 兼容** | 取决于服务商 | 自定义 | 支持第三方代理 |

## 文件变更列表

### 新增文件

```
app/src/main/java/io/legado/app/ai/
├── AIProvider.kt                    # AI 提供商接口
├── AIProviderFactory.kt             # 提供商工厂
├── ContentRepairService.kt          # 重写，支持多提供商
└── providers/
    ├── OpenAIProvider.kt            # OpenAI 官方
    ├── DashScopeProvider.kt         # 阿里云通义千问
    ├── DeepSeekProvider.kt          # DeepSeek
    ├── ZhipuProvider.kt             # 智谱 AI
    ├── BaiduProvider.kt             # 百度千帆
    └── CustomOpenAIProvider.kt      # 自定义 OpenAI 兼容
```

### 修改文件

```
app/src/main/java/io/legado/app/help/config/AppConfig.kt
  - 添加 aiContentRepairEnabled 等 AI 相关配置属性

app/src/main/java/io/legado/app/constant/PreferKey.kt
  - 添加 aiRepairProvider, aiRepairApiUrl, aiRepairModel 等常量

app/src/main/res/xml/pref_config_read.xml
  - 重新设计 AI 内容修正设置界面，增加分类和更多选项

app/src/main/res/values/strings.xml
  - 添加英文界面字符串

app/src/main/res/values/arrays.xml
  - 添加 AI 提供商选项数组

app/src/main/res/values-zh/strings.xml
  - 添加中文界面字符串

app/src/main/res/values-zh/arrays.xml
  - 添加中文 AI 提供商选项数组
```

### 配置文件

```
local.properties                     # SDK 路径配置（需用户根据实际修改）
local.properties.template           # 配置模板
```

## 用户配置指南

### 1. 注册 AI 服务（以阿里云为例）

1. 访问 https://dashscope.aliyun.com/
2. 注册/登录阿里云账号
3. 进入控制台 → API Key 管理
4. 创建新的 API Key

### 2. 在 App 中配置

1. 打开阅读 App
2. 进入：我的 → 设置 → 阅读设置
3. 找到 "AI内容修正设置" 分类
4. 开启 "AI内容修正"
5. 选择 "AI提供商" → "阿里云 DashScope"
6. 填写 API密钥
7. 其他选项保持默认即可

### 3. 高级设置（可选）

- **自定义API地址**：用于第三方代理或自建服务
- **模型名称**：留空使用默认推荐模型，或填写特定模型
- **温度参数**：控制创造性，越低越稳定（默认 0.2）
- **最大Token数**：控制生成长度（默认 512）
- **系统提示词**：自定义 AI 的角色和任务

## 编译 APK

### 环境要求

- Java 17 ✓ (已安装)
- Android SDK 36
- Gradle 8.14.3 ✓ (已配置)

### 快速安装 SDK

```powershell
# 运行自动安装脚本
.\install-android-sdk.ps1

# 或手动安装 Android Studio
# https://developer.android.com/studio
```

### 编译命令

```bash
# 调试版本
.\gradlew.bat assembleDebug

# 发布版本
.\gradlew.bat assembleRelease
```

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`

## 技术实现细节

### 架构设计

```
AIProvider (接口)
    ├── name: 显示名称
    ├── providerId: 配置标识
    ├── defaultApiUrl: 默认 API 地址
    ├── supportedModels: 支持的模型列表
    ├── buildRequestBody(): 构建请求
    ├── buildHeaders(): 构建请求头
    └── parseResponse(): 解析响应

AIProviderFactory
    └── 管理所有提供商实例

ContentRepairService
    └── 根据配置动态选择提供商执行修正
```

### 请求/响应格式

所有国内提供商均兼容 OpenAI 的 Chat Completions API 格式：

**请求：**
```json
{
  "model": "qwen-turbo",
  "messages": [
    {"role": "system", "content": "系统提示词"},
    {"role": "user", "content": "前文上下文:\n...\n---\n当前段落:\n..."}
  ],
  "temperature": 0.2,
  "max_tokens": 512
}
```

**响应：**
```json
{
  "choices": [{
    "message": {
      "content": "修正后的文本"
    }
  }]
}
```

### 错误处理

- 网络错误：返回原文，不中断阅读
- API 错误：返回原文，记录日志
- 超时：返回原文，避免阻塞

## 注意事项

1. **API Key 安全**：API Key 存储在本地 SharedPreferences，不会上传到服务器
2. **流量消耗**：每个段落都会调用 API，注意流量和额度消耗
3. **隐私说明**：正文内容会发送到所选 AI 提供商的服务器
4. **离线不可用**：此功能需要网络连接

## 免费额度对比

| 提供商 | 免费额度 | 有效期 |
|--------|----------|--------|
| 阿里云 DashScope | 100万 Token | 新用户 180 天 |
| 智谱 AI | 1000万 Token | 新用户 90 天 |
| DeepSeek | 5000万 Token | 新用户 60 天 |
| 百度千帆 | 按模型不同 | 新用户 30-90 天 |

> 注：免费额度信息可能随时变化，请以官方最新说明为准。

## 常见问题

**Q: 为什么需要选择国内 AI 提供商？**  
A: 国内提供商访问速度快、价格低、部分有免费额度，且对中文支持更好。

**Q: API Key 会泄露吗？**  
A: API Key 只存储在本地，不会上传到任何服务器。

**Q: 支持离线使用吗？**  
A: 不支持，所有 AI 修正功能都需要联网调用 API。

**Q: 可以同时在多个设备使用吗？**  
A: 可以，但需要注意 API 调用的额度限制。

**Q: 如何查看 API 调用记录？**  
A: 需要登录对应 AI 提供商的控制台查看。
