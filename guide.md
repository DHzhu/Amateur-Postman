# Amateur-Postman 插件使用指南 (User Guide)

> [!NOTE]
> **Amateur-Postman** 是一款专为开发者设计的 IntelliJ IDEA 高性能、轻量级 API 调试与测试插件。它兼具 Postman 的脚本、变量、批量运行能力，同时深度契合 IDE 原生体验，支持超大响应预览及 gRPC/WebSocket 长连接调试。

本文档将详细向您介绍它所具备的各大核心功能面板及使用技巧。

---

## 目录
1. [HTTP 接口调试与分析](#1-http-接口调试与分析)
2. [多层级变量系统与 Quick Look 可视化](#2-多层级变量系统与-quick-look-可视化)
3. [自动化脚本与测试引擎 (Pre-request / Tests)](#3-自动化脚本与测试引擎-pre-request--tests)
4. [集合管理与导入导出 (HAR / Postman / OpenAPI)](#4-集合管理与导入导出-har--postman--openapi)
5. [批量测试工具 (Collection Runner)](#5-批量测试工具-collection-runner)
6. [gRPC 协议测试 (Unary & Streaming)](#6-grpc-协议测试-unary--streaming)
7. [WebSocket 长连接调试](#7-websocket-长连接调试)
8. [内置 Mock Server 服务](#8-内置-mock-server-服务)
9. [IDE 代码深度联动](#9-ide-代码深度联动)

---

## 1. HTTP 接口调试与分析

### 1.1 请求构建 (Request Builder)
您可以在主操作面板上创建并配置您的 HTTP 请求：
* **请求方法**：支持 `GET`、`POST`、`PUT`、`DELETE`、`PATCH`、`HEAD`、`OPTIONS` 等常用 HTTP 方法。
* **参数配置**：
  * **Headers & Query Params**：通过直观的键值表格进行编辑。
  * **Body**：提供多种编辑模式：
    * `raw` (JSON, Text, HTML, XML)：基于 IntelliJ 编辑器，支持自动补全及语法高亮。
    * `multipart/form-data`：用于表单或多文件上传。
    * `GraphQL`：专属 GraphQL 编辑面板，支持编写查询和变量。

### 1.2 响应预览与性能分析
* **High-Perf Response Viewer**：经过深度性能优化的响应预览器，即使是面对 10MB+ 的超大 JSON 响应也可以实现秒级加载、流畅解析、折叠与主题高亮。
* **时序瀑布流 (Timing Waterfall)**：在响应结果页的 "Timeline" 或 "Timing" 选项卡中，您可以看到详细的耗时拆解，包括 **DNS 解析**、**TCP 握手**、**SSL 握手**、**首字节时间 (TTFB)** 等，轻松定位性能瓶颈。

---

## 2. 多层级变量系统与 Quick Look 可视化

### 2.1 变量层级与覆盖规则
Amateur-Postman 支持类似 Postman 的四层变量系统。当变量名冲突时，会按照 **优先级：临时变量 > 环境变量 > 集合变量 > 全局变量** 进行覆盖：

| 变量级别 | 描述 | 修改方式 |
| :--- | :--- | :--- |
| **临时变量 (Temporary)** | 仅在当前请求周期内由 `Pre-request` 脚本动态生成。 | 通过 `am.environment.set()` 动态注入 |
| **环境变量 (Environment)** | 绑定于所选的环境配置（如 `dev`、`prod`），通常存放 baseUrl、Token 等。 | UI 环境管理面板 |
| **集合变量 (Collection)** | 仅在当前的 Collection (集合) 或子文件夹内生效。 | 集合右键属性面板 |
| **全局变量 (Global)** | 跨所有集合与环境，在当前 IDE 项目中均有效。 | 全局变量编辑框 |

### 2.2 动态占位符与动态变量
您可以在 URL、Headers、Body 中使用双大括号 `{{variable_name}}` 引用上述变量。
此外，插件还内置了以下系统动态变量：
* `{{$timestamp}}`：生成当前毫秒级时间戳。
* `{{$uuid}}`：生成一个随机的 UUID 字符串。
* `{{$randomInt}}`：生成一个随机的整数。

### 2.3 Environment Quick Look (变量悬浮窗)
点击界面右上角的 **眼睛 (Quick Look)** 图标，会弹出一个半透明悬浮窗：
* 实时显示当前环境下的所有可用变量。
* **变量来源追踪**：会自动将变量标记为 `Global`、`Environment`、`Collection` 还是 `Temporary`（由脚本设置的临时变量），并标识出当前生效的值与被覆盖的变量，避免多层变量混淆带来的调试困扰。

---

## 3. 自动化脚本与测试引擎 (Pre-request / Tests)

插件内置了基于 **GraalVM JS** 的脚本执行引擎，确保在脚本运行期有极佳的响应速度及兼容性。

### 3.1 请求前置脚本 (Pre-request Script)
在请求发送前执行。主要用于动态计算参数、设置环境变量、加解密等。
* **全局变量**：`am`
* **常用 API 示例**：
  ```javascript
  // 设置/读取环境变量 (临时变量)
  am.environment.set("token", "my-temp-token");
  var value = am.environment.get("token");

  // 使用内置函数
  var uuid = am.uuid();
  var randomNum = am.randomInt(1, 100);

  // 使用内置 CryptoJS 加密库计算签名
  var sign = CryptoJS.MD5("mySecret" + am.timestamp()).toString();
  am.environment.set("apiSignature", sign);
  ```

### 3.2 响应测试脚本 (Tests)
在获得响应后执行。主要用于编写断言或提取 Response 内容并传递给后续请求。
* **全局变量**：`pm`（为了完全兼容 Postman 生态）
* **常用 API 示例**：
  ```javascript
  // 1. 断言状态码
  pm.test("Status code is 200", function () {
      pm.response.to.have.status(200);
  });

  // 2. 简易断言响应体包含字符串
  pm.test("Body matches string", function () {
      pm.expect.body.toContain("success");
  });

  // 3. 使用内置 chai.js 库对 JSON 进行复杂校验
  pm.test("Check response json value", function () {
      var jsonData = pm.response.json();
      expect(jsonData.code).to.equal(0);
      expect(jsonData.data).to.be.an('object');
  });

  // 4. 使用内置 ajv 校验 JSON Schema 规范
  var schema = {
      "type": "object",
      "properties": {
          "id": { "type": "number" }
      },
      "required": ["id"]
  };
  pm.test("Schema is valid", function () {
      pm.response.to.have.jsonSchema(schema);
  });

  // 5. 提取 Token 传递给下一个接口
  var token = pm.response.json().data.token;
  pm.environment.set("jwtToken", token);

  // 6. 发起异步 HTTP 请求 (例如清空测试数据)
  pm.sendRequest({
      url: "http://localhost:8080/api/clear",
      method: "POST"
  }, function (err, res) {
      console.log("Cleanup status: " + res.code);
  });
  ```

---

## 4. 集合管理与导入导出 (HAR / Postman / OpenAPI)

所有的请求都保存在 **Collections** 树中（物理数据自动保存在项目目录的 `.idea/amateur-postman-collections.xml` 文件中，方便加入 Git 进行团队共享）。

### 4.1 导入选项
* **导入 Postman**：支持直接导入 `Postman Collection v2.1` 格式的 JSON 导出文件。
* **导入 HAR (HTTP Archive)**：
  1. 通过 Chrome DevTools、Fiddler 等工具录制并导出 `.har` 文件。
  2. 点击插件面板上的 `Import HAR`。
  3. 插件会自动解析并按 `Host` 进行层级分组，自动过滤不需要的静态资源。
  4. 支持通过树形 Checkbox 进行可视化预览，选择性勾选要导入的 API 请求。

### 4.2 导出选项
* **导出为 OpenAPI 3.0**：
  1. 在集合的根节点或子文件夹上右键，选择 `Export as OpenAPI 3.0`。
  2. 自动将嵌套的文件夹层级映射为 Tags 标签。
  3. 自动将 `{{变量}}` 解析并映射为路径参数或规范占位符。
  4. 支持输出为 YAML 或 JSON 文件。

---

## 5. 批量测试工具 (Collection Runner)

在集合（Collection）或文件夹节点上右键，选择 **Run Collection** 即可打开批量运行器：
* **运行列表**：展示该集合下的所有请求，支持通过拖拽或 Checkbox 调整运行顺序及是否参与运行。
* **批量设置**：可设置迭代次数 (Iterations)、请求间的延迟间隔 (Delay Ms)、以及数据关联文件 (CSV/JSON)。
* **完整生命周期**：依次按 `Pre-request` -> `HTTP Request` -> `Tests (Assertions)` 执行每一个接口。
* **运行报告**：运行结束后生成交互式报表，汇总 Assertion 通过率、失败请求详情与平均响应时长。

---

## 6. gRPC 协议测试 (Unary & Streaming)

不需要单独运行第三方 gRPC 测试工具，Amateur-Postman 深度集成了 gRPC 协议调试：

### 6.1 配置方式
1. 切换协议面板至 `gRPC`。
2. 配置或导入您的 `.proto` 文件夹。插件会自动解析所有的 `service` 与 `method`。
3. 选择对应的 gRPC 目标地址，如 `localhost:50051`。

### 6.2 调用支持
* **Unary Call (一元调用)**：填写 JSON 格式 of Request Body，支持 `{{变量}}` 占位符解析，一键发起调用。
* **流式调用 (Streaming)**：
  * **Server Streaming (服务端流)**：一次发送，持续接收服务端推送的流数据，响应列表将持续虚拟滚动更新。
  * **Client Streaming (客户端流)**：可在连接建立后，手动多次点击 "Send Message" 发送 JSON 帧，最后点击 "Commit/Complete" 结束输入。
  * **Bi-directional Streaming (双向流式)**：客户端与服务端均可独立不间断发送/接收数据。

---

## 7. WebSocket 长连接调试

提供全功能的 WebSocket / Web Socket Secure (WSS) 连接调试面板：
* 输入以 `ws://` 或 `wss://` 开头的 WebSocket 链接。
* **状态保持**：点击 Connect 建立连接，顶部会显示实时的 Connection 状态。
* **双向消息监控**：发送端支持 Text、JSON 格式的消息。所有接收及发送的历史帧，会以气泡式消息流的时间轴形式平滑渲染在 Response 区。
* 支持在连接生命周期内，使用全局变量对发送内容进行动态替换。

---

## 8. 内置 Mock Server 服务

Amateur-Postman 内置了基于 `java.net.ServerSocket` 开发的高性能、轻量级本地 Mock 服务器，方便在后端接口未就绪时进行前端联调。

### 8.1 规则设置 (Mock Rules)
您可以添加多条匹配规则（数据持久化保存在 `.idea/amateur-postman-mock.xml` 中）：
* **路径与方法**：如 `GET /api/v1/user`。
* **匹配权重优先级**：当多个规则冲突时，优先匹配 `Priority`（优先级）数值更高的规则。
* **请求体匹配模式 (Body Matcher)**：支持 `NONE`（仅匹配路径/方法）、`JSON` 精确属性匹配、以及 `REGEXP`（正则表达式匹配请求体）。

### 8.2 稳定特性
* **延时响应**：支持设置 `delayMs`（延迟时间），可用于测试前端的 loading 交互或超时的边界逻辑。
* **流式 Body 与 OOM 自动保护**：即使 Mock 接收或下发超大的请求体，底层依然能进行分块读取，自动释放内存，避免撑爆您的 IDE。

---

## 9. IDE 代码深度联动

作为 IDEA 插件，Amateur-Postman 与您的项目源码有着完美的互操作性：
* **Controller 快速发起 (Gutter Icon)**：
  如果您在写 Spring Boot (`@RestController` / `@RequestMapping`) 或 JAX-RS (Jakarta) 路由代码，在控制器的具体方法行号左侧，会显示一个 **AP 蓝色小图标**。
* **一键跳转**：点击该 Gutter Icon，插件会立即在工具栏中自动生成对应 HTTP 路由方法的调试请求，无需手动输入 URL。
