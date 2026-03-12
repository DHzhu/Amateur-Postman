package com.github.dhzhu.amateurpostman.codeinsight

import com.github.dhzhu.amateurpostman.models.HttpBody
import com.github.dhzhu.amateurpostman.models.HttpMethod
import com.github.dhzhu.amateurpostman.models.HttpRequest
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifierList
import javax.swing.Icon

/**
 * LineMarkerProvider for Spring Boot and JAX-RS Controller methods.
 *
 * Displays a gutter icon next to controller methods that allows
 * jumping to Amateur-Postman to send a request.
 */
class AmControllerLineMarkerProvider : LineMarkerProvider {

    companion object {
        // Spring Boot annotations
        private val SPRING_CONTROLLER_ANNOTATIONS = setOf(
            "org.springframework.web.bind.annotation.RestController",
            "org.springframework.web.bind.annotation.Controller"
        )

        private val SPRING_MAPPING_ANNOTATIONS = mapOf(
            "org.springframework.web.bind.annotation.RequestMapping" to null, // Check method attribute
            "org.springframework.web.bind.annotation.GetMapping" to HttpMethod.GET,
            "org.springframework.web.bind.annotation.PostMapping" to HttpMethod.POST,
            "org.springframework.web.bind.annotation.PutMapping" to HttpMethod.PUT,
            "org.springframework.web.bind.annotation.DeleteMapping" to HttpMethod.DELETE,
            "org.springframework.web.bind.annotation.PatchMapping" to HttpMethod.PATCH
        )

        // JAX-RS annotations
        private val JAXRS_PATH_ANNOTATIONS = setOf(
            "javax.ws.rs.Path",
            "jakarta.ws.rs.Path"
        )

        private val JAXRS_METHOD_ANNOTATIONS = mapOf(
            "javax.ws.rs.GET" to HttpMethod.GET,
            "javax.ws.rs.POST" to HttpMethod.POST,
            "javax.ws.rs.PUT" to HttpMethod.PUT,
            "javax.ws.rs.DELETE" to HttpMethod.DELETE,
            "javax.ws.rs.PATCH" to HttpMethod.PATCH,
            "javax.ws.rs.HEAD" to HttpMethod.HEAD,
            "javax.ws.rs.OPTIONS" to HttpMethod.OPTIONS,
            "jakarta.ws.rs.GET" to HttpMethod.GET,
            "jakarta.ws.rs.POST" to HttpMethod.POST,
            "jakarta.ws.rs.PUT" to HttpMethod.PUT,
            "jakarta.ws.rs.DELETE" to HttpMethod.DELETE,
            "jakarta.ws.rs.PATCH" to HttpMethod.PATCH,
            "jakarta.ws.rs.HEAD" to HttpMethod.HEAD,
            "jakarta.ws.rs.OPTIONS" to HttpMethod.OPTIONS
        )
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        // Only process method identifiers
        if (element !is PsiIdentifier) return null

        val parent = element.parent
        if (parent !is PsiMethod) return null

        val method = parent

        // Check if the class is a controller
        if (!isControllerClass(method.containingClass)) return null

        // Extract mapping info
        val mappingInfo = extractMappingInfo(method) ?: return null

        // Create line marker
        val icon = getIconForMethod(mappingInfo.method)
        val tooltip = "${mappingInfo.method.name} ${mappingInfo.path}\nClick to send in Amateur-Postman"

        return LineMarkerInfo(
            element,
            element.textRange,
            icon,
            { tooltip },
            { _, _ ->
                handleNavigation(element.project, mappingInfo)
            },
            GutterIconRenderer.Alignment.RIGHT,
            { "Amateur-Postman: ${mappingInfo.method.name}" }
        )
    }

    /**
     * Checks if the class has a controller annotation.
     */
    private fun isControllerClass(psiClass: PsiClass?): Boolean {
        if (psiClass == null) return false

        val modifierList = psiClass.modifierList ?: return false
        return modifierList.annotations.any { annotation ->
            val qualifiedName = annotation.qualifiedName ?: return@any false
            SPRING_CONTROLLER_ANNOTATIONS.contains(qualifiedName) ||
            JAXRS_PATH_ANNOTATIONS.contains(qualifiedName)
        }
    }

    /**
     * Extracts HTTP method and path from method annotations.
     */
    private fun extractMappingInfo(method: PsiMethod): MappingInfo? {
        val modifierList = method.modifierList ?: return null

        for (annotation in modifierList.annotations) {
            val qualifiedName = annotation.qualifiedName ?: continue

            // Spring annotations
            if (SPRING_MAPPING_ANNOTATIONS.containsKey(qualifiedName)) {
                val httpMethod = SPRING_MAPPING_ANNOTATIONS[qualifiedName]
                    ?: extractMethodFromRequestMapping(annotation)
                val path = extractPathFromSpringAnnotation(annotation)

                // Get class-level path
                val classPath = extractClassLevelPath(method.containingClass)
                val fullPath = combinePaths(classPath, path)

                return MappingInfo(httpMethod, fullPath)
            }

            // JAX-RS annotations
            if (JAXRS_METHOD_ANNOTATIONS.containsKey(qualifiedName)) {
                val httpMethod = JAXRS_METHOD_ANNOTATIONS[qualifiedName] ?: continue
                val methodPath = extractJaxRsPath(method)
                val classPath = extractJaxRsClassPath(method.containingClass)
                val fullPath = combinePaths(classPath, methodPath)

                return MappingInfo(httpMethod, fullPath)
            }
        }

        return null
    }

