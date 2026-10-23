# 架构说明

EasyPhoto 是单模块 Android App，使用 Kotlin、Jetpack Compose、CameraX 和 MediaStore。

## 模块结构

- `MainActivity.kt`：Activity 入口，承载 Compose。
- `navigation/`：页面路由和 NavHost。
- `feature/home/`：首页，后置拍摄、前置拍摄、看照片三个大入口，以及管理员 PIN 设置和验证入口。
- `feature/camera/`：权限说明、CameraX 预览、拍照和拍照成功页。
- `feature/gallery/`：照片网格、空状态。
- `feature/viewer/`：单张照片全屏浏览、左右切换、双指缩放和拖动。
- `data/media/`：照片文件名、MediaStore 保存和查询、排序。
- `util/`：权限策略。

## 关键决策

- 相机使用 CameraX `Preview` + `ImageCapture`，不调用系统相机。
- 保存使用 MediaStore，目标目录为 `Pictures/EasyPhoto/`。
- 拍摄页提前准备下一张照片的输出位置，保存完成后再写入真实文件名和拍摄时间，兼顾响应速度与相册排序。
- 日常模式使用图形快门、图形返回、闪光灯开关状态图形、可选震动和本地提示音，避免核心流程依赖识字。
- 相册列数、全屏翻页箭头、双指缩放、震动和提示音由管理员设置持久化控制。
- 震动使用 `VIBRATE` 权限直接触发短振动，不依赖系统“触摸时振动”开关；提示音使用媒体音量通道，避免勿扰模式屏蔽通知音。
- 前置镜像设置同时作用于 `PreviewView` 与 `ImageCapture.Metadata`，保证预览和保存照片方向一致。
- Android 10+ 使用 `RELATIVE_PATH` 和 `IS_PENDING`。
- Android 8–9 使用公共 Pictures/EasyPhoto 路径，并只在需要时申请读写权限。
- 相册只查询 EasyPhoto 目录，不扫描全盘。
- 相机与相册直接使用 CameraX 和 MediaStore；可选飞书通知使用 HTTP 和 WorkManager，不引入 DI 框架。

## 默认桌面能力

Manifest 同时声明 `MAIN + LAUNCHER` 和 `MAIN + HOME + DEFAULT`。应用不会自动修改默认桌面。管理员可开启软锁定，在离开应用时尝试返回；该行为受系统限制，不实现 Device Owner、Lock Task 或无障碍拦截，也不等同于企业 Kiosk。

## 飞书照片通知（可选）

管理员开启通知后，`CameraScreen` 仅在照片已成功写入 `Pictures/EasyPhoto/` 后，将对应 `content://` 地址提交给 `FeishuPhotoScheduler`。任务由 WorkManager 在已联网时执行；网络问题使用指数退避重试，最多 5 次，且不会阻塞相机。

`FeishuSecureConfigStore` 使用 Android Keystore 的 AES-GCM 密钥加密 App ID、App Secret 和私密群 Webhook 地址。`FeishuDirectClient` 依次获取飞书短期令牌、上传图片并发送群消息。应用日志只记录结果与错误摘要，绝不记录照片内容或凭证。
