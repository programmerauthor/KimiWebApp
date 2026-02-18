# GitHub Actions 自动构建配置指南

## 概述

此配置支持推送到 GitHub 时自动构建签名 APK，并发布 Release。

## 配置步骤

### 1. 创建 GitHub 仓库

1. 访问 https://github.com/new
2. 仓库名称：`KimiWebApp`（或其他名称）
3. 选择 "Public" 或 "Private"
4. 点击 "Create repository"

### 2. 上传代码到 GitHub

```bash
# 在项目目录中执行
cd KimiWebApp

git init
git add .
git commit -m "Initial commit"

git branch -M main
git remote add origin https://github.com/你的用户名/KimiWebApp.git
git push -u origin main
```

### 3. 生成签名密钥

在本地执行以下命令生成 keystore：

```bash
keytool -genkey -v \
  -keystore kimikeystore.jks \
  -alias kimirelease \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass 你的密钥库密码 \
  -keypass 你的密钥密码 \
  -dname "CN=KimiWebApp, OU=Dev, O=YourOrg, L=City, ST=State, C=CN"
```

### 4. 将密钥转换为 Base64

```bash
# macOS/Linux
base64 -i kimikeystore.jks -o keystore_base64.txt

# Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("kimikeystore.jks")) | Out-File -Encoding ASCII keystore_base64.txt
```

### 5. 配置 GitHub Secrets

在 GitHub 仓库页面：
1. 点击 "Settings" → "Secrets and variables" → "Actions"
2. 点击 "New repository secret"
3. 添加以下 Secrets：

| Secret 名称 | 值 | 说明 |
|------------|-----|------|
| `KEYSTORE_BASE64` | keystore_base64.txt 的内容 | 密钥库的 Base64 编码 |
| `KEYSTORE_PASSWORD` | 你的密钥库密码 | 密钥库密码 |
| `KEY_ALIAS` | kimirelease | 密钥别名 |
| `KEY_PASSWORD` | 你的密钥密码 | 密钥密码 |

### 6. 修改 build.gradle 支持签名

编辑 `app/build.gradle`，在 `android` 块中添加：

```gradle
android {
    // ... 现有配置 ...
    
    signingConfigs {
        release {
            storeFile file("keystore.jks")
            storePassword System.getenv("KEYSTORE_PASSWORD")
            keyAlias System.getenv("KEY_ALIAS")
            keyPassword System.getenv("KEY_PASSWORD")
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

### 7. 触发构建

推送代码到 main 分支：

```bash
git add .
git commit -m "Add GitHub Actions workflow"
git push origin main
```

GitHub Actions 将自动开始构建。

### 8. 下载 APK

构建完成后：
1. 进入 GitHub 仓库页面
2. 点击 "Actions" 标签
3. 点击最新的工作流运行
4. 在 "Artifacts" 部分下载 APK

或者查看 Releases 页面下载自动发布的 Release APK。

## 工作流说明

### 触发条件

- 推送到 `main` 或 `master` 分支
- 创建 Pull Request
- 手动触发（workflow_dispatch）

### 构建流程

1. **检出代码** — 获取最新代码
2. **设置 JDK 17** — 配置 Java 环境
3. **设置 Android SDK** — 安装 Android 构建工具
4. **缓存 Gradle** — 加速后续构建
5. **解码密钥库** — 从 Secrets 恢复签名密钥
6. **构建 Release APK** — 编译并签名
7. **上传构建产物** — 保存 APK 作为 Artifact
8. **创建 Release** — 自动发布到 Releases 页面

## 注意事项

1. **密钥安全**：永远不要将 `keystore.jks` 提交到 Git 仓库
2. **密码安全**：所有密码都通过 GitHub Secrets 管理
3. **构建时间**：首次构建可能需要 5-10 分钟（下载依赖）
4. **免费额度**：GitHub Actions 对公开仓库免费，私有仓库有 2000 分钟/月的免费额度

## 故障排除

### 构建失败

1. 检查 Secrets 是否正确配置
2. 查看 Actions 日志获取详细错误信息
3. 确保 `gradlew` 有执行权限

### 签名失败

1. 确认 `KEYSTORE_BASE64` 正确生成
2. 检查密码和别名是否匹配
3. 验证 keystore 文件未损坏

## 自定义配置

### 修改构建触发条件

编辑 `.github/workflows/build.yml`：

```yaml
on:
  push:
    branches: [ main ]  # 只在 main 分支推送时触发
  workflow_dispatch:     # 允许手动触发
```

### 添加测试步骤

```yaml
- name: Run tests
  run: ./gradlew test
```

### 多渠道打包

```yaml
- name: Build all variants
  run: ./gradlew assemble
```

---

*配置完成时间：2026-02-18*  
*作者：幸运智能助手*
