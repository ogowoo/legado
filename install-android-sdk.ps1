# Android SDK 自动安装脚本 for Windows
# 需要管理员权限

param(
    [string]$SdkInstallPath = "$env:LOCALAPPDATA\Android\Sdk"
)

$ErrorActionPreference = "Stop"

function Write-Header($text) {
    Write-Host ""
    Write-Host "=== $text ===" -ForegroundColor Green
}

function Write-Info($text) {
    Write-Host $text -ForegroundColor Cyan
}

function Write-Warn($text) {
    Write-Host $text -ForegroundColor Yellow
}

Write-Header "Android SDK 自动安装脚本"
Write-Info "安装路径: $SdkInstallPath"

# 检查管理员权限
$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Warn "建议以管理员身份运行此脚本，以避免权限问题"
}

# 创建目录
try {
    New-Item -ItemType Directory -Force -Path $SdkInstallPath | Out-Null
    Write-Info "创建目录: $SdkInstallPath"
} catch {
    Write-Error "创建目录失败: $_"
    exit 1
}

$CmdlineToolsPath = "$SdkInstallPath\cmdline-tools"
$TempDir = "$env:TEMP\android-sdk-setup"

# 清理临时目录
if (Test-Path $TempDir) {
    Remove-Item -Path $TempDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $TempDir | Out-Null

# 下载命令行工具
$SdkUrl = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"
$ZipFile = "$TempDir\cmdline-tools.zip"

Write-Header "下载 Android SDK 命令行工具"
Write-Info "URL: $SdkUrl"

try {
    $ProgressPreference = 'SilentlyContinue'
    Invoke-WebRequest -Uri $SdkUrl -OutFile $ZipFile -UseBasicParsing
    Write-Info "下载完成: $ZipFile"
} catch {
    Write-Error "下载失败: $_"
    exit 1
}

# 解压
Write-Header "解压命令行工具"
try {
    Expand-Archive -Path $ZipFile -DestinationPath $TempDir -Force
    Write-Info "解压完成"
} catch {
    Write-Error "解压失败: $_"
    exit 1
}

# 移动到正确位置
$SourcePath = "$TempDir\cmdline-tools"
$DestPath = "$CmdlineToolsPath\latest"

if (Test-Path $DestPath) {
    Write-Warn "清除旧版本..."
    Remove-Item -Path $DestPath -Recurse -Force
}

New-Item -ItemType Directory -Force -Path $CmdlineToolsPath | Out-Null
Move-Item -Path $SourcePath -Destination $DestPath -Force
Write-Info "命令行工具安装完成: $DestPath"

# 设置环境变量
Write-Header "配置环境变量"

try {
    [Environment]::SetEnvironmentVariable("ANDROID_HOME", $SdkInstallPath, "User")
    [Environment]::SetEnvironmentVariable("ANDROID_SDK_ROOT", $SdkInstallPath, "User")
    Write-Info "已设置 ANDROID_HOME: $SdkInstallPath"

    # 添加到 PATH
    $UserPath = [Environment]::GetEnvironmentVariable("Path", "User")
    $NewPaths = @(
        "$CmdlineToolsPath\latest\bin"
        "$SdkInstallPath\platform-tools"
    )

    foreach ($Path in $NewPaths) {
        if ($UserPath -notlike "*$Path*") {
            $UserPath = "$UserPath;$Path"
            Write-Info "添加到 PATH: $Path"
        }
    }
    [Environment]::SetEnvironmentVariable("Path", $UserPath, "User")
} catch {
    Write-Error "配置环境变量失败: $_"
    exit 1
}

# 设置当前会话环境变量
$env:ANDROID_HOME = $SdkInstallPath
$env:ANDROID_SDK_ROOT = $SdkInstallPath
$env:Path = "$env:Path;$CmdlineToolsPath\latest\bin;$SdkInstallPath\platform-tools"

# 接受许可证
Write-Header "接受 SDK 许可证"
$SdkManager = "$CmdlineToolsPath\latest\bin\sdkmanager.bat"

try {
    # 使用 echo 自动接受许可证
    $proc = Start-Process -FilePath "cmd.exe" -ArgumentList "/c echo y| `"$SdkManager`" --licenses" -Wait -PassThru
    Write-Info "许可证处理完成"
} catch {
    Write-Warn "许可证处理可能失败，但继续安装: $_"
}

# 安装必要组件
Write-Header "安装必要的 SDK 组件"

$Packages = @(
    "platforms;android-36"
    "platform-tools"
    "build-tools;36.0.0"
)

foreach ($Package in $Packages) {
    Write-Info "正在安装: $Package"
    try {
        & $SdkManager --install $Package
        Write-Info "$Package 安装完成"
    } catch {
        Write-Warn "$Package 安装失败: $_"
    }
}

# 创建 local.properties
$ProjectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$LocalPropertiesPath = "$ProjectDir\local.properties"

$EscapedSdkPath = $SdkInstallPath -replace '\\', '\\'
$LocalPropertiesContent = "sdk.dir=$EscapedSdkPath"

Set-Content -Path $LocalPropertiesPath -Value $LocalPropertiesContent
Write-Info "创建 local.properties: $LocalPropertiesPath"

# 清理
Remove-Item -Path $TempDir -Recurse -Force -ErrorAction SilentlyContinue

Write-Header "安装完成"
Write-Host ""
Write-Host "Android SDK 安装成功！" -ForegroundColor Green
Write-Host "SDK 路径: $SdkInstallPath" -ForegroundColor Cyan
Write-Host "ANDROID_HOME: $SdkInstallPath" -ForegroundColor Cyan
Write-Host ""
Write-Host "请重新打开 PowerShell/IDE 以使用新环境变量" -ForegroundColor Yellow
Write-Host ""
Write-Host "验证安装:" -ForegroundColor Green
Write-Host "  sdkmanager --list" -ForegroundColor Gray
Write-Host ""
Write-Host "编译 APK:" -ForegroundColor Green
Write-Host "  .\gradlew.bat assembleDebug" -ForegroundColor Gray
Write-Host ""
