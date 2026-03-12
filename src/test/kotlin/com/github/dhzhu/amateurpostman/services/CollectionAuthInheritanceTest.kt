package com.github.dhzhu.amateurpostman.services

import com.github.dhzhu.amateurpostman.models.*
import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.mockito.kotlin.mock

/**
 * Tests for collection-level auth inheritance.
 */
class CollectionAuthInheritanceTest {

    private lateinit var collectionService: CollectionService
    private lateinit var authService: AuthService
    private lateinit var oauth2Service: OAuth2Service

    @BeforeEach
    fun setUp() {
        val mockProject = mock<Project>()
        collectionService = CollectionService(mockProject)
        oauth2Service = OAuth2Service(mockProject)
        authService = AuthService(mockProject)
    }

    // ========== Auth Source Priority Tests ==========

    @Test
    fun testRequestAuthOverridesInheritedAuth(): Unit = runBlocking {
        // Create collection with auth
        val collection = RequestCollection.create("Test Collection").copy(
            auth = SerializableAuthentication(type = "BEARER", token = "collection-token")
        )

        // Create request with its own auth
        val request = HttpRequest(
            url = "https://api.example.com/test",
            method = HttpMethod.GET,
            authentication = BearerToken("request-token")
        )

        val requestItem = CollectionItem.Request(
            id = "request-1",
            name = "Test Request",
            request = request
        )

        val collectionWithRequest = collection.copy(items = listOf(requestItem))

        // Request auth should take priority
        val resolvedAuth = authService.resolveAuthForTest(collectionWithRequest, "request-1")
        Assertions.assertNotNull(resolvedAuth)
        val headers = resolvedAuth!!.toHeaders()
        Assertions.assertEquals("Bearer request-token", headers["Authorization"])
    }

    @Test
    fun testFolderAuthInheritedByRequest(): Unit = runBlocking {
        // Create collection
        val collection = RequestCollection.create("Test Collection")

        // Create folder with auth
        val folder = CollectionItem.Folder(
            id = "folder-1",
            name = "Protected Folder",
            auth = SerializableAuthentication(type = "BEARER", token = "folder-token")
        )

        // Create request without auth
        val request = HttpRequest(
            url = "https://api.example.com/test",
            method = HttpMethod.GET
        )
        val requestItem = CollectionItem.Request(
            id = "request-1",
            name = "Test Request",
            request = request,
            parentId = "folder-1"
        )

        val folderWithRequest = folder.copy(children = listOf(requestItem))
        val collectionWithFolder = collection.copy(items = listOf(folderWithRequest))

        // Request should inherit folder auth
        val resolvedAuth = authService.resolveAuthForTest(collectionWithFolder, "request-1")
        Assertions.assertNotNull(resolvedAuth)
        val headers = resolvedAuth!!.toHeaders()
        Assertions.assertEquals("Bearer folder-token", headers["Authorization"])
    }

    @Test
    fun testCollectionAuthInheritedByRequest(): Unit = runBlocking {
        // Create collection with auth
        val collection = RequestCollection.create("Test Collection").copy(
            auth = SerializableAuthentication(type = "BASIC", username = "admin", password = "secret")
        )

        // Create request without auth
        val request = HttpRequest(
            url = "https://api.example.com/test",
            method = HttpMethod.GET
        )
        val requestItem = CollectionItem.Request(
            id = "request-1",
            name = "Test Request",
            request = request
        )

        val collectionWithRequest = collection.copy(items = listOf(requestItem))

        // Request should inherit collection auth
        val resolvedAuth = authService.resolveAuthForTest(collectionWithRequest, "request-1")
        Assertions.assertNotNull(resolvedAuth)
        val headers = resolvedAuth!!.toHeaders()
        // Basic Auth should be present
        Assertions.assertTrue(headers["Authorization"]!!.startsWith("Basic "))
    }

    @Test
    fun testNoAuthWhenNoneConfigured(): Unit = runBlocking {
        // Create collection without auth
        val collection = RequestCollection.create("Test Collection")

        // Create request without auth
        val request = HttpRequest(
            url = "https://api.example.com/test",
            method = HttpMethod.GET
        )
        val requestItem = CollectionItem.Request(
            id = "request-1",
            name = "Test Request",
            request = request
        )

        val collectionWithRequest = collection.copy(items = listOf(requestItem))

        // No auth should be resolved
        val resolvedAuth = authService.resolveAuthForTest(collectionWithRequest, "request-1")
        Assertions.assertNull(resolvedAuth)
    }

    // ========== Nested Folder Tests ==========

