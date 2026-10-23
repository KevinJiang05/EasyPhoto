# 构建、安装与发布

## 开发环境

- JDK 17 或 21，编译目标 JVM 17。
- Android SDK Platform 36、Build-Tools 35.0.0 和 platform-tools。
- Android Studio 打开项目后选择本机 SDK；`local.properties` 不提交。
- Gradle 版本由仓库内 Wrapper 固定。

在项目根目录执行：

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

Linux/macOS 使用 `./gradlew`。CI 在 Linux 上运行同一组任务，下载入口在仓库 Actions 页面。Debug APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 安装

普通体验者优先从 [GitHub Releases](https://github.com/KevinJiang05/EasyPhoto/releases/latest) 下载签名 release APK。Android 8.0 及以上可安装；相机表现与系统桌面行为仍取决于机型。

开发调试时，把 SDK 的 platform-tools 加入当前终端路径，或使用自己的 adb 完整路径：

```powershell
adb devices -l
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.kevin.easyphoto/.MainActivity
```

同一包名的覆盖安装需要兼容签名。Debug、自行签名与官方 release 可能无法直接相互覆盖；不要为安装演示包而直接卸载正在使用的版本。

## Release 签名

签名只从当前进程的环境变量读取，不在源码中保存本机路径或凭据：

| 环境变量 | 含义 |
| --- | --- |
| `EASYPHOTO_KEYSTORE` | 本机 keystore 文件路径 |
| `EASYPHOTO_STORE_PASSWORD` | keystore 密码 |
| `EASYPHOTO_KEY_ALIAS` | 签名别名 |
| `EASYPHOTO_KEY_PASSWORD` | 私钥密码 |

使用本机私有配置或凭据管理工具设置上述四项，再执行：

```powershell
.\gradlew.bat assembleRelease
```

四项均未配置时会生成 unsigned release，不能作为正式可安装版本发布；只配置部分字段时构建会报错。CI 不使用发布私钥，只构建 debug 包。

发布前检查：

1. 运行测试、构建与 lint，核对验收记录。
2. 使用 SDK 的 `apksigner verify --print-certs` 验证 APK，并与旧 release 的证书摘要比较，保持覆盖升级兼容。
3. 计算 SHA-256，连同签名 APK、版本说明及演示视频放入 GitHub Release。
4. 标签对应发布源码提交；未经过真机验证的变更在版本说明中明确标注。

keystore、密码、本机 SDK 配置、缓存与 APK 都不进入 Git。正式签名证书的公开摘要可随安装包验证，私钥始终留在本机。
