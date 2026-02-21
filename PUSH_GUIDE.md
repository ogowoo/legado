# 推送到 GitHub 并发布 Release

## 快速推送

```bash
cd /root/.openclaw/workspace/legado
./push-to-github.sh
```

## 手动推送步骤

### 1. 配置远程仓库

```bash
cd /root/.openclaw/workspace/legado
git remote add origin https://github.com/ogowoo/legado.git
```

### 2. 使用 Token 推送（推荐）

在 GitHub 生成 Personal Access Token：
https://github.com/settings/tokens

然后：

```bash
# 设置远程 URL 使用 token
export GITHUB_TOKEN=your_token_here
git remote set-url origin "https://${GITHUB_TOKEN}@github.com/ogowoo/legado.git"

# 推送
git push -u origin master:main --force
git push origin v1.0.0-ai --force
```

### 3. 使用用户名密码推送

```bash
git push -u origin master:main --force
git push origin v1.0.0-ai --force
```

输入用户名和密码（或 token）。

## 验证推送

推送后访问：
- 代码：https://github.com/ogowoo/legado
- Actions：https://github.com/ogowoo/legado/actions
- Releases：https://github.com/ogowoo/legado/releases

## 构建状态

GitHub Actions 会自动：
1. ✅ 检出代码
2. ✅ 设置 JDK 17
3. ✅ 设置 Android SDK
4. ✅ 构建 APK
5. ✅ 上传构建产物
6. ✅ 创建 Release（如果是标签推送）

## 下载 APK

构建完成后，APK 将出现在：
https://github.com/ogowoo/legado/releases/tag/v1.0.0-ai

## 故障排除

### 推送被拒绝
```bash
# 强制推送（会覆盖远程分支）
git push -u origin master:main --force
```

### 需要身份验证
- 使用 Personal Access Token 代替密码
- 确保 token 有 `repo` 权限

### Actions 构建失败
1. 检查 build.gradle 配置
2. 检查依赖版本
3. 查看 Actions 日志
