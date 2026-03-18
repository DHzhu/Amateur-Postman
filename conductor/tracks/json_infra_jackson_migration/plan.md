# Implementation Plan - JSON Infrastructure - Full Migration to Jackson

## Phase 1: Infrastructure & Dependencies ✅ DONE
- [x] Task: Add `jackson-databind` and `jackson-module-kotlin` dependencies to `build.gradle.kts`. <!-- a6f1c91 -->
- [x] Task: Create `JsonService.kt` (Project Service) that initializes a singleton `ObjectMapper` with Kotlin support and indentation. <!-- a6f1c91 -->
- [x] Task: Add basic unit tests for `JsonService` to verify Kotlin's null-safety handling and default values. <!-- a6f1c91 -->

## Phase 2: Serialization/Deserialization Refactoring ✅ DONE
- [x] Task: Refactor `HttpModels.kt`, `CollectionModels.kt`, and `EnvironmentModels.kt` to use Jackson annotations where needed (e.g., `@JsonProperty`). <!-- c94a331 -->
- [x] Task: Replace all `Gson().fromJson` and `Gson().toJson` calls in `PostmanImporter.kt`, `PostmanExporter.kt`, and `HarParser.kt` with `JsonService`. <!-- c94a331 -->
- [x] Task: Update `HttpRequestServiceImpl.kt` response body parsing logic to use Jackson. <!-- c94a331 -->

## Phase 3: Utility & UI Integration ✅ DONE
- [x] Task: Update `SyntaxHighlighter.kt`'s `formatJson` to use Jackson's pretty-printing. <!-- c94a331 -->
- [x] Task: Refactor `OpenApiImporter.kt` and `OpenApiExporter` to use Jackson instead of Gson for intermediate representation. <!-- c94a331 -->
- [x] Task: Update `PostmanToolWindowPanel.kt` to use `JsonService` for variable visualization. <!-- c94a331 -->

## Phase 4: Scripting Layer Alignment ✅ DONE
- [x] Task: Update `ScriptExecutionService.kt`'s `PmBinding` to ensure `pm.response.json()` returns native JS objects via `JSON.parse` (matching Postman's behavior) and uses Jackson for other serialization. <!-- c94a331 -->
- [x] Task: Update `JsonToSchemaConverter.kt` to replace `JsonElement` types with Jackson's `JsonNode`. <!-- c94a331 -->

## Phase 5: Cleanup & Verification ✅ DONE
- [x] Task: Remove `com.google.code.gson:gson` dependency from `build.gradle.kts`. <!-- c94a331 -->
- [x] Task: Run all project unit tests (330+) to ensure no regressions in behavior. <!-- c94a331 -->
- [x] Task: Verify that large JSON files (>5MB) are handled correctly and efficiently. <!-- c94a331 -->
- [x] Task: Conductor - Final Audit & Documentation. <!-- c94a331 -->
