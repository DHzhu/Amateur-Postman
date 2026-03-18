# Implementation Plan - UI Performance - Migration to IntelliJ Editor

## Phase 1: Editor Prototyping ✅ PLANNED
- [ ] Task: Research and prototype `EditorFactory.createViewer` within a ToolWindow context.
- [ ] Task: Create `ResponseEditorComponent.kt` as a wrapper for the IntelliJ `Editor` instance.
- [ ] Task: Implement proper resource management (Disposable) to ensure `Editor` instances are correctly released.

## Phase 2: HighPerfResponseViewer Refactoring ✅ PLANNED
- [ ] Task: Replace `JTextPane` with `ResponseEditorComponent` in `HighPerfResponseViewer.kt`.
- [ ] Task: Implement automatic `FileType` detection (JSON, XML, HTML) based on the `Content-Type` header.
- [ ] Task: Support "Pretty Printing" for JSON and XML before rendering in the editor.

## Phase 3: Large Payload Optimization ✅ PLANNED
- [ ] Task: Benchmarking 10MB, 50MB, and 100MB JSON responses with the new `Editor` component.
- [ ] Task: Implement a "Partial Rendering" or "Truncation" mechanism if necessary for extremely large bodies.
- [ ] Task: Ensure search (Ctrl+F) and folding are enabled in the read-only editor.

## Phase 4: Styling & UX Refinement ✅ PLANNED
- [ ] Task: Ensure the editor's theme (Dark/Light) correctly adapts to IntelliJ's global theme settings.
- [ ] Task: Implement a "Copy to Clipboard" and "Save to File" button in the response UI.
- [ ] Task: Cleanup `SyntaxHighlighter.kt` (deprecate current `JTextPane` highlighters).

## Phase 5: Verification & Audit ✅ PLANNED
- [ ] Task: Run integration tests for the response viewer UI.
- [ ] Task: Manually verify the UI with real-world large JSON responses.
- [ ] Task: Conductor - Final Audit & Documentation.
