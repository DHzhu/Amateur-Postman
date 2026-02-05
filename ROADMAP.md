# Amateur-Postman 功能现状与发展规划

## 📊 当前功能清单（已完成）

### ✅ 核心功能（已完成）

#### 1. HTTP 请求功能
- ✅ **HTTP 方法支持**：GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
- ✅ **URL 输入**：完整的 URL 输入和验证
- ✅ **请求参数**：Query Params（表格形式，自动 URL 编码）
- ✅ **请求头**：Headers 表格编辑，默认 Content-Type
- ✅ **请求体**：Body 文本编辑，支持 JSON 格式化
- ✅ **认证方式**：
  - Basic Auth（用户名/密码）
  - Bearer Token（JWT 令牌）
  - No Auth

#### 2. 响应显示功能
- ✅ **响应状态**：状态码、状态消息、响应时间
- ✅ **响应体**：
  - 语法高亮显示（JSON）
  - Headers 显示
  - Raw 原始响应
- ✅ **响应操作**：复制响应、清空响应
- ✅ **响应大小**：自动格式化（B/KB/MB）
- ✅ **JSON 格式化**：自动美化和语法高亮

#### 3. 历史记录功能
- ✅ **自动保存**：每次请求自动记录（最多 100 条）
- ✅ **持久化存储**：使用 IntelliJ PersistentStateComponent
- ✅ **历史管理**：
  - 加载历史请求
  - 删除单个/清空全部
  - 重命名历史项
  - 搜索过滤
- ✅ **状态指示**：成功（绿色）/失败（红色）/等待中（灰色）
- ✅ **右键菜单**：Load、Copy URL、Copy as cURL、Rename、Delete

#### 4. cURL 集成
- ✅ **导入**：解析 cURL 命令为请求
- ✅ **导出**：将请求导出为 cURL 命令
- ✅ **支持特性**：
  - 多种 cURL 参数解析
  - Basic Auth 自动转换
  - 多行格式支持

#### 5. UI/UX 功能
- ✅ **工具窗口**：位于 IDE 右侧
- ✅ **分割布局**：左侧历史 + 右侧请求/响应
- ✅ **标签页组织**：Params、Authorization、Headers、Body
- ✅ **响应标签页**：Body、Headers、Raw
- ✅ **键盘快捷键**：
  - `Ctrl+Enter` - 发送请求
  - `Ctrl+L` - 清空响应
  - `Escape` - 取消请求
  - `Enter/Delete` - 历史操作
- ✅ **请求取消**：支持取消进行中的请求

#### 6. 技术特性
- ✅ **异步处理**：Kotlin Coroutines，非阻塞 UI
- ✅ **错误处理**：优雅的异常捕获和显示
- ✅ **资源管理**：正确实现 Disposable
- ✅ **单元测试**：完整的测试覆盖（Models、Utils）

---

## 🎯 Postman 核心功能对比

### Postman 完整功能列表（参考）

