# Implementation Plan - JSON Infrastructure - Full Migration to Jackson

## Phase 1: Infrastructure & Dependencies ✅ DONE
- [x] Task: Add `jackson-databind` and `jackson-module-kotlin` dependencies to `build.gradle.kts`.
- [x] Task: Create `JsonService.kt` (Project Service) that initializes a singleton `ObjectMapper` with Kotlin support and indentation.
- [x] Task: Add basic unit tests for `JsonService` to verify Kotlin's null-safety handling and default values.

## Phase 2: Serialization/Deserialization Refactoring ✅ PLANNED
- [ ] Task: Refactor `HttpModels.kt`, `CollectionModels.kt`, and `EnvironmentModels.kt` to use Jackson annotations where needed (e.g., `@JsonProperty`).
- [ ] Task: Replace all `Gson().fromJson` and `Gson().toJson` calls in `PostmanImporter.kt`, `PostmanExporter.kt`, and `HarParser.kt` with `JsonService`.
- [ ] Task: Update `HttpRequestServiceImpl.kt` response body parsing logic to use Jackson.

## Phase 3: Utility & UI Integration ✅ PLANNED
- [ ] Task: Update `SyntaxHighlighter.kt`'s `formatJson` to use Jackson's pretty-printing.
- [ ] Task: Refactor `OpenApiImporter.kt` and `OpenApiExporter` to use Jackson instead of Gson for intermediate representation.
- [ ] Task: Update `PostmanToolWindowPanel.kt` to use `JsonService` for variable visualization.

## Phase 4: Scripting Layer Alignment ✅ PLANNED
- [ ] Task: Update `ScriptExecutionService.kt`'s `PmBinding` to ensure `pm.response.json()` returns native JS objects via `JSON.parse` (matching Postman's behavior) and uses Jackson for other serialization.
- [ ] Task: Update `JsonToSchemaConverter.kt` to replace `JsonElement` types with Jackson's `JsonNode`.

## Phase 5: Cleanup & Verification ✅ PLANNED
- [ ] Task: Remove `com.google.code.gson:gson` dependency from `build.gradle.kts`.
- [ ] Task: Run all project unit tests (330+) to ensure no regressions in behavior.
- [ ] Task: Verify that large JSON files (>5MB) are handled correctly and efficiently.
- [ ] Task: Conductor - Final Audit & Documentation.