    /**
     * Extracts HTTP method from @RequestMapping annotation.
     */
    private fun extractMethodFromRequestMapping(annotation: com.intellij.psi.PsiAnnotation): HttpMethod {
        val methodAttr = annotation.findAttributeValue("method")?.text ?: return HttpMethod.GET
        return when {
            methodAttr.contains("GET") -> HttpMethod.GET
            methodAttr.contains("POST") -> HttpMethod.POST
            methodAttr.contains("PUT") -> HttpMethod.PUT
            methodAttr.contains("DELETE") -> HttpMethod.DELETE
            methodAttr.contains("PATCH") -> HttpMethod.PATCH
            else -> HttpMethod.GET
        }
    }

    /**
     * Extracts path from Spring mapping annotation.
     */
    private fun extractPathFromSpringAnnotation(annotation: com.intellij.psi.PsiAnnotation): String {
        // Try 'value' first, then 'path'
        val valueAttr = annotation.findAttributeValue("value")
            ?: annotation.findAttributeValue("path")
            ?: return "/"

        return extractPathFromValue(valueAttr.text)
    }

    /**
     * Extracts class-level path from @RequestMapping.
     */
    private fun extractClassLevelPath(psiClass: PsiClass?): String {
        if (psiClass == null) return ""

        val modifierList = psiClass.modifierList ?: return ""
        for (annotation in modifierList.annotations) {
            if (annotation.qualifiedName == "org.springframework.web.bind.annotation.RequestMapping") {
                return extractPathFromSpringAnnotation(annotation)
            }
        }

        return ""
    }

    /**
     * Extracts JAX-RS path from method.
     */
    private fun extractJaxRsPath(method: PsiMethod): String {
        val modifierList = method.modifierList ?: return "/"
        for (annotation in modifierList.annotations) {
            if (JAXRS_PATH_ANNOTATIONS.contains(annotation.qualifiedName)) {
                val valueAttr = annotation.findAttributeValue("value") ?: return "/"
                return extractPathFromValue(valueAttr.text)
            }
        }
        return "/"
    }

    /**
     * Extracts JAX-RS class-level path.
     */
    private fun extractJaxRsClassPath(psiClass: PsiClass?): String {
        if (psiClass == null) return ""

        val modifierList = psiClass.modifierList ?: return ""
        for (annotation in modifierList.annotations) {
            if (JAXRS_PATH_ANNOTATIONS.contains(annotation.qualifiedName)) {
                val valueAttr = annotation.findAttributeValue("value") ?: return ""
                return extractPathFromValue(valueAttr.text)
            }
        }

        return ""
    }

    /**
     * Combines class and method paths.
     */
    private fun combinePaths(classPath: String, methodPath: String): String {
        val normalizedClass = classPath.trimEnd('/')
        val normalizedMethod = if (methodPath.startsWith("/")) methodPath else "/$methodPath"

        return if (normalizedClass.isEmpty()) {
            normalizedMethod.ifEmpty { "/" }
        } else {
            "$normalizedClass$normalizedMethod"
        }
    }

    /**
     * Extracts path from annotation value text.
     */
    private fun extractPathFromValue(text: String): String {
        // Handle array format: {"/path1", "/path2"} - take first
        if (text.startsWith("{")) {
            val paths = text.removeSurrounding("{", "}")
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
            val path = paths.firstOrNull() ?: return "/"
            return if (path.startsWith("/")) path else "/$path"
        }

        // Single value
        val path = text.removeSurrounding("\"")
        return if (path.startsWith("/")) path else "/$path"
    }

    /**
     * Gets icon for HTTP method.
     */
    private fun getIconForMethod(method: HttpMethod): Icon {
        return when (method) {
            HttpMethod.GET -> AllIcons.Actions.Find
            HttpMethod.POST -> AllIcons.General.Add
            HttpMethod.PUT -> AllIcons.Actions.Edit
            HttpMethod.DELETE -> AllIcons.Actions.Cancel
            HttpMethod.PATCH -> AllIcons.Actions.Edit
            else -> AllIcons.Actions.Execute
        }
    }

    /**
     * Handles navigation to Amateur-Postman.
     */
    private fun handleNavigation(project: Project, mappingInfo: MappingInfo) {
        // Open Amateur-Postman tool window
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow("Amateur-Postman")

        toolWindow?.activate {
            // Create and publish request
            val httpRequest = HttpRequest(
                url = mappingInfo.path,
                method = mappingInfo.method,
                headers = mapOf("Content-Type" to "application/json"),
                body = null
            )

            // Notify listeners
            project.messageBus
                .syncPublisher(ControllerRequestTopic.REQUEST)
                .onRequestReady(httpRequest, "${mappingInfo.method.name} ${mappingInfo.path}")
        }
    }

    /**
     * Information extracted from a controller method.
     */
    data class MappingInfo(
        val method: HttpMethod,
        val path: String
    )
}

/**
 * Topic for controller-to-toolwindow communication.
 * Used when clicking gutter icons on controller methods.
 */
object ControllerRequestTopic {
    val REQUEST: com.intellij.util.messages.Topic<ControllerRequestListener> =
        com.intellij.util.messages.Topic.create(
            "Amateur-Postman.ControllerRequest",
            ControllerRequestListener::class.java
        )

    interface ControllerRequestListener {
        fun onRequestReady(request: HttpRequest, name: String)
    }
}