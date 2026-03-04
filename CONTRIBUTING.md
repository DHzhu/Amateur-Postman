# 参与贡献

感谢你对 Amateur-Postman 的关注！作为一个开源项目，我们非常欢迎社区的反馈、建议和 Pull Requests。

## 🛠️ 开发环境配置

1. **JDK 版本**: 确保已安装 **JDK 21**。
2. **IDE**: 推荐使用最新版本的 **IntelliJ IDEA** (Community 或 Ultimate)。
3. **插件 SDK**: 本项目基于 IntelliJ Platform Plugin Template。打开项目后，IDEA 会自动同步 Gradle 配置。

## 🏗️ 构建与运行

- **编译项目**: `./gradlew build`
- **运行测试**: `./gradlew test` (本项目拥有 280+ 测试用例，请确保全部通过)
- **启动沙盒 IDE 调试**: `./gradlew runIde`

## 📝 提交规范

1. **分支策略**: 所有的开发应在 `feature/` 或 `bugfix/` 分支进行。
2. **代码风格**: 遵循 [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)。本项目已集成代码检查。
3. **提交记录**: 建议使用清晰简洁的提交信息，例如：
   - `feat: add environment variable visualization`
   - `fix: resolve NPE in MockServerManager`
   - `docs: update contributing guide`

## 🚀 提 PR 流程

1. **Fork** 本仓库。
2. 创建你的 **Feature 分支** (`git checkout -b feature/AmazingFeature`)。
3. **Commit** 你的修改 (`git commit -m 'Add some AmazingFeature'`)。
4. **Push** 到远程分支 (`git push origin feature/AmazingFeature`)。
5. 在 GitHub 上发起 **Pull Request**。

## ⚠️ 核心红线

- **测试先行**: 新增功能必须包含相应的单元测试。
- **性能敏感**: 修改 UI 部分时，请注意对大数据量响应的兼容性。
- **安全第一**: 禁止硬编码任何 API 密钥或敏感信息。

---

再次感谢你帮助 Amateur-Postman 变得更好！🚀
