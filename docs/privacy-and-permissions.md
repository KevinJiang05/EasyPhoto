# 隐私与权限说明

## CAMERA

必须存在。EasyPhoto 的核心功能是拍照，需要相机权限才能打开后置摄像头。权限只在用户进入“拍照”功能时申请，不在 App 首次启动时申请。

## Android 9 读写存储权限

Mate 20 当前目标为 Android 9。Android 9 的公共图片目录仍使用旧存储权限模型，保存和读取 `Pictures/EasyPhoto/` 可能需要：

- `READ_EXTERNAL_STORAGE`，`maxSdkVersion=28`
- `WRITE_EXTERNAL_STORAGE`，`maxSdkVersion=28`

## Android 10+ 不申请存储权限

Android 10+ 使用 MediaStore 和 scoped storage。EasyPhoto 保存到 `Pictures/EasyPhoto/` 时通过 `RELATIVE_PATH` 定位目录，不需要申请传统存储权限。

## 不申请 READ_MEDIA_IMAGES

EasyPhoto 不读取手机所有图片，只查询本 App 保存目录。Android 13+ 的 `READ_MEDIA_IMAGES` 不适合本版本的最小权限原则。

## 不使用 MANAGE_EXTERNAL_STORAGE

EasyPhoto 不需要管理全部文件，也不扫描全盘照片。`MANAGE_EXTERNAL_STORAGE` 权限过大，第一版明确禁止。

## INTERNET 与 ACCESS_NETWORK_STATE

默认情况下 EasyPhoto 不请求网络，也不上传照片。管理员可在“飞书照片通知”中主动保存飞书配置并开启通知后，照片会在本机保存成功后异步发送到管理员指定的飞书私密群。

因此 Manifest 包含：

- `INTERNET`：仅用于管理员主动开启的飞书照片发送和重试。
- `ACCESS_NETWORK_STATE`：用于仅在设备已联网时调度发送任务，避免无意义的网络请求。
- `WAKE_LOCK`：由 AndroidX WorkManager 自动合并，用于网络恢复后的可靠短任务调度；不需要用户授权，也不会常驻保持屏幕开启。

该功能默认关闭；关闭后会取消未发送队列。通知任务不使用前台服务，也不会显示持续运行通知。App 不接入广告、统计、推送、账号体系或其他联网 SDK。飞书群消息会保留照片副本，这不是“照片不上云”的功能。