根据 [Postman 官方文档](https://www.postman.com/api-platform/api-testing/) 和 2025 最新版本：

#### 📦 请求管理
1. **请求构建器**
   - ✅ HTTP 方法（已完成）
   - ✅ URL 和 Params（已完成）
   - ✅ Headers（已完成）
   - ✅ Body（已完成，但有限）
   - ❌ **Multi-part/form-data**（文件上传）
   - ❌ **Raw 选项**（Text、JSON、XML、HTML 等）
   - ❌ **Binary 文件上传**
   - ❌ **GraphQL 请求**

2. **认证**
   - ✅ Basic Auth（已完成）
   - ✅ Bearer Token（已完成）
   - ❌ **OAuth 1.0a**
   - ❌ **OAuth 2.0**
   - ❌ **API Key**
   - ❌ **Digest Auth**
   - ❌ **AWS Signature**
   - ❌ **NTLM**
   - ❌ **Hawk Authentication**

3. **请求组织**
   - ❌ **Collections**（请求集合）
   - ❌ **Folders**（文件夹）
   - ❌ **保存请求**（Save Request）
   - ❌ **请求描述和文档**

#### 🔧 环境和变量
1. **环境管理**
   - ✅ **Environments**（环境切换）
   - ✅ **Variables**（变量）
   - ✅ **全局变量**
   - ❌ **集合变量**（未来 Phase 3）

2. **变量类型**
   - ✅ **环境变量**
   - ✅ **全局变量**
   - ❌ **集合变量**（未来 Phase 3）
   - ❌ **本地变量**
   - ❌ **数据变量**

#### 🧪 测试和自动化
1. **测试脚本**
   - ❌ **Tests 标签页**（响应断言）
   - ❌ **Pre-request Script**（请求前脚本）
   - ❌ **断言库**（pm.test, pm.expect 等）
   - ❌ **Chai.js 集成**

2. **自动化测试**
   - ❌ **Collection Runner**（批量运行）
   - ❌ **数据驱动测试**（CSV/JSON）
   - ❌ **测试报告**（HTML/JSON）
   - ❌ **CI/CD 集成**

3. **Mock 服务器**
   - ❌ **Mock Server**
   - ❌ **示例响应**

#### 📊 响应处理
1. **响应查看**
   - ✅ Body、Headers、Raw（已完成）
   - ❌ **Preview**（HTML 预览）
   - ❌ **Visualize**（JSON 可视化）
   - ❌ **Cookies**（Cookie 管理）
   - ❌ **响应时间线**（Waterfall）
   - ❌ **响应大小统计**（详细）

2. **响应保存**
   - ❌ **保存响应示例**
   - ❌ **响应比较**

#### 🔄 协作和分享
1. **团队功能**
   - ❌ **工作区**（Workspaces）
   - ❌ **共享集合**
   - ❌ **注释和评论**

2. **导入导出**
   - ✅ cURL（已完成）
   - ❌ **Postman Collection 导入**
   - ❌ **Postman Collection 导出**
   - ❌ **OpenAPI (Swagger) 导入**

#### 🎨 用户体验
1. **界面定制**
   - ❌ **主题切换**（Light/Dark）
   - ❌ **字体大小调整**
   - ❌ **布局定制**

2. **搜索功能**
   - ✅ 历史搜索（已完成，但简单）
   - ❌ **全局搜索**（跨集合）
   - ❌ **高级过滤**（按状态码、时间等）

#### 🚀 高级功能
1. **API 设计**
   - ❌ **API 设计器**
   - ❌ **OpenAPI/Swagger 生成**
   - ❌ **API 文档生成**

2. **监控和调试**
   - ❌ **API 监控**
   - ❌ **请求拦截**（Proxy）
   - ❌ **Cookies 管理**
   - ❌ **Certificates**（客户端证书）
   - ❌ **Proxy 设置**

3. **Integrations**
   - ❌ **版本控制集成**（Git）
   - ❌ **CI/CD 集成**（Jenkins, GitHub Actions）
   - ❌ **Slack/Teams 通知**

---

## 🗺️ 发展路线图

### Phase 1: 增强请求功能（优先级：高）

#### 1.1 请求体增强
- [ ] **Raw 编辑器选项**
  - [ ] Text、JSON、XML、HTML、JavaScript
  - [ ] Content-Type 自动设置
  - [ ] 语法高亮和格式化
- [ ] **文件上传**
  - [ ] Multipart/form-data 支持
  - [ ] 文件选择器
  - [ ] 拖拽上传
- [ ] **GraphQL 支持**
  - [ ] GraphQL 查询编辑器
  - [ ] GraphQL 变量
  - [ ] schema 检测

**估算时间**: 2-3 周

#### 1.2 更多认证方式
- [ ] OAuth 2.0（最常用）
- [ ] API Key
- [ ] Digest Auth
- [ ] 自定义 Header 认证

**估算时间**: 1-2 周

### Phase 2: 环境和变量（优先级：高）✅ **已完成**

#### 2.1 环境管理 ✅
- ✅ **环境管理器 UI**
  - ✅ 创建/编辑/删除环境
  - ✅ 环境切换器（下拉菜单）
  - ✅ 环境变量编辑器（表格形式）
- ✅ **变量类型**
  - ✅ 初始值
  - ✅ 当前值
  - ✅ 启用/禁用开关
  - ✅ 持久化存储（`amateur-postman-env.xml`）

**完成时间**: 2025-02-05
**实现内容**:
- EnvironmentService 服务（持久化、CRUD 操作）
- EnvironmentPanel UI（环境选择、变量表格）
- EnvironmentModels 数据模型
- 完整单元测试（22+ 测试用例）

#### 2.2 变量系统 ✅
- ✅ **变量语法**
  - ✅ `{{variableName}}` 支持
  - ✅ 自动变量替换（URL、Headers、Body）
- ✅ **变量作用域**
  - ✅ 全局变量
  - ✅ 环境变量
  - ✅ 环境变量优先级高于全局变量
- ✅ **变量函数**
  - ✅ `{{$timestamp}}`、`{{$timestamp:format}}`
  - ✅ `{{$uuid}}`、`{{$guid}}`
  - ✅ `{{$randomInt}}`、`{{$randomInt:min,max}}`、`{{$randomInt:length}}`
  - ✅ `{{$randomString:length}}`
- ✅ **高级特性**
  - ✅ 递归变量解析（最多 10 层）
  - ✅ 大小写不敏感的变量查找
  - ✅ 变量验证和缺失检测

**完成时间**: 2025-02-05
**实现内容**:
- VariableResolver 工具类
- 自动集成到 HttpRequestServiceImpl
- 完整单元测试（31+ 测试用例）
- 支持所有常用 Postman 变量函数

**文件清单**:
- `src/main/kotlin/.../models/EnvironmentModels.kt`
- `src/main/kotlin/.../services/EnvironmentService.kt`
- `src/main/kotlin/.../utils/VariableResolver.kt`
- `src/main/kotlin/.../ui/EnvironmentPanel.kt`
- `src/test/kotlin/.../models/EnvironmentModelsTest.kt`
- `src/test/kotlin/.../utils/VariableResolverTest.kt`

### Phase 3: 请求组织和保存（优先级：中）✅ **已完成**

#### 3.1 请求集合（Collections） ✅ **已完成**
- ✅ **集合管理**
  - ✅ 创建集合
  - ✅ 文件夹结构
  - ✅ 拖拽组织（基础版）
- ✅ **保存请求**
  - ✅ 保存到集合
  - ✅ 请求描述
  - ✅ 请求元数据

**完成时间**: 2025-02-05
**实现内容**:
- CollectionModels 数据模型（RequestCollection, CollectionItem with Folder/Request）
- CollectionService 服务（持久化、CRUD 操作、文件夹管理）
- CollectionsPanel UI（树形视图、右键菜单、双击加载）
- SaveRequestDialog UI（保存请求对话框）

#### 3.2 导入导出 ✅ **已完成**
- ✅ **Postman Collection v2.1**
  - ✅ 导入 Postman 集合（PostmanImporter）
  - ✅ 导出为 Postman 集合（PostmanExporter）
- [ ] **OpenAPI/Swagger**
  - [ ] 导入 OpenAPI 规范
  - [ ] 生成请求集合

**完成时间**: 2025-02-05
**实现内容**:
- PostmanImporter 工具类（解析 v2.1 JSON 格式）
- PostmanExporter 工具类（生成 v2.1 JSON 格式）
- 完整单元测试（28+ 测试用例）
- 支持嵌套文件夹、请求元数据、查询参数、请求体

### Phase 4: 测试和自动化（优先级：中）

#### 4.1 响应测试
- [ ] **Tests 标签页**
  - [ ] 代码编辑器（语法高亮）
  - [ ] 断言库集成
- [ ] **常用断言**
  - [ ] Status code 检查
  - [ ] 响应时间检查
  - [ ] JSON 路径断言
  - [ ] 包含文本检查

**估算时间**: 2-3 周

#### 4.2 Pre-request Script
- [ ] **请求前脚本**
  - [ ] 脚本编辑器
  - [ ] 变量设置
  - [ ] 动态 Headers

**估算时间**: 1-2 周

#### 4.3 批量运行
- [ ] **Collection Runner**
  - [ ] 选择集合运行
  - [ ] 顺序/并行执行
  - [ ] 迭代次数
  - [ ] 测试报告

**估算时间**: 3 周

### Phase 5: 高级功能（优先级：低）

#### 5.1 Cookie 管理
- [ ] **Cookie Jar**
  - [ ] Cookie 管理器 UI
  - [ ] 自动 Cookie 处理
  - [ ] Cookie 持久化

**估算时间**: 2 周

#### 5.2 Proxy 和拦截
- [ ] **代理设置**
  - [ ] HTTP/HTTPS Proxy
  - [ ] 系统代理
- [ ] **请求捕获**
  - [ ] 浏览器请求拦截
  - [ ] 自动生成请求

**估算时间**: 3-4 周

#### 5.3 API 设计
- [ ] **OpenAPI 生成**
  - [ ] 从请求生成 OpenAPI 文档
  - [ ] 导出 YAML/JSON

**估算时间**: 2 周

#### 5.4 Mock 服务器
- [ ] **本地 Mock Server**
  - [ ] 创建 Mock 端点
  - [ ] 返回示例响应
  - [ ] 延迟模拟

**估算时间**: 3-4 周

### Phase 6: 用户体验优化（优先级：中）

#### 6.1 UI 增强
- [ ] **布局定制**
  - [ ] 支持水平/垂直分屏布局切换
  - [ ] 响应面板全屏模式
- [ ] **响应可视化**
  - [ ] JSON 树状视图与节点折叠/展开
  - [ ] HTML 预览
  - [ ] 图片预览
- [ ] **响应时间线**
  - [ ] Waterfall 图表
  - [ ] DNS、TCP、TLS 时间分解

**估算时间**: 3 周

#### 6.2 搜索和过滤
- [ ] **智能补全**
  - [ ] 联动 IDE 索引，根据项目代码自动补全 URL
  - [ ] Header 常用字段补全
- [ ] **高级搜索**
  - [ ] 按状态码过滤
  - [ ] 按响应时间过滤
  - [ ] 按内容搜索
- [ ] **历史管理增强**
  - [ ] 历史分组
  - [ ] 导出历史

**估算时间**: 2 周

### Phase 7: 安全与工程化（优先级：中）

#### 7.1 安全增强
- [ ] **敏感数据保护**
  - [ ] 使用 IntelliJ `PasswordSafe` 存储 Auth Token 和敏感环境变量
  - [ ] 历史记录脱敏显示
- [ ] **证书管理**
  - [ ] 自定义 SSL 证书支持
  - [ ] 忽略 SSL 证书校验开关

**估算时间**: 2 周

#### 7.2 工程化质量
- [ ] **自动化测试**
  - [ ] 引入 IntelliJ UI Test Robot 进行工具窗口交互测试
  - [ ] 性能基准测试（针对大 JSON 响应）

**估算时间**: 2 周

### Phase 8: 企业功能（优先级：低）

#### 8.1 团队协作
- [ ] **同步功能**
  - [ ] 云同步
  - [ ] 团队共享
- [ ] **版本控制**
  - [ ] Git 集成
  - [ ] 变更历史

#### 8.2 CI/CD 集成
- [ ] **命令行工具**
  - [ ] Newman-like CLI
- [ ] **测试报告**
  - [ ] JUnit XML
  - [ ] HTML 报告

---

## 📅 建议实施顺序

### 短期目标（1-2 个月）
1. ✅ **Phase 1.1** - 请求体增强（Raw 编辑器、文件上传）
2. ✅ **Phase 1.2** - 更多认证（OAuth 2.0、API Key）
3. ✅ **Phase 2** - 环境和变量系统

**理由**: 这些是 Postman 最常用的核心功能，能显著提升插件的实用性。

### 中期目标（3-4 个月）
4. ✅ **Phase 3** - 请求集合和导入导出
5. ✅ **Phase 4** - 测试和自动化
6. ✅ **Phase 6.1** - UI 增强

**理由**: 这些功能让插件从"工具"升级为"平台"，支持更复杂的 API 测试场景。

### 长期目标（6 个月+）
7. ✅ **Phase 5** - 高级功能（Cookie、Proxy、Mock）
8. ✅ **Phase 6.2** - 搜索和过滤优化
9. ✅ **Phase 7** - 企业功能

**理由**: 这些是高级功能，适合企业级使用场景。

---

## 🎓 学习资源

### 参考项目
- [Postman Open Source](https://github.com/postmanlabs)
- [Insomnia REST Client](https://github.com/Kong/insomnia)
- [REST Client (VS Code)](https://github.com/Huachao/vscode-restclient)

### 技术文档
- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/welcome.html)
- [OkHttp Documentation](https://square.github.io/okhttp/)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

### Postman 官方文档
- [Postman Learning Center](https://learning.postman.com/)
- [Postman API Network](https://blog.postman.com/)

---

## 📝 总结

**当前状态**: Amateur-Postman 已经实现了 Postman 的**基础核心功能**（请求构建、发送、响应查看、历史记录、cURL 支持）。

**核心差距**:
1. 缺少**环境变量**系统
2. 缺少**请求集合**组织
3. 缺少**测试断言**功能
4. 缺少**文件上传**和**高级认证**

**建议优先级**:
1. **Phase 1** - 增强请求功能（OAuth、文件上传）
2. **Phase 2** - 环境和变量系统
3. **Phase 3** - 请求集合

完成这三个阶段后，插件将覆盖 Postman **80% 的日常使用场景**。
