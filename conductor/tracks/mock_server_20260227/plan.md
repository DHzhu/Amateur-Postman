# Implementation Plan - Mock Server Support

## Phase 1: 核心 Mock 引擎实现 ✅ PENDING
- [ ] Task: [TDD] 编写 MockServer 基础测试，验证端口启动和简单的路径路由匹配
- [ ] Task: 实现 `MockRule` 模型和 `MockServerManager` 服务
- [ ] Task: 集成 Sun Http Server 或 OkHttp MockWebServer 到插件生命周期
- [ ] Task: 实现基础的匹配算法 (Path + Method)
- [ ] Task: Conductor - User Manual Verification 'Core Mock Engine'

## Phase 2: 配置 UI 与持久化 ✅ PENDING
- [ ] Task: 创建 `MockServerPanel` 用于管理规则列表
- [ ] Task: 实现 Mock 规则的 XML 持久化存储
- [ ] Task: 在 Collection 树形菜单中添加 "Create Mock from Request" 操作
- [ ] Task: Conductor - User Manual Verification 'Mock UI & Persistence'

## Phase 3: 高级功能与优化 ✅ PENDING
- [ ] Task: 支持响应延迟模拟 (Latency)
- [ ] Task: 支持请求体精确/正则匹配
- [ ] Task: 编写集成测试，验证通过插件发送请求到本地 Mock 端口并获得预期响应
- [ ] Task: Conductor - User Manual Verification 'Advanced Mock Features'
