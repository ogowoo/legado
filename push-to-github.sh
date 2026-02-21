#!/bin/bash

# Legado AI Repair - GitHub 推送脚本

echo "================================"
echo "Legado AI Repair 推送脚本"
echo "================================"
echo ""

# 检查是否在正确的目录
if [ ! -f "build.gradle" ]; then
    echo "错误：请在项目根目录运行此脚本"
    exit 1
fi

# 显示当前状态
echo "当前 Git 状态："
git status --short
echo ""

# 配置远程仓库（如果还没有）
if ! git remote get-url origin >/dev/null 2>&1; then
    echo "配置远程仓库..."
    git remote add origin https://github.com/ogowoo/legado.git
fi

echo "远程仓库："
git remote -v
echo ""

# 询问是否推送
read -p "是否推送到 GitHub? (y/n): " confirm

if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
    echo "已取消推送"
    exit 0
fi

# 获取 GitHub Token（可选）
read -p "输入 GitHub Personal Access Token (直接回车使用用户名密码): " token

if [ -n "$token" ]; then
    # 使用 token 推送
    git remote set-url origin "https://${token}@github.com/ogowoo/legado.git"
fi

# 推送主分支
echo ""
echo "推送 main 分支..."
git push -u origin master:main --force

if [ $? -eq 0 ]; then
    echo "✅ main 分支推送成功"
else
    echo "❌ main 分支推送失败"
    exit 1
fi

# 推送标签
echo ""
echo "推送标签 v1.0.0-ai..."
git push origin v1.0.0-ai --force

if [ $? -eq 0 ]; then
    echo "✅ 标签推送成功"
else
    echo "❌ 标签推送失败"
    exit 1
fi

echo ""
echo "================================"
echo "推送完成！"
echo "================================"
echo ""
echo "GitHub Actions 将自动开始构建"
echo "请访问: https://github.com/ogowoo/legado/actions"
echo ""
echo "构建完成后，Release 将发布在:"
echo "https://github.com/ogowoo/legado/releases"
echo ""
