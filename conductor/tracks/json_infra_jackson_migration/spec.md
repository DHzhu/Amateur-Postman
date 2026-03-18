# Specification - JSON Infrastructure - Full Migration to Jackson

## Background
The current codebase uses `com.google.gson:gson` inconsistently across the project. Gson has performance limitations with large payloads and lacks first-class support for Kotlin's null-safety and default parameters (often using `Unsafe` to bypass constructors).

## Goals
- **Complete Replacement**: Remove `gson` dependency from `build.gradle.kts`.
- **Infrastructure Standardization**: Create a centralized `JsonService` (IntelliJ Project Service) to handle all serialization/deserialization.
- **Performance**: Leverage Jackson's high-performance streaming and data binding.
- **Kotlin Integrity**: Use `jackson-module-kotlin` to ensure non-null fields and default values are respected during deserialization.

## Architecture
- **JsonService**: A singleton service providing access to a pre-configured `ObjectMapper`.
- **Configuration**:
  - `registerModule(KotlinModule())`
  - `SerializationFeature.INDENT_OUTPUT` (for pretty printing)
  - `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES = false` (robustness)
- **Scripting Layer**: Update `pm.response.json()` and other script bindings to use Jackson results or native JS `JSON.parse` exclusively.

## Acceptance Criteria
1. All Gson imports are removed from the project.
2. All unit tests pass, confirming no regression in persistence or response handling.
3. No `null` is assigned to non-nullable Kotlin properties after deserialization.
4. Large JSON responses (>10MB) can be parsed significantly faster.
