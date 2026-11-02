# Track: 升级 IDEA 版本支持至 2026.3

## 目标
将插件最高支持版本升级到 IntelliJ IDEA 2026.3 (263)，同时保持对 253–262 的兼容。

## 任务列表
- [x] **task-1** 调研 2026.3 (263) 的 API 变更、不兼容项及 IntelliJ Platform Gradle Plugin 最低版本要求，同步至 `spec.md`。 (01d9044)
- [x] **task-2** 准备 2026.3 构建 IDE（本地安装至 `/opt/idea/` 或改为远程依赖），更新 `build.gradle.kts` 中的 IDE 依赖声明。 (01d9044)
- [x] **task-3** 更新 `gradle.properties`：`pluginUntilBuild = 263.*`、`platformVersion = 2026.3`；按需升级 `libs.versions.toml` 中的 `intelliJPlatform` 等插件版本。 (01d9044)
- [x] **task-4** 更新 `pluginVerification`，覆盖 2025.3 / 2026.2 / 2026.3。 (01d9044)
- [x] **task-5** 运行 `./gradlew build` 与 `./gradlew test`，修复编译错误与测试失败。 (01d9044)
- [x] **task-6** 运行 `./gradlew verifyPlugin`，修复废弃/内部/不兼容 API 问题。 (01d9044)
- [x] **task-7** 更新 `CHANGELOG.md`，完成代码审查与轨道归档。 (cb86ab9)