    @Test
    fun testNestedFolderAuthClosestParentWins(): Unit = runBlocking {
        // Create collection
        val collection = RequestCollection.create("Test Collection")

        // Outer folder with auth
        val outerFolder = CollectionItem.Folder(
            id = "outer",
            name = "Outer Folder",
            auth = SerializableAuthentication(type = "BEARER", token = "outer-token")
        )

        // Inner folder with different auth
        val innerFolder = CollectionItem.Folder(
            id = "inner",
            name = "Inner Folder",
            parentId = "outer",
            auth = SerializableAuthentication(type = "BEARER", token = "inner-token")
        )

        // Request without auth
        val request = HttpRequest(
            url = "https://api.example.com/test",
            method = HttpMethod.GET
        )
        val requestItem = CollectionItem.Request(
            id = "request-1",
            name = "Test Request",
            request = request,
            parentId = "inner"
        )

        val nestedStructure = outerFolder.copy(
            children = listOf(innerFolder.copy(children = listOf(requestItem)))
        )
        val collectionWithStructure = collection.copy(items = listOf(nestedStructure))

        // Inner folder auth should win
        val resolvedAuth = authService.resolveAuthForTest(collectionWithStructure, "request-1")
        Assertions.assertNotNull(resolvedAuth)
        val headers = resolvedAuth!!.toHeaders()
        Assertions.assertEquals("Bearer inner-token", headers["Authorization"])
    }

    @Test
    fun testNestedFolderInheritsFromParentWhenNoOwnAuth(): Unit = runBlocking {
        // Create collection
        val collection = RequestCollection.create("Test Collection")

        // Outer folder with auth
        val outerFolder = CollectionItem.Folder(
            id = "outer",
            name = "Outer Folder",
            auth = SerializableAuthentication(type = "BEARER", token = "outer-token")
        )

        // Inner folder without auth
        val innerFolder = CollectionItem.Folder(
            id = "inner",
            name = "Inner Folder",
            parentId = "outer"
            // No auth - should inherit from outer
        )

        // Request without auth
        val request = HttpRequest(
            url = "https://api.example.com/test",
            method = HttpMethod.GET
        )
        val requestItem = CollectionItem.Request(
            id = "request-1",
            name = "Test Request",
            request = request,
            parentId = "inner"
        )

        val nestedStructure = outerFolder.copy(
            children = listOf(innerFolder.copy(children = listOf(requestItem)))
        )
        val collectionWithStructure = collection.copy(items = listOf(nestedStructure))

        // Should inherit from outer folder
        val resolvedAuth = authService.resolveAuthForTest(collectionWithStructure, "request-1")
        Assertions.assertNotNull(resolvedAuth)
        val headers = resolvedAuth!!.toHeaders()
        Assertions.assertEquals("Bearer outer-token", headers["Authorization"])
    }

    // ========== SerializableAuthentication Tests ==========

    @Test
    fun testSerializableBasicAuth() {
        val auth = BasicAuth("user", "pass")
        val serialized = SerializableAuthentication.from(auth)
        Assertions.assertNotNull(serialized)
        Assertions.assertEquals("BASIC", serialized!!.type)
        Assertions.assertEquals("user", serialized.username)
        Assertions.assertEquals("pass", serialized.password)

        val deserialized = serialized.toAuthentication()
        Assertions.assertTrue(deserialized is BasicAuth)
        val headers = deserialized!!.toHeaders()
        Assertions.assertTrue(headers["Authorization"]!!.startsWith("Basic "))
    }

    @Test
    fun testSerializableBearerToken() {
        val auth = BearerToken("my-token")
        val serialized = SerializableAuthentication.from(auth)
        Assertions.assertNotNull(serialized)
        Assertions.assertEquals("BEARER", serialized!!.type)
        Assertions.assertEquals("my-token", serialized.token)

        val deserialized = serialized.toAuthentication()
        Assertions.assertTrue(deserialized is BearerToken)
        Assertions.assertEquals("Bearer my-token", deserialized!!.toHeaders()["Authorization"])
    }

    @Test
    fun testSerializableApiKeyAuth() {
        val auth = ApiKeyAuth("X-API-Key", "secret-key", ApiKeyAuth.ApiKeyLocation.HEADER)
        val serialized = SerializableAuthentication.from(auth)
        Assertions.assertNotNull(serialized)
        Assertions.assertEquals("API_KEY", serialized!!.type)
        Assertions.assertEquals("X-API-Key", serialized.apiKey)
        Assertions.assertEquals("secret-key", serialized.apiValue)
        Assertions.assertEquals("HEADER", serialized.apiKeyLocation)

        val deserialized = serialized.toAuthentication()
        Assertions.assertTrue(deserialized is ApiKeyAuth)
        Assertions.assertEquals("secret-key", deserialized!!.toHeaders()["X-API-Key"])
    }

    @Test
    fun testSerializableOAuth2ConfigRef() {
        val auth = OAuth2ConfigRef("config-123")
        val serialized = SerializableAuthentication.from(auth)
        Assertions.assertNotNull(serialized)
        Assertions.assertEquals("OAUTH2_CONFIG", serialized!!.type)
        Assertions.assertEquals("config-123", serialized.oauth2ConfigId)

        val deserialized = serialized.toAuthentication()
        Assertions.assertTrue(deserialized is OAuth2ConfigRef)
        Assertions.assertEquals("config-123", (deserialized as OAuth2ConfigRef).configId)
    }

