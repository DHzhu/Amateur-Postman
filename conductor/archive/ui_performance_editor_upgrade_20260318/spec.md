# Specification - UI Performance - Migration to IntelliJ Editor

## Background
The current `HighPerfResponseViewer` uses `JTextPane` and `StyledDocument` for syntax highlighting. `JTextPane` is inefficient for large files, lacking virtual scrolling and efficient styling mechanisms, which leads to UI hangs with responses over 5MB.

## Goals
- **Editor Migration**: Replace `JTextPane` with IntelliJ's native `Editor` (com.intellij.openapi.editor.Editor).
- **Virtualization**: Native support for large files with high-performance virtual scrolling.
- **Async Highlighting**: Use IntelliJ's built-in syntax highlighters (JSON, XML, etc.) or implement an asynchronous highlighting mechanism.
- **Robustness**: Support viewing files up to 100MB+ without blocking the UI thread.
- **Features**: Native support for folding, searching, and line numbering.

## Architecture
- **ResponseEditorComponent**: A custom component hosting an IntelliJ `Editor` instance.
- **EditorFactory**: Use `EditorFactory.getInstance().createViewer(Document)` for a read-only viewer.
- **FileType Detection**: Dynamically assign the correct `FileType` (JSON, XML, HTML, PlainText) to the `Document` to enable native highlighting.

## Acceptance Criteria
1. No UI freezing when loading 10MB+ JSON responses.
2. Syntax highlighting is accurate for JSON, XML, and HTML.
3. Native search (Ctrl+F) and folding are functional within the viewer.
4. Large responses (>50MB) display a "Partial View" or high-performance notice if necessary.
