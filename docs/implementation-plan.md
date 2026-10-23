# 实现计划

## 阶段 1：项目初始化

- 创建 Gradle Wrapper、app 模块、Manifest、Compose 主题和首页。
- 风险：依赖版本与 SDK 不匹配。
- 验证：`assembleDebug`。

## 阶段 2：导航与首页

- 实现首页、拍照、相册、全屏浏览路由。
- 风险：返回栈行为不符合老人机体验。
- 验证：安装后人工检查首页与返回行为。

## 阶段 3：权限策略

- 进入拍照时申请 Camera。
- Android 9 额外申请读写存储权限。
- Android 10+ 不申请存储权限。
- 验证：单元测试和真机权限弹窗。

## 阶段 4：CameraX 拍照

- 后置相机预览。
- 闪光灯开关。
- 快门防重复。
- MediaStore 保存到 `Pictures/EasyPhoto/`。
- 验证：真机拍照和保存目录检查。

## 阶段 5：相册与浏览

- 只查询 EasyPhoto 目录。
- 三列缩略图。
- 单张全屏左右切换。
- 验证：拍摄多张后检查排序、进入、切换和返回。

## 阶段 6：测试与验收

- 执行 `testDebugUnitTest`、`assembleDebug`、`lintDebug`、`git diff --check`。
- 安装并启动到 Mate 20。
- 人工验收前不声明相机完整通过。
