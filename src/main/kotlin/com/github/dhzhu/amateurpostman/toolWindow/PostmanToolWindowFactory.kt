package com.github.dhzhu.amateurpostman.toolWindow

import com.github.dhzhu.amateurpostman.ui.GrpcEditorPanel
import com.github.dhzhu.amateurpostman.ui.PostmanToolWindowPanel
import com.github.dhzhu.amateurpostman.ui.WebSocketPanel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory

/** Factory for creating the Amateur-Postman tool window */
class PostmanToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // Protocol-level tabs: HTTP | WebSocket | gRPC
        val protocolTabs = JBTabbedPane()

        // ── HTTP tab (existing feature) ───────────────────────────────────
        val httpPanel = PostmanToolWindowPanel(project)
        Disposer.register(toolWindow.disposable, httpPanel)
        protocolTabs.addTab("HTTP", httpPanel.createPanel())

        // ── WebSocket tab (Phase 1 - Protocol Expansion) ───────────────────
        val wsPanel = WebSocketPanel(project)
        Disposer.register(toolWindow.disposable) { wsPanel.dispose() }
        protocolTabs.addTab("WebSocket", wsPanel.createPanel())

        // ── gRPC tab (Phase 3) ────────────────────────────────────────────
        val grpcPanel = GrpcEditorPanel(project)
        Disposer.register(toolWindow.disposable) { grpcPanel.dispose() }
        protocolTabs.addTab("gRPC", grpcPanel.createPanel())

        val content = ContentFactory.getInstance().createContent(protocolTabs, "", false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}
