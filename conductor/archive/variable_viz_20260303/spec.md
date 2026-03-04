# Specification: Variable Scopes Visualization

## 目标
解决用户在使用多级变量时由于作用域覆盖（Shadowing）而产生的困惑。通过一个常驻或悬浮的小窗口，显示当前选定环境下的所有变量解析结果及其来源。

## 关键功能定义

### 1. Environment Quick Look Panel
- **入口**: 在 `EnvironmentPanel` 或主请求面板右上角增加一个“小眼睛”图标。
- **UI 形式**: 一个悬浮窗（Balloon 或 JBPopup）显示分类列表。
- **展示内容**:
    - **Global Variables**: 显示所有全局变量，被覆盖的变量显示删除线。
    - **Environment Variables**: 显示当前环境的变量，高优先级显示。
    - **Collection Variables**: 显示所属集合的变量。
    - **Calculated Value**: 显示最终解析出来的值。

### 2. 实时的变量冲突检测
- **功能**: 当一个环境变量被 Pre-request 脚本动态设置时，Quick Look 窗口应实时感知并标记变量的来源为 "Temporary"。

## 验收标准
- 悬浮窗中能清晰看到同名变量在不同作用域的优先级关系。
- 动态变量（如 $timestamp）在 Quick Look 中不应显示为常量。
