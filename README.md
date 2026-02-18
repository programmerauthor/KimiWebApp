# Kimi WebView Android App

一个封装 Kimi AI 网页的 Android 应用，支持完整的浏览器功能。

## 功能特性

### 核心功能
- ✅ 完整 WebView 支持，加载 https://www.kimi.com/bot
- ✅ 自动登录状态保持（Cookie 持久化）
- ✅ 下拉刷新页面
- ✅ 返回键支持网页后退
- ✅ 进度条显示加载状态

### 文件上传支持
- ✅ 相册选择图片/视频
- ✅ 多文件选择
- ✅ 相机拍照上传
- ✅ 文件管理器选择

### 浏览器功能
- ✅ JavaScript 完整支持
- ✅ DOM Storage / LocalStorage
- ✅ 地理位置权限
- ✅ 媒体播放（音频/视频）
- ✅ 文件下载
- ✅ 缩放和自适应屏幕

### 权限支持
- 互联网访问
- 存储读写（下载/上传）
- 相机（拍照上传）
- 麦克风（语音输入）
- 地理位置

## 项目结构

```
KimiWebApp/
├── app/
│   ├── build.gradle              # 应用级 Gradle 配置
│   └── src/main/
│       ├── AndroidManifest.xml   # 应用清单
│       ├── java/com/example/kimiwebapp/
│       │   └── MainActivity.kt   # 主活动
│       └── res/
│           ├── layout/
│           │   └── activity_main.xml
│           ├── values/
│           │   ├── colors.xml
│           │   ├── strings.xml
│           │   └── themes.xml
│           ├── drawable/
│           │   └── progress_drawable.xml
│           └── xml/
│               └── network_security_config.xml
└── build.gradle                  # 项目级 Gradle 配置
```

## 构建说明

### 环境要求
- Android Studio Arctic Fox 或更高版本
- JDK 11 或更高版本
- Android SDK 34
- Gradle 8.0+

### 构建步骤

1. 打开 Android Studio
2. 选择 "Open an existing project"
3. 选择 `KimiWebApp` 文件夹
4. 等待 Gradle 同步完成
5. 点击 "Build" → "Build Bundle(s) / APK(s)" → "Build APK(s)"

### 命令行构建

```bash
# 进入项目目录
cd KimiWebApp

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease

# APK 输出路径
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release-unsigned.apk
```

## 安装使用

### 安装 APK
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 首次使用
1. 打开应用
2. 等待加载 Kimi 网页
3. 登录你的 Kimi 账号
4. 登录状态会自动保持

## 技术细节

### WebView 配置
- JavaScript 启用
- DOM Storage 启用
- 数据库支持
- 缓存模式：默认
- 混合内容支持
- 文件访问权限

### Cookie 管理
- 自动接受 Cookie
- 第三方 Cookie 支持
- 应用生命周期内持久化

### 文件上传处理
- 使用 `onShowFileChooser` 拦截文件选择
- 支持多文件选择
- 相册和文件管理器集成

## 注意事项

1. **网络权限**：应用需要互联网权限访问 Kimi 网站
2. **存储权限**：用于文件下载和上传功能
3. **证书**：Release 版本需要签名才能安装
4. **WebView 版本**：建议使用系统最新 WebView 版本

## 自定义修改

### 修改加载的网址
编辑 `MainActivity.kt`：
```kotlin
webView.loadUrl("https://www.kimi.com/bot")
```

### 修改应用名称
编辑 `res/values/strings.xml`：
```xml
<string name="app_name">你的应用名称</string>
```

### 修改主题颜色
编辑 `res/values/colors.xml`：
```xml
<color name="kimi_blue">#你的颜色代码</color>
```

## 许可证

MIT License
# Trigger build
