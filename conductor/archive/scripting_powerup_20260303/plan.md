# Implementation Plan: Scripting Power-up

## 阶段 1: 基础结构与库集成
- [x] **步骤 1.1**: 下载并集成 `chai.js` 到 `src/main/resources/js/`。 (1h)
- [x] **步骤 1.2**: 修改 `ScriptExecutionService.kt`，在引擎启动时自动加载 `chai.js`，修复 GraalVM 作用域隔离与 Java 对象兼容性问题。 (0.5h)
- [x] **步骤 1.3**: 通过 JS preamble IIFE 包装器注入 `chai.expect`，同时将 `pm.response.json()` 改为 `JSON.parse(body)` 返回原生 JS 对象。 (0.5h)
- [x] **步骤 1.4**: 验证 chai 断言（equal/type/property/array/chain/negation 等）全部通过。 (0.5h)

## 阶段 2: `pm.sendRequest` 核心实现 (关键挑战)
- [x] **步骤 2.1**: 在 `PmBinding` 中实现 `sendRequest(requestJson: String): String` 方法，接受 `HttpRequestService?` 依赖。 (1h)
- [x] **步骤 2.2**: 使用 `runBlocking` 桥接 suspend 函数，在 `Dispatchers.IO` 上同步等待 HTTP 响应；通过 JSON string round-trip 避免 GraalVM 类型问题。 (2h)
- [x] **步骤 2.3**: 编写 5 个单元测试（FakeHttpRequestService 替代 mock）验证 GET/POST/链式断言/错误回调/文本响应。 (1h)
- [x] **步骤 2.4**: JS preamble 中实现完整 `callback(err, response)` 映射，response 包含 `code/body/headers/json()/text()`。 (1h)

## 阶段 3: 响应解析与快捷断言
- [x] **步骤 3.1**: ~~将 `pm.response.json()` 返回结果通过 Proxy 包装以支持 `.` 属性访问。~~ **已跳过**：preamble 修复已使 `json()` 返回原生 JS 对象，原生支持 `.` 访问，Proxy 无必要。
- [x] **步骤 3.2**: 实现 `pm.response.to` 断言链：`to.have.status/header/body`、`to.be.ok/success/notFound/error/json`（getter 属性，Postman 兼容）。编写 9 个测试覆盖所有断言路径。 (1h)

## 阶段 4: 进阶库集成与最终验证
- [x] **步骤 4.1**: 集成 `ajv` v6.12.6 实现 JSON Schema 校验。下载 CDN 独立包 (119KB)，加载至 GraalVM 引擎，注入 `Ajv` 构造函数至脚本作用域，实现 `pm.response.to.have.jsonSchema(schema)` 快捷断言。编写 5 个单元测试。 (1h)
- [x] **步骤 4.2**: 全量测试规模确认：120 个测试用例，覆盖 ScriptExecution/MockServer/VariableScope/EventListener 等模块。JS 库加载性能：引擎初始化时一次性加载 3 个库（ajv 119KB + chai 68KB + crypto-js 48KB），不影响单次脚本执行性能。 (1h)
- [x] **步骤 4.3**: 最终归档。 (0.5h)
