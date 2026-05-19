# Spec: 全面代码审查修复

## 背景

升级至 IntelliJ 2025.3 平台后，项目需要全面代码审查以发现潜在的质量问题。通过 4 个并行审查任务（UI 层、Services/网络层、Models/Actions 层、构建配置/测试）发现共 29 项问题。

## 审查范围

- UI 层：24 个文件（ui/ + toolWindow/）
- Services 层：18 个文件 + SimpleHttpServer
- Models/Actions/Enums/Listeners 层
- 构建配置与测试代码（约 45+ 测试文件）

## 技术策略

### 严重问题修复策略

1. **SimpleHttpServer Content-Length OOM** — 在 parseRequest 中增加 MAX_BODY_SIZE (1MB) 上界检查，readLine 增加 MAX_HEADER_LINE_LENGTH (8KB) 限制
2. **formatXml 数组越界** — 在访问 `xml[i+1]` 前增加 `i+1 < xml.length` 边界检查
3. **VariablesTableModel 类型转换** — 使用安全转换 `value as? String ?: ""`
4. **GrpcStreamingService 线程安全** — 引入 synchronized 和 AtomicInteger
5. **OAuth2Service OkHttpClient 泄漏** — 实现 Disposable 接口
6. **GrpcStreamingServiceTest JUnit 4** — 替换为 JUnit 5 API
7. **MockServerManager Thread.sleep** — 评估是否需要协程化改造或保留（低优先级）
8. **AuthService runBlocking** — 评估调用链，标记为已知风险或重构

### 中等问题修复策略

9-15: 资源泄漏统一实现 Disposable；JSON/XSS 使用 Jackson 序列化和 HTML 转义
16-20: 反序列化增加 try-catch；异常捕获缩窄；监听器改 CopyOnWriteArrayList
21-23: 测试 flaky 问题使用 CountDownLatch 替代 Thread.sleep；OkHttp 版本纳入 version catalog

### 轻微问题修复策略

24-29: var 字段加注释说明；共享 OkHttpClient；Plugin Verifier 增加 Community 版本

## 约束

- 不引入新依赖
- 保持 API 兼容性
- 每项修复后编译通过 + 测试通过
