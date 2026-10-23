# 开发环境

- Android Studio：使用支持 Android Gradle Plugin 8.13.2 的版本。
- JDK：17 或 21；项目编译目标为 JVM 17。
- Android SDK：Platform 36、Build-Tools 35.0.0 和 platform-tools。
- Gradle：使用仓库内的 Wrapper，当前版本见 `gradle/wrapper/gradle-wrapper.properties`。
- 用 Android Studio 打开项目并选择已安装的 SDK；本机 `local.properties` 中的 `sdk.dir` 不提交。
- 在项目根目录执行 README 中的构建、测试和 lint 命令；不依赖固定盘符或开发者目录。
- SDK 和 Gradle 缓存位置由各自本机配置决定，不要求修改全局环境变量或系统 PATH。
- 历史真机验证使用 Huawei Mate 20；不公开设备序列号，新机型需按手动清单验证。
