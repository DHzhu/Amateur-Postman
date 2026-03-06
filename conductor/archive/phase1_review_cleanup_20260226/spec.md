# Specification - Phase 1 Review Cleanup

## Background
在 Phase 1 的代码审查中，发现了两处可以改进的地方：
1. 冗余的全路径类引用。
2. 极其基础的 XML 格式化逻辑。

## Requirements
1. **Import Optimization**:
   - 在 `PostmanToolWindowPanel.kt` 中引入 `import com.github.dhzhu.amateurpostman.models.GraphQLRequest`。
   - 替换所有 `com.github.dhzhu.amateurpostman.models.GraphQLRequest.fromJson` 为 `GraphQLRequest.fromJson`。

2. **XML Formatter Improvement**:
   - 评估目前的 `formatXml` 是否能满足基本需求。
   - 如果不满足，考虑添加更严谨的边界处理，或在代码注释中明确其 "Basic implementation" 的局限性。

## Acceptance Criteria
- [ ] 代码中不再出现 `GraphQLRequest` 的全路径调用。
- [ ] XML 格式化逻辑被记录或得到轻微改进。
- [ ] 所有自动化测试通过。
