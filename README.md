# Amateur-Postman

![Build](https://github.com/DHzhu/Amateur-Postman/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

<!-- Plugin description -->
**Amateur-Postman** 是一款集成在 IntelliJ IDEA 内部的高性能、轻量级 HTTP 客户端插件。它专为追求极简体验的开发者设计，通过内嵌的 GraalVM JavaScript 引擎和 Netty Mock 服务，提供足以替代专业工具的脚本化测试能力，同时保持极低的代码侵入性与内存占用。
<!-- Plugin description end -->

## ✨ 核心特性

- 🚀 **极简 UI**: 聚焦核心测试流程，告别臃肿的界面。
- 📜 **脚本增强**: 集成 GraalVM JS，完美兼容 Postman 风格脚本（支持 `pm.sendRequest`、`chai.js` 断言、`CryptoJS` 等）。
- 🧬 **多级变量系统**: 支持 Global、Environment、Collection 三层作用域，通过 `VariableResolver` 实现动态占位符解析。
- 🛠️ **内置 Mock Server**: 基于 Netty 实现，支持自定义规则匹配、权重控制及 OOM 保护。
- 🔥 **高性能表现**: 深度优化的 UI 渲染逻辑，支持 1000+ 请求管理与 5MB+ 响应流畅预览。
- ✅ **稳健性保障**: 拥有 280+ 核心单元测试（覆盖 Mock, Scripting, JUnit 5 迁移等）。

## 📸 快速上手

### 安装方式
1. **IDE 内安装**:
   <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > 搜索 **"Amateur-Postman"** > <kbd>Install</kbd>
2. **离线安装**:
   在 [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) 下载最新发布版 Zip 包，通过 `Install plugin from disk...` 安装。

## 🗺️ 路线图 (Roadmap)

- [x] **Phase 1: 核心请求与集合管理** (Completed)
- [x] **Phase 2: Mock Server 与脚本增强** (Completed)
- [ ] **Phase 3: 变量可视化 (Environment Quick Look)** (In Progress)
- [ ] **Phase 4: UI/UX 性能优化与虚拟滚动** (Upcoming)

## 🤝 参与贡献

我们欢迎任何形式的贡献！在提交 Pull Request 之前，请阅读我们的 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 📄 开源协议

本项目基于 **MIT License** 开源 - 详情请参阅 [LICENSE](LICENSE) 文件。

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