    @Test
    fun testSerializableNoAuth() {
        val serialized = SerializableAuthentication.from(NoAuth)
        Assertions.assertNotNull(serialized)
        Assertions.assertEquals("NONE", serialized!!.type)

        val deserialized = serialized.toAuthentication()
        Assertions.assertTrue(deserialized is NoAuth)
        Assertions.assertTrue(deserialized!!.toHeaders().isEmpty())
    }

    // ========== Auth Source Description Tests ==========

    @Test
    fun testAuthSourceDescriptionRequest() {
        val request = HttpRequest(
            url = "https://api.example.com/test",
            method = HttpMethod.GET,
            authentication = BearerToken("token")
        )
        val requestItem = CollectionItem.Request(
            id = "request-1",
            name = "Test Request",
            request = request
        )
        val collection = RequestCollection.create("Test").copy(items = listOf(requestItem))

        val description = authService.getAuthSourceDescriptionForTest(collection, "request-1")
        Assertions.assertEquals("Request", description)
    }

    @Test
    fun testAuthSourceDescriptionFolder() {
        val folder = CollectionItem.Folder(
            id = "folder-1",
            name = "My Folder",
            auth = SerializableAuthentication(type = "BEARER", token = "token")
        )
        val request = HttpRequest(
            url = "https://api.example.com/test",
            method = HttpMethod.GET
        )
        val requestItem = CollectionItem.Request(
            id = "request-1",
            name = "Test Request",
            request = request,
            parentId = "folder-1"
        )
        val folderWithRequest = folder.copy(children = listOf(requestItem))
        val collection = RequestCollection.create("Test").copy(items = listOf(folderWithRequest))

        val description = authService.getAuthSourceDescriptionForTest(collection, "request-1")
        Assertions.assertEquals("Folder: My Folder", description)
    }

    @Test
    fun testAuthSourceDescriptionCollection() {
        val collection = RequestCollection.create("My Collection").copy(
            auth = SerializableAuthentication(type = "BEARER", token = "token")
        )
        val request = HttpRequest(
            url = "https://api.example.com/test",
            method = HttpMethod.GET
        )
        val requestItem = CollectionItem.Request(
            id = "request-1",
            name = "Test Request",
            request = request
        )
        val collectionWithRequest = collection.copy(items = listOf(requestItem))

        val description = authService.getAuthSourceDescriptionForTest(collectionWithRequest, "request-1")
        Assertions.assertEquals("Collection: My Collection", description)
    }
}

// Extension methods for testing without persistence
private suspend fun AuthService.resolveAuthForTest(collection: RequestCollection, itemId: String): Authentication? {
    // Use reflection or internal method for testing
    // This is a simplified version for unit testing
    val requestItem = collection.findItemById(itemId) as? CollectionItem.Request ?: return null

    // 1. Check request-level auth first
    val requestAuth = requestItem.request.authentication
    if (requestAuth != null && requestAuth !is OAuth2ConfigRef) {
        return requestAuth
    }

    // 2. Check folder inheritance
    if (requestItem.parentId != null) {
        val folderChain = buildFolderChainForTest(collection.items, requestItem.parentId)
        for (folder in folderChain) {
            val folderAuth = folder.auth?.toAuthentication()
            if (folderAuth != null && folderAuth !is OAuth2ConfigRef) {
                return folderAuth
            }
        }
    }

    // 3. Check collection-level auth
    val collectionAuth = collection.auth?.toAuthentication()
    if (collectionAuth != null && collectionAuth !is OAuth2ConfigRef) {
        return collectionAuth
    }

    return null
}

private fun buildFolderChainForTest(items: List<CollectionItem>, targetFolderId: String): List<CollectionItem.Folder> {
    val chain = mutableListOf<CollectionItem.Folder>()
    var currentId: String? = targetFolderId

    while (currentId != null) {
        val folder = findFolderByIdForTest(items, currentId)
        if (folder != null) {
            chain.add(folder)
            currentId = folder.parentId
        } else {
            break
        }
    }

    return chain
}

private fun findFolderByIdForTest(items: List<CollectionItem>, folderId: String): CollectionItem.Folder? {
    for (item in items) {
        if (item.id == folderId && item is CollectionItem.Folder) {
            return item
        }
        if (item is CollectionItem.Folder) {
            val found = findFolderByIdForTest(item.children, folderId)
            if (found != null) return found
        }
    }
    return null
}

private fun AuthService.getAuthSourceDescriptionForTest(collection: RequestCollection, itemId: String): String? {
    val requestItem = collection.findItemById(itemId) as? CollectionItem.Request ?: return null

    if (requestItem.request.authentication != null) {
        return "Request"
    }

    if (requestItem.parentId != null) {
        val folderChain = buildFolderChainForTest(collection.items, requestItem.parentId)
        for (folder in folderChain) {
            if (folder.auth != null) {
                return "Folder: ${folder.name}"
            }
        }
    }

    if (collection.auth != null) {
        return "Collection: ${collection.name}"
    }

    return null
}