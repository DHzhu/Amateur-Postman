# Specification - Phase 1: Request Enhancement

## Goal
增强 HTTP 请求的构建能力，支持更复杂的请求场景，包括多种 Body 类型、文件上传和 GraphQL。

## Requirements

### 1. Raw Editor Enhancement
- 支持多种 Content-Type 切换：Text, JSON, XML, HTML, JavaScript。
- 根据选择的类型自动更新请求头的 `Content-Type`。
- 提供对应类型的语法高亮和基础格式化功能。

### 2. Multipart/form-data Support
- 支持文件上传。
- 提供 Key-Value 表格，Value 可以选择为“文本”或“文件”。
- 集成 IntelliJ 文件选择器。

### 3. GraphQL Support
- 提供专用的 GraphQL 编辑器。
- 支持定义 Query 和 Variables。
- 能够处理 JSON 格式的 GraphQL 响应。

## Proposed Changes
- 修改 `HttpModels.kt` 增加 Body 类型支持。
- 更新 `PostmanToolWindowPanel` 和 `HttpRequestService` 以处理 Multipart 请求。
- 集成 IntelliJ 内置的语言支持进行语法高亮。
