# EasyPhoto

EasyPhoto 是面向高龄老人的本地 Android 相机相册 App。日常只保留拍照、前置拍摄和看照片入口；飞书照片通知为管理员可选功能，默认关闭。

当前版本仅具备成为默认桌面的能力，不是企业 Kiosk，不阻止用户通过系统功能离开应用。

## 环境

- Android Studio：使用支持本项目 Android Gradle Plugin 的版本。
- JDK：17 或 21。
- Android SDK：安装 Platform 36 和 Build-Tools 35.0.0；路径由 Android Studio 在本机 `local.properties` 中配置。
- Gradle：项目 Gradle Wrapper
- 应用包名：`com.kevin.easyphoto`
- minSdk：26
- targetSdk：36

## 功能边界

- 使用 CameraX 后置相机预览与拍照。
- 照片保存到公共图片目录 `Pictures/EasyPhoto/`。
- App 内相册只查询并显示 `Pictures/EasyPhoto/` 内的照片。
- 支持单张照片全屏查看、左右切换，以及双指缩放和拖动。
- 不跳转系统相机或系统相册。
- 不包含广告、统计、推送、公开分享、视频、美颜或扫码。
- 管理员可选择开启飞书私密群照片通知：照片始终先保存到本机，再异步发送；该功能默认关闭，失败会自动重试，且不会阻塞拍照。

## 真机调试

1. 在手机中开启开发者选项。
2. 开启 USB 调试。
3. 连接 USB 数据线。
4. 在手机上允许 USB 调试。
5. 检查设备：

```powershell
adb devices -l
```

状态为 `device` 才视为可调试。以下 `adb` 命令要求 SDK 的 `platform-tools` 已在当前终端 PATH 中，也可使用你本机的 `adb` 完整路径。

## 构建

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
```

Debug APK 输出：

```text
app\build\outputs\apk\debug\app-debug.apk
```

## 安装和启动

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.kevin.easyphoto/.MainActivity
```

## 管理员模式

家属长按首页底部版本信息 4 秒打开管理员入口。首次使用时设置并确认 6 位数字 PIN，之后使用该 PIN 进入管理员模式；请避免使用生日等容易猜测的数字。

PIN 使用随机盐和摘要保存在设备的应用私有配置中，不提供通用默认口令。管理员模式用于减少日常误操作，不等同于系统锁屏或企业设备管理。忘记 PIN 时需要清除应用数据并重新配置；飞书通知等应用设置也会被重置。

## 飞书照片通知（管理员可选）

1. 在飞书中创建仅自己可见的私密群，并添加“自定义机器人”，取得 Webhook 地址。
2. 在飞书开放平台创建自建应用，申请图片上传能力，获取 App ID 与 App Secret。
3. 在 EasyPhoto 管理员模式的“飞书照片通知”中填写并保存配置，然后点击“测试连接”。
4. 测试群收到测试文本后，再开启“自动发送照片”。

配置使用 Android Keystore 加密保存于设备本机，不写入 APK、Git、日志或本项目文件。飞书会保存发送到群聊的照片副本；关闭通知会取消仍在等待网络的发送任务。

## Release

当前未配置 release 签名。后续需要由开发者本地生成并保管 keystore，不应把真实密钥提交到仓库。

仓库仅包含源码、测试、文档和 Gradle Wrapper；本机 SDK 配置、凭据文件、缓存、APK 及签名产物不提交。首页开发者邮箱为有意公开的联系方式。

## 荣耀手机后续验收

- 是否可被系统识别为默认桌面候选。
- Home 键是否回到 EasyPhoto 首页。
- CameraX 是否可打开后置相机。
- 闪光灯按钮是否可用。
- 照片是否保存到 `Pictures/EasyPhoto/`。
- 照片方向是否正确。
- 权限弹窗是否只在进入拍照时出现。
- 手势导航、状态栏和底部导航栏是否遮挡关键按钮。
