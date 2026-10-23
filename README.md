# EasyPhoto

这是我给奶奶做的一个 Android 相机，她现在已经开始用了。

我想把她要用的东西放在最显眼的地方：拍照、自拍、看照片。按钮大一些，步骤少一些，平时用不着的设置留给家里人处理。EasyPhoto 就是围绕这几件事慢慢做出来的。

[![Android CI](https://github.com/KevinJiang05/EasyPhoto/actions/workflows/android.yml/badge.svg)](https://github.com/KevinJiang05/EasyPhoto/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-476B5B.svg)](LICENSE)

[下载 APK](https://github.com/KevinJiang05/EasyPhoto/releases/latest) · [看一段操作演示](https://github.com/KevinJiang05/EasyPhoto/releases/download/v0.3.0/EasyPhoto-demo.mp4)

## 用起来是什么样

打开以后，首页就是三个大入口。点进去拍照，拍完可以接着拍，也可以去看照片。相册默认一排两张，点开后能翻页、放大，再回到首页。

![EasyPhoto 操作预览：模拟器中的拍照与相册流程](docs/media/demo.gif)

<table>
<tr>
<td align="center"><img src="docs/media/home.png" width="210" alt="模拟器：三个日常入口"><p>三个日常入口</p></td>
<td align="center"><img src="docs/media/camera.png" width="210" alt="模拟器：图形快门与相机预览"><p>直接拍照</p></td>
<td align="center"><img src="docs/media/gallery.png" width="210" alt="模拟器：两列大图相册"><p>两列大图相册</p></td>
<td align="center"><img src="docs/media/viewer.png" width="210" alt="模拟器：全屏照片查看"><p>全屏看照片</p></td>
</tr>
</table>

这里放的是模拟器录屏和截图，相册里的风景画也是专门准备的演示图，没有用家人的照片。相机里的色块是模拟器测试画面，真机拍出来当然不是这样。[素材说明](docs/media/README.md)

## 给家里人准备的几个功能

- **拍照和看照片放在前面。** 后置、前置、相册各有一个大入口，快门、返回和翻页都用了图形按钮。
- **可以按习惯调整。** 要不要震动、拍完要不要响一声、照片能不能缩放，这些都能由家里人设置。
- **照片存在手机里。** 保存位置是 `Pictures/EasyPhoto/`，App 里的相册只看这个目录。
- **设置单独收起来。** 长按首页底部的版本信息 4 秒，由家属设置 6 位数字 PIN。进去后可以调整拍摄设置，也可以整理、删除照片。
- **想收到照片的话，可以接飞书。** 这个功能默认关闭。开启后，照片先在手机里保存，再发到配置好的群里；网络不好也不会耽误拍照。

没有广告，不需要注册账号，也没有内购。飞书通知打开后，群里会留下一份照片，配置前可以先看看[隐私与权限说明](docs/privacy-and-permissions.md)。

## 想给家里的长辈试试

在 [Releases](https://github.com/KevinJiang05/EasyPhoto/releases/latest) 下载 APK，允许对应来源安装后就能使用。支持 Android 8.0 及以上，当前版本是 `0.3.0`，第一次进入拍摄页会询问相机权限。

建议家里人先陪着走一遍：拍一张、找到它、点开看看，再返回首页。顺手把声音、震动和相册大小调合适，设置好 PIN，最后关闭管理员模式。

PIN 请记好。忘记后需要清除应用数据重新配置，飞书等设置也会一起重置。飞书通知需要自己填写应用凭据和群机器人 Webhook，安装包里没有预置配置。

之前在 Huawei Mate 20 上做过真机测试。这次发布新增的 PIN 流程和小屏结果页调整已经在模拟器上检查过，还没来得及拿真机复测，覆盖升级也待确认。不同手机的相机表现会有差别，夜景、HDR 这些效果也不能按原厂相机来期待。具体记录放在[验收文档](docs/acceptance-record.md)里。

## 写代码时踩过的坑

最折腾的是相机。早期想在首页也放实时预览，结果在 Mate 20 上遇到了卡死和拍照失败。后来查到，首页退出时的解绑操作会把拍摄页的相机也一起关掉。现在首页用静态画面，相机留在拍摄页打开，避免两边争用。

飞书通知则放到了后台：照片存好以后才交给 WorkManager，断网了就等网络恢复再重试。拍照本身不用等上传结果。

这些过程和取舍整理在了[项目小记](docs/showcase.md)和[架构说明](docs/architecture.md)里，有兴趣可以接着看。

## 自己编译

项目用 Kotlin、Jetpack Compose、CameraX、MediaStore 和 WorkManager，只有一个 App 模块。

准备 JDK 17/21、Android SDK Platform 36，使用仓库里的 Gradle Wrapper。Windows 下执行：

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
```

[构建、安装与签名说明](docs/development.md) · [手动测试清单](docs/manual-test-checklist.md)

源码按 [MIT License](LICENSE) 开放，欢迎拿去改成适合自己家人的样子。遇到问题可以提 [Issue](https://github.com/KevinJiang05/EasyPhoto/issues)，也可以通过 App 首页的邮箱联系我。
