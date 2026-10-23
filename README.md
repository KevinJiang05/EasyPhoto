# EasyPhoto

EasyPhoto 是一个简单的 Android 相机和相册，主要给家里的长辈使用。

最初是给奶奶做的，她目前已经在用。首页保留后置拍摄、前置拍摄和看照片三个入口，字体和按钮偏大，其他设置由家属调整。

[![Android CI](https://github.com/KevinJiang05/EasyPhoto/actions/workflows/android.yml/badge.svg)](https://github.com/KevinJiang05/EasyPhoto/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-476B5B.svg)](LICENSE)

[下载 APK](https://github.com/KevinJiang05/EasyPhoto/releases/latest) · [操作演示](https://github.com/KevinJiang05/EasyPhoto/releases/download/v0.3.0/EasyPhoto-demo.mp4)

## 界面

![EasyPhoto 操作预览：模拟器中的拍照与相册流程](docs/media/demo.gif)

<table>
<tr>
<td align="center"><img src="docs/media/home.png" width="210" alt="模拟器：三个日常入口"><p>三个日常入口</p></td>
<td align="center"><img src="docs/media/camera.png" width="210" alt="模拟器：图形快门与相机预览"><p>直接拍照</p></td>
<td align="center"><img src="docs/media/gallery.png" width="210" alt="模拟器：两列大图相册"><p>两列大图相册</p></td>
<td align="center"><img src="docs/media/viewer.png" width="210" alt="模拟器：全屏照片查看"><p>全屏看照片</p></td>
</tr>
</table>

以上是模拟器中的实际界面，相册使用准备的风景绘图，相机显示模拟器测试画面，没有使用家庭照片。[素材说明](docs/media/README.md)

## 功能

- 后置与前置拍摄，使用图形快门、返回和闪光灯按钮。
- 相册默认一排两张，支持全屏查看、翻页和缩放。
- 可以调整相册列数、按钮震动、拍照提示音和拍照后的操作。
- 照片保存在手机的 `Pictures/EasyPhoto/`，应用相册只查询这个目录。
- 管理员页面提供拍摄设置和照片整理、批量删除。

另有可选的飞书照片通知，默认关闭。开启后，照片先保存到手机，再由后台任务发送到配置好的群；上传失败不影响拍照。群里会保留照片副本，具体见[隐私与权限说明](docs/privacy-and-permissions.md)。

应用没有广告、账号注册和内购。

## 安装与设置

支持 Android 8.0 及以上，当前版本为 `0.3.0`。从 [Releases](https://github.com/KevinJiang05/EasyPhoto/releases/latest) 下载 APK，允许对应来源安装。第一次进入拍摄页时会申请相机权限。

初次使用时，可以由家里人陪着拍一张、找到照片，再返回首页，按使用习惯调整声音、震动和相册列数。

长按首页底部版本信息 4 秒可进入管理员页面，首次需要设置并确认 6 位数字 PIN。设置完成后关闭管理员模式。忘记 PIN 需要清除应用数据重新配置，飞书等设置也会重置。

使用飞书通知需要自行填写应用凭据和群机器人 Webhook，安装包中没有预置配置。

## 使用情况

早期版本在 Huawei Mate 20 上做过真机测试。`0.3.0` 新增的 PIN 流程和小屏结果页调整已在模拟器上检查，真机复测和覆盖升级验证仍待完成。不同手机的相机表现会有差别，夜景、HDR 等效果也取决于机型。

[验收记录](docs/acceptance-record.md) · [手动测试清单](docs/manual-test-checklist.md)

## 开发

项目使用 Kotlin、Jetpack Compose、CameraX、MediaStore 和 WorkManager，采用单模块结构。首页使用静态画面，相机在拍摄页打开；照片保存与飞书发送分开处理。

本地构建需要 JDK 17/21、Android SDK Platform 36 和仓库中的 Gradle Wrapper。Windows 下执行：

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

[构建与签名说明](docs/development.md) · [架构说明](docs/architecture.md) · [设计与实现记录](docs/showcase.md)

源码采用 [MIT License](LICENSE)，可以按自己家人的需要修改。问题可提交到 [Issues](https://github.com/KevinJiang05/EasyPhoto/issues)，也可以通过 App 首页的邮箱联系开发者。
