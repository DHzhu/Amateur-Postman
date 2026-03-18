# Implementation Plan - UI Performance - Migration to IntelliJ Editor

## Phase 1: Editor Prototyping ✅ PLANNED
- [x] Task: Research and prototype `EditorFactory.createViewer` within a ToolWindow context. 462151c
- [x] Task: Create `ResponseEditorComponent.kt` as a wrapper for the IntelliJ `Editor` instance. 462151c
- [x] Task: Implement proper resource management (Disposable) to ensure `Editor` instances are correctly released. 462151c

## Phase 2: HighPerfResponseViewer Refactoring ✅ PLANNED
- [x] Task: Replace `JTextPane` with `ResponseEditorComponent` in `HighPerfResponseViewer.kt`. 462151c
- [x] Task: Implement automatic `FileType` detection (JSON, XML, HTML) based on the `Content-Type` header. 462151c
- [x] Task: Support "Pretty Printing" for JSON and XML before rendering in the editor. 462151c

## Phase 3: Large Payload Optimization ✅ PLANNED
- [x] Task: Benchmarking 10MB, 50MB, and 100MB JSON responses with the new `Editor` component. 462151c
- [x] Task: Implement a "Partial Rendering" or "Truncation" mechanism if necessary for extremely large bodies. 462151c
- [x] Task: Ensure search (Ctrl+F) and folding are enabled in the read-only editor. 462151c

## Phase 4: Styling & UX Refinement ✅ PLANNED
- [x] Task: Ensure the editor's theme (Dark/Light) correctly adapts to IntelliJ's global theme settings. 462151c
- [x] Task: Implement a "Copy to Clipboard" and "Save to File" button in the response UI. 462151c
- [x] Task: Cleanup `SyntaxHighlighter.kt` (deprecate current `JTextPane` highlighters). 462151c

## Phase 5: Verification & Audit ✅ PLANNED
- [x] Task: Run integration tests for the response viewer UI. 462151c
- [x] Task: Manually verify the UI with real-world large JSON responses. 462151c
- [x] Task: Conductor - Final Audit & Documentation. 462151c
