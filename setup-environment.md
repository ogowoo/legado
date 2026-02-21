# Android 编译环境配置指南

## 环境要求

- Java 17 (已安装 ✓)
- Android SDK 36
- Gradle 8.14.3 (已配置 ✓)

## 快速安装 Android SDK

### 方法一：自动安装（推荐）

以管理员身份运行 PowerShell，执行：

```powershell
# 设置执行策略（首次需要）
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser

# 运行安装脚本
.\install-android-sdk.ps1
```

### 方法二：手动安装

1. **下载 Android Studio** (包含 SDK)
   - 官网：https://developer.android.com/studio
   - 下载并安装，记住 SDK 安装路径（默认：`C:\Users\用户名\AppData\Local\Android\Sdk`）

2. **配置环境变量**
   ```powershell
   [Environment]::SetEnvironmentVariable("ANDROID_HOME", "C:\Users\你的用户名\AppData\Local\Android\Sdk", "User")
   ```

3. **安装必要组件**
   ```powershell
   sdkmanager "platforms;android-36" "platform-tools" "build-tools;36.0.0"
   ```

## 配置本地属性

创建 `local.properties` 文件：

```properties
sdk.dir=C\:\\Users\\你的用户名\\AppData\\Local\\Android\\Sdk
```

## 编译 APK

```bash
# 调试版本
.\gradlew.bat assembleDebug

# 发布版本（需要签名配置）
.\gradlew.bat assembleRelease
```

APK 输出位置：`app/build/outputs/apk/`

## 国内 AI 提供商注册

### 1. 阿里云 DashScope (推荐)
- 网址：https://dashscope.aliyun.com/
- 免费额度：100 万 Token
- 推荐模型：qwen-turbo

### 2. 智谱 AI (GLM)
- 网址：https://open.bigmodel.cn/
- 免费额度：1000 万 Token
- 推荐模型：glm-4-flash（免费）

### 3. DeepSeek
- 网址：https://platform.deepseek.com/
- 特点：价格极低，中文表现好
- 推荐模型：deepseek-chat

### 4. 百度千帆
- 网址：https://qianfan.baidu.com/
- 免费额度：有免费额度赠送
- 推荐模型：ernie-speed-128k

## 配置 AI 内容修正

1. 打开 App → 我的 → 设置 → 阅读设置
2. 启用 "AI content repair"
3. 选择 "AI Provider"（推荐阿里云或智谱 AI）
4. 填写对应的 API Key
5. 根据需要调整其他参数（可选）
