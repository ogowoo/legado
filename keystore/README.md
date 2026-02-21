# 读书应用签名配置指南

## 📋 简介

此目录包含读书应用的签名密钥生成工具和配置模板。

## 🔑 生成签名密钥

### 方法一：使用批处理脚本（Windows）

```batch
# 在 keystore 目录下运行
generate_keystore.bat
```

### 方法二：使用 PowerShell

```powershell
# 生成密钥库
keytool -genkey -v `
    -keystore dushu.keystore `
    -alias dushu `
    -keyalg RSA `
    -keysize 2048 `
    -validity 36500 `
    -storepass dushu123456 `
    -keypass dushu123456 `
    -dname "CN=读书, OU=读书应用, O=读书团队, L=中国, ST=北京, C=CN"
```

### 方法三：使用命令行

```bash
keytool -genkey -v \
    -keystore dushu.keystore \
    -alias dushu \
    -keyalg RSA \
    -keysize 2048 \
    -validity 36500 \
    -storepass dushu123456 \
    -keypass dushu123456 \
    -dname "CN=读书, OU=读书应用, O=读书团队, L=中国, ST=北京, C=CN"
```

## ⚙️ 配置签名

### 步骤 1：复制配置文件

```bash
cp signing.properties.template signing.properties
```

### 步骤 2：修改 signing.properties

根据实际情况修改：

```properties
STORE_FILE=keystore/dushu.keystore
STORE_PASSWORD=你的密钥库密码
KEY_ALIAS=dushu
KEY_PASSWORD=你的密钥密码
```

### 步骤 3：配置 build.gradle

在 `app/build.gradle` 中添加：

```gradle
android {
    signingConfigs {
        release {
            def signingPropsFile = rootProject.file('keystore/signing.properties')
            if (signingPropsFile.exists()) {
                def props = new Properties()
                props.load(new FileInputStream(signingPropsFile))
                
                storeFile file(props['STORE_FILE'])
                storePassword props['STORE_PASSWORD']
                keyAlias props['KEY_ALIAS']
                keyPassword props['KEY_PASSWORD']
            }
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

## 🔒 密钥信息

默认生成的密钥信息：

| 项目 | 值 |
|------|-----|
| 密钥库文件 | dushu.keystore |
| 别名 | dushu |
| 密钥库密码 | dushu123456 |
| 密钥密码 | dushu123456 |
| 算法 | RSA |
| 有效期 | 100年 |

**⚠️ 重要提醒：**
- 请妥善保管密钥库文件和密码
- 丢失密钥库将无法更新应用
- 建议备份到安全的地方
- 生产环境请使用强密码

## 📄 查看密钥信息

```bash
keytool -list -v -keystore dushu.keystore
```

## 🚀 构建签名APK

```bash
# 构建发布版本
./gradlew assembleRelease

# 输出位置
app/build/outputs/apk/release/
```

## 📞 支持

如有问题，请提交 Issue 到 GitHub 仓库。
