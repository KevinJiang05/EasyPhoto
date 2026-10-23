# 2026-06-27 拍摄页返回首页闪退修复验收

## 问题

- Mate 20 真机上，点击“后置拍摄”或“前置拍摄”进入拍摄页后，再点击“返回首页”会闪退。
- adb logcat 未发现 EasyPhoto 的 Java `FATAL EXCEPTION`，但出现 `handleAppDied: pkgName is com.kevin.easyphoto` 与华为相机 HAL 错误。

## 处理

- 首页不再创建或绑定 CameraX 实时预览。
- 相机资源只在“后置拍摄”和“前置拍摄”页面中打开。
- 管理员设置中停用“首页实时预览”开关，并说明 Mate 20 上该能力会触发相机底层稳定性问题。

## 构建验证

- `assembleDebug`：通过。
- `testDebugUnitTest`：通过。
- `lintDebug`：通过。
- `adb install -r app-debug.apk`：通过。

## 真机验证

- 设备：Huawei Mate 20，adb 状态为 `device`；设备序列号已省略。
- 后置拍摄页返回首页：通过，`mResumedActivity` 保持 `com.kevin.easyphoto/.MainActivity`，未出现 `handleAppDied`。
- 前置拍摄页返回首页：通过，`mResumedActivity` 保持 `com.kevin.easyphoto/.MainActivity`，未出现 `handleAppDied`。

## 结论

- 本轮闪退已修复。
- 后续如果仍想做首页实时预览，建议只作为新机型实验功能重新单独验证；Mate 20 上默认保持停用。
