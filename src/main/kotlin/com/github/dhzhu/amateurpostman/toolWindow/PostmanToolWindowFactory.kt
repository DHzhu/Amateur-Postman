package com.github.dhzhu.amateurpostman.toolWindow

import com.github.dhzhu.amateurpostman.ui.PostmanToolWindowPanel
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/** Factory for creating the Amateur-Postman tool window */
class PostmanToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = PostmanToolWindowPanel(project)
        val content = ContentFactory.getInstance().createContent(panel.createPanel(), "", false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}
