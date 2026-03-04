# Specification: Scripting Power-up (Advanced API)

## 目标
将插件的脚本能力从简单的断言和变量设置，提升为能够控制复杂 API 流程的工具，最大程度兼容 Postman 脚本生态。

## 关键功能定义

### 1. `pm.sendRequest(request, callback)`
- **功能**: 在 Pre-request 或 Test 脚本中发起额外的 HTTP 请求。
- **技术挑战**: JS 是同步运行的，但请求必须是异步的。
- **方案**: 实现一个桥接类，在 Kotlin 侧调用 `HttpRequestService` 发起请求，并将结果回调给 JS 环境。由于 GraalVM 限制，可能需要将 `callback` 包装为 `Value` 对象并在主线程执行完毕后调用，或者利用协程挂起机制。

### 2. 增强型断言库 (Chai.js 风格)
- **功能**: 支持 `pm.expect(data).to.be.an('object')` 等链式调用。
- **方案**: 引入 `chai.js` 作为内置资源加载，并映射到 `pm.expect`。

### 3. 响应解析增强
- **功能**: 支持 `pm.response.json()` 返回 Proxy 对象以支持直接访问属性，提供 `pm.response.to.have.status(200)` 等原生快捷断言。

### 4. 更多第三方库集成
- **功能**: 集成 `ajv` (JSON Schema 校验)、`xml2js` 等。

## 验收标准
- 可以在脚本中调用 `pm.sendRequest` 获取另一个接口的数据并将其设置为变量。
- 脚本中支持复杂的 `pm.expect` 断言。
- 支持在脚本中进行 JSON Schema 格式校验。
