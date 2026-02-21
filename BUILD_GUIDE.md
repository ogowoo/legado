# Legado AI 内容修复 - 构建和发布指南

## 项目说明

本项目为 Legado 阅读应用添加了 AI 内容修复功能，可以自动纠正小说中的错乱文字。

## 功能特性

✅ **多 AI 提供商支持**
- OpenAI (GPT-3.5/4)
- Google Gemini
- Anthropic Claude
- DeepSeek (国产)
- 自定义 API (兼容 OpenAI 格式)

✅ **智能功能**
- 功能开关控制
- LRU 缓存机制
- 自动重试（最多3次）
- 优雅错误处理

## 文件结构

```
legado/
├── app/src/main/java/io/legado/app/ai/
│   ├── AIProvider.kt              # AI 提供商接口
│   ├── AIProviderFactory.kt       # 工厂模式
│   ├── ContentRepairService.kt    # 核心服务
│   ├── RepairCacheManager.kt      # 缓存管理
│   └── provider/
│       ├── OpenAIProvider.kt      # OpenAI 实现
│       ├── GeminiProvider.kt      # Gemini 实现
│       ├── ClaudeProvider.kt      # Claude 实现
│       ├── DeepSeekProvider.kt    # DeepSeek 实现
│       └── CustomProvider.kt      # 自定义 API 实现
├── .github/workflows/
│   └── build.yml                  # GitHub Actions 工作流
└── README_AI.md                   # 功能说明文档
```

## 快速开始

### 1. 推送到 GitHub

```bash
cd /root/.openclaw/workspace/legado
./push-to-github.sh
```

或者手动推送：

```bash
cd /root/.openclaw/workspace/legado
git remote add origin https://github.com/ogowoo/legado.git
git push -u origin master:main --force
git push origin v1.0.0-ai --force
```

### 2. 等待自动构建

推送到 GitHub 后，GitHub Actions 会自动：
1. 编译 APK
2. 运行测试
3. 创建 Release
4. 上传 APK 文件

### 3. 下载 APK

构建完成后，访问：
https://github.com/ogowoo/legado/releases

## 本地构建（可选）

如果需要本地构建：

### 环境要求
- JDK 17+
- Android SDK
- Gradle

### 构建步骤

```bash
# 1. 设置环境变量
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=/opt/android-sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH

# 2. 接受许可证
yes | sdkmanager --licenses

# 3. 构建 APK
./gradlew assembleAppRelease

# 4. 输出位置
app/build/outputs/apk/app/release/
```

## 使用方法

1. 安装 APK
2. 打开设置 → AI 内容修复
3. 开启功能开关
4. 选择 AI 提供商
5. 输入 API Key
6. 开始阅读

## 配置项

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| aiContentRepair | 功能开关 | false |
| aiRepairProviderType | AI 提供商 | OPENAI |
| aiRepairApiKey | API 密钥 | - |
| aiRepairModel | 模型名称 | - |

## 技术实现

### 设计模式
- **工厂模式**: AIProviderFactory 创建不同的 AI 提供商
- **策略模式**: 不同的 AIProvider 实现相同的接口
- **单例模式**: ContentRepairService 和 RepairCacheManager

### 核心流程

```
阅读页面加载
    ↓
检查 AI 修复开关
    ↓
查询缓存 → 命中 → 返回缓存
    ↓
调用 AI API
    ↓
保存缓存 → 返回结果
```

## 测试

### 单元测试
```bash
./gradlew test
```

### 集成测试
```bash
./gradlew connectedAndroidTest
```

## 故障排除

### 构建失败
1. 检查 JDK 版本 (需要 17+)
2. 检查 Android SDK 是否安装
3. 检查网络连接

### API 调用失败
1. 检查 API Key 是否正确
2. 检查网络连接
3. 查看日志输出

## 贡献

欢迎提交 Issue 和 Pull Request！

## 许可证

遵循原 Legado 项目的开源许可证。
