# Amateur-Postman

![Build](https://github.com/DHzhu/Amateur-Postman/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

<!-- Plugin description -->
**Amateur-Postman** 是一款集成在 IntelliJ IDEA 内部的高性能、轻量级 HTTP 客户端插件。它专为追求极简体验的开发者设计，通过内嵌的 GraalVM JavaScript 引擎和 Netty Mock 服务，提供足以替代专业工具的脚本化测试能力，同时保持极低的代码侵入性与内存占用。
<!-- Plugin description end -->

## ✨ 核心特性

- 🚀 **极简 UI**: 聚焦核心测试流程，支持**虚拟滚动（Virtual Scrolling）**，轻松应对 1000+ 请求集合。
- 📜 **脚本增强**: 内置 GraalVM JS，深度兼容 Postman API（支持 `pm.sendRequest`、`chai.js` 断言、`ajv` 模式校验、`CryptoJS` 等）。
- 🧬 **变量与可视化**: 动态解析全局、环境、集合变量，支持 **Environment Quick Look** 悬浮窗与变量来源/优先级追踪。
- 🛠️ **内置 Mock Server**: 基于 Netty 实现，具备规则权重匹配、流式 Body 读取与 OOM 自动保护机制。
- 🔥 **高性能表现**: 深度优化的 **High-Perf Response Viewer**，支持 10MB+ 超大响应流畅预览与主题感知高亮。
- 📊 **性能分析**: 详细的请求耗时分解，提供 DNS、TCP、SSL、TTFB 等关键节点的时序瀑布流视图。
- 🔌 **gRPC 支持**: 动态加载 `.proto` 文件，无需代码生成，支持 Unary 调用、Metadata 注入与变量解析。
- ✅ **稳健性保障**: 拥有 **330+** 核心单元测试（基于 JUnit 5），确保在高性能场景下的稳定性。

## 📸 快速上手

### 安装方式
1. **IDE 内安装**:
   <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > 搜索 **"Amateur-Postman"** > <kbd>Install</kbd>
2. **离线安装**:
   在 [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) 下载最新发布版 Zip 包，通过 `Install plugin from disk...` 安装。

## 🗺️ 路线图 (Roadmap)

- [x] **Phase 1: 核心请求与集合管理** (Completed)
- [x] **Phase 2: 脚本引擎增强 (Advanced API & pm.sendRequest)** (Completed)
- [x] **Phase 3: 变量可视化 (Environment Quick Look)** (Completed)
- [x] **Phase 4: 内置 Mock Server 与规则引擎** (Completed)
- [x] **Phase 5: UI/UX 性能优化与虚拟滚动** (Completed)
- [x] **Phase 6: 性能分析与时序瀑布流** (Completed)
- [ ] **Phase 7: 协议扩展 — gRPC Unary Call** (✅ Completed)
- [ ] **Phase 8: 协议扩展 — WebSocket & gRPC Streaming** (Upcoming)
- [ ] **Phase 9: 外部脚本库动态加载支持** (Upcoming)

## 🤝 参与贡献

我们欢迎任何形式的贡献！在提交 Pull Request 之前，请阅读我们的 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 📄 开源协议

本项目基于 **MIT License** 开源 - 详情请参阅 [LICENSE](LICENSE) 文件。

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
