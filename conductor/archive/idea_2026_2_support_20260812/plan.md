# Track: 升级 IDEA 版本支持至 2026.2

## 目标
将插件的最高支持版本升级到 IntelliJ IDEA 2026.2，包括更新相关配置并解决任何潜在的兼容性问题。

## 任务列表
- [x] 1. 调研 2026.2 版本的兼容性指南和变更。
- [x] 2. 更新 `gradle.properties`，修改 `pluginUntilBuild` 至 `262.*` 等相关版本属性。
- [x] 3. 运行项目构建与测试 (`./gradlew build` 和 `./gradlew test`)，修复潜在的编译错误。
- [x] 4. 运行插件验证工具 (`./gradlew verifyPlugin`)。
- [x] 5. 解决任何由于废弃 API 引发的警告或错误。
