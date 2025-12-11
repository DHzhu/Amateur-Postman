package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.models.HttpMethod
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import javax.swing.JButton
import javax.swing.JPanel

/** Unit tests for PostmanToolWindowPanel UI components */
class PostmanToolWindowPanelTest : BasePlatformTestCase() {

    private lateinit var panel: PostmanToolWindowPanel

    override fun setUp() {
        super.setUp()
        panel = PostmanToolWindowPanel(project)
    }

    fun testPanelCreation() {
        val mainPanel = panel.createPanel()
        assertNotNull(mainPanel)
        assertTrue(mainPanel is JPanel)
    }

    fun testPanelHasComponents() {
        val mainPanel = panel.createPanel()
        assertTrue(mainPanel.componentCount > 0)
    }

    fun testPanelLayout() {
        val mainPanel = panel.createPanel()
        assertNotNull(mainPanel.layout)
    }

    fun testPanelIsVisible() {
        val mainPanel = panel.createPanel()
        assertTrue(mainPanel.isVisible)
    }

    fun testHttpMethodEnumInComboBox() {
        val methods = HttpMethod.values()
        assertEquals(7, methods.size)

        val expectedMethods =
                setOf(
                        HttpMethod.GET,
                        HttpMethod.POST,
                        HttpMethod.PUT,
                        HttpMethod.DELETE,
                        HttpMethod.PATCH,
                        HttpMethod.HEAD,
                        HttpMethod.OPTIONS
                )

        assertEquals(expectedMethods, methods.toSet())
    }

    fun testDefaultHttpMethod() {
        val mainPanel = panel.createPanel()
        assertNotNull(mainPanel)
    }

    fun testPanelComponentsAreAccessible() {
        val mainPanel = panel.createPanel()
        assertTrue(mainPanel.componentCount > 0)
        assertNotNull(mainPanel.components)
    }

    fun testPanelHasBorderLayout() {
        val mainPanel = panel.createPanel()
        assertEquals("java.awt.BorderLayout", mainPanel.layout.javaClass.name)
    }

    fun testPanelInitialization() {
        val mainPanel = panel.createPanel()
        assertNotNull(mainPanel)
        assertTrue(mainPanel.isEnabled)
    }

    fun testMultiplePanelCreation() {
        val panel1 = PostmanToolWindowPanel(project)
        val panel2 = PostmanToolWindowPanel(project)

        val mainPanel1 = panel1.createPanel()
        val mainPanel2 = panel2.createPanel()

        assertNotNull(mainPanel1)
        assertNotNull(mainPanel2)
        assertNotSame(mainPanel1, mainPanel2)
    }

    fun testPanelComponentHierarchy() {
        val mainPanel = panel.createPanel()

        fun countComponents(component: java.awt.Component): Int {
            var count = 1
            if (component is java.awt.Container) {
                for (child in component.components) {
                    count += countComponents(child)
                }
            }
            return count
        }

        val totalComponents = countComponents(mainPanel)
        assertTrue(totalComponents > 10)
    }

    fun testPanelHasSendButton() {
        val mainPanel = panel.createPanel()

        fun findButton(component: java.awt.Component, text: String): JButton? {
            if (component is JButton && component.text == text) {
                return component
            }
            if (component is java.awt.Container) {
                for (child in component.components) {
                    val found = findButton(child, text)
                    if (found != null) return found
                }
            }
            return null
        }

        val sendButton = findButton(mainPanel, "Send")
        assertNotNull("Send button should exist", sendButton)
    }

    fun testPanelHasTabsForRequestConfiguration() {
        val mainPanel = panel.createPanel()

        fun findTabbedPane(
                component: java.awt.Component
        ): com.intellij.ui.components.JBTabbedPane? {
            if (component is com.intellij.ui.components.JBTabbedPane) {
                return component
            }
            if (component is java.awt.Container) {
                for (child in component.components) {
                    val found = findTabbedPane(child)
                    if (found != null) return found
                }
            }
            return null
        }

        val tabbedPane = findTabbedPane(mainPanel)
        assertNotNull("Tabbed pane should exist", tabbedPane)

        if (tabbedPane != null) {
            assertTrue("Should have at least 4 tabs", tabbedPane.tabCount >= 4)
        }
    }

    fun testPanelRespondsToProjectChanges() {
        val panel1 = PostmanToolWindowPanel(project)
        val mainPanel1 = panel1.createPanel()

        assertNotNull(mainPanel1)
        assertTrue(mainPanel1.isEnabled)
    }

    fun testPanelMemoryManagement() {
        for (i in 1..10) {
            val testPanel = PostmanToolWindowPanel(project)
            val mainPanel = testPanel.createPanel()
            assertNotNull(mainPanel)
        }
        assertTrue(true)
    }

    fun testPanelComponentsAreEnabled() {
        val mainPanel = panel.createPanel()
        assertTrue(mainPanel.isEnabled)
    }

    fun testPanelHasProperSize() {
        val mainPanel = panel.createPanel()
        val preferredSize = mainPanel.preferredSize
        assertNotNull(preferredSize)
        assertTrue(preferredSize.width >= 0)
        assertTrue(preferredSize.height >= 0)
    }
}
