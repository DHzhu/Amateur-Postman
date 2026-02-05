package com.github.dhzhu.amateurpostman.models

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CollectionModelsTest {

    private lateinit var testCollection: RequestCollection
    private lateinit var testRequest: HttpRequest

    @Before
    fun setup() {
        testRequest = HttpRequest(
            method = HttpMethod.GET,
            url = "https://api.example.com/users",
            headers = mapOf("Content-Type" to "application/json"),
            body = null
        )

        testCollection = RequestCollection(
            id = "collection-1",
            name = "Test API",
            description = "Test API collection",
            items = listOf(
                CollectionItem.Folder(
                    id = "folder-1",
                    name = "Users",
                    children = listOf(
                        CollectionItem.Request(
                            id = "request-1",
                            name = "Get Users",
                            description = "Get all users",
                            request = testRequest,
                            parentId = "folder-1"
                        )
                    ),
                    parentId = null
                ),
                CollectionItem.Request(
                    id = "request-2",
                    name = "Health Check",
                    description = "API health check",
                    request = testRequest,
                    parentId = null
                )
            ),
            createdAt = 1000L,
            modifiedAt = 2000L
        )
    }

    @Test
    fun testCollectionCreation() {
        val collection = RequestCollection.create("My API", "Description")

        assertNotNull(collection.id)
        assertEquals("My API", collection.name)
        assertEquals("Description", collection.description)
        assertTrue(collection.items.isEmpty())
        assertTrue(collection.createdAt > 0)
        assertTrue(collection.modifiedAt > 0)
    }

    @Test
    fun testCollectionGeneratesUniqueIds() {
        val collection1 = RequestCollection.create("API 1")
        val collection2 = RequestCollection.create("API 2")

        assertNotEquals(collection1.id, collection2.id)
    }

    @Test
    fun testWithUpdatedTimestamp() {
        val original = RequestCollection.create("Test")
        Thread.sleep(10) // Ensure time passes
        val updated = original.withUpdatedTimestamp()

        assertTrue(updated.modifiedAt > original.modifiedAt)
        assertEquals(original.id, updated.id)
        assertEquals(original.name, updated.name)
    }

    @Test
    fun testFindItemByIdRequest() {
        val found = testCollection.findItemById("request-1")

        assertNotNull(found)
        assertTrue(found is CollectionItem.Request)
        assertEquals("Get Users", found?.name)
    }

    @Test
    fun testFindItemByIdFolder() {
        val found = testCollection.findItemById("folder-1")

        assertNotNull(found)
        assertTrue(found is CollectionItem.Folder)
        assertEquals("Users", found?.name)
    }

    @Test
    fun testFindItemByIdNested() {
        val found = testCollection.findItemById("request-1")

        assertNotNull(found)
        assertEquals("request-1", found?.id)
    }

    @Test
    fun testFindItemByIdNotFound() {
        val found = testCollection.findItemById("non-existent")

        assertNull(found)
    }

    @Test
    fun testFolderCreation() {
        val folder = CollectionItem.Folder.create("My Folder")

        assertNotNull(folder.id)
        assertEquals("My Folder", folder.name)
        assertTrue(folder.children.isEmpty())
        assertNull(folder.parentId)
    }

    @Test
    fun testFolderCreationWithParent() {
        val folder = CollectionItem.Folder.create("Child", "parent-id")

        assertEquals("parent-id", folder.parentId)
    }

    @Test
    fun testFolderGetTotalItemCount() {
        val folder = CollectionItem.Folder(
            id = "folder-1",
            name = "Root",
            children = listOf(
                CollectionItem.Request(
                    id = "req-1",
                    name = "Request 1",
                    request = testRequest
                ),
                CollectionItem.Folder(
                    id = "folder-2",
                    name = "Subfolder",
                    children = listOf(
                        CollectionItem.Request(
                            id = "req-2",
                            name = "Request 2",
                            request = testRequest
                        )
                    ),
                    parentId = "folder-1"
                )
            )
        )

        // Should count: 1 request + 1 subfolder + 1 nested request = 3 total
        assertEquals(3, folder.getTotalItemCount())
    }

    @Test
    fun testFolderGetTotalItemCountEmpty() {
        val folder = CollectionItem.Folder.create("Empty")
        assertEquals(0, folder.getTotalItemCount())
    }

    @Test
    fun testRequestCreation() {
        val request = CollectionItem.Request.create(
            name = "Test Request",
            request = testRequest,
            description = "Test description"
        )

        assertNotNull(request.id)
        assertEquals("Test Request", request.name)
        assertEquals("Test description", request.description)
        assertEquals(testRequest, request.request)
        assertNull(request.parentId)
    }

    @Test
    fun testRequestGetDisplayName() {
        val request = CollectionItem.Request(
            id = "req-1",
            name = "Get Users",
            description = "Get all users",
            request = testRequest
        )

        val displayName = request.getDisplayName()

        assertTrue(displayName.contains("Get Users"))
        assertTrue(displayName.contains("GET"))
        assertTrue(displayName.contains("https://api.example.com/users"))
    }

    @Test
    fun testRequestGetDisplayNameLongURL() {
        val longUrlRequest = testRequest.copy(
            url = "https://api.example.com/very/long/path/that/exceeds/fifty/characters"
        )

        val request = CollectionItem.Request(
            id = "req-1",
            name = "Test",
            request = longUrlRequest
        )

        val displayName = request.getDisplayName()
        // URL should be truncated to 50 chars
        assertTrue(displayName.length < 100)
    }

    @Test
    fun testSerializableCollectionToCollection() {
        val serializable = SerializableCollection.from(testCollection)
        val converted = serializable.toCollection()

        assertEquals(testCollection.id, converted.id)
        assertEquals(testCollection.name, converted.name)
        assertEquals(testCollection.description, converted.description)
        assertEquals(testCollection.items.size, converted.items.size)
        assertEquals(testCollection.createdAt, converted.createdAt)
        assertEquals(testCollection.modifiedAt, converted.modifiedAt)
    }

    @Test
    fun testSerializableCollectionRoundTrip() {
        // Convert to serializable and back
        val serializable = SerializableCollection.from(testCollection)
        val converted = serializable.toCollection()

        // Find original and converted items
        val originalRequest = testCollection.findItemById("request-1")
        val convertedRequest = converted.findItemById("request-1")

        assertNotNull(originalRequest)
        assertNotNull(convertedRequest)

        if (originalRequest is CollectionItem.Request && convertedRequest is CollectionItem.Request) {
            assertEquals(originalRequest.name, convertedRequest.name)
            assertEquals(originalRequest.description, convertedRequest.description)
            assertEquals(originalRequest.request.url, convertedRequest.request.url)
            assertEquals(originalRequest.request.method, convertedRequest.request.method)
        }
    }

    @Test
    fun testSerializableCollectionItemFolder() {
        val folder = CollectionItem.Folder(
            id = "folder-1",
            name = "Test Folder",
            children = listOf(
                CollectionItem.Request(
                    id = "req-1",
                    name = "Test Request",
                    request = testRequest
                )
            )
        )

        val serializable = SerializableCollectionItem.from(folder)
        val converted = serializable.toCollectionItem()

        assertTrue(converted is CollectionItem.Folder)
        assertEquals(folder.id, converted.id)
        assertEquals(folder.name, converted.name)

        if (converted is CollectionItem.Folder) {
            assertEquals(1, converted.children.size)
            assertTrue(converted.children[0] is CollectionItem.Request)
        }
    }

    @Test
    fun testSerializableCollectionItemRequest() {
        val requestItem = CollectionItem.Request(
            id = "req-1",
            name = "Test Request",
            description = "Description",
            request = testRequest
        )

        val serializable = SerializableCollectionItem.from(requestItem)
        val converted = serializable.toCollectionItem()

        assertTrue(converted is CollectionItem.Request)
        assertEquals(requestItem.id, converted.id)
        assertEquals(requestItem.name, converted.name)

        if (converted is CollectionItem.Request) {
            assertEquals(requestItem.description, converted.description)
            assertEquals(testRequest.url, converted.request.url)
            assertEquals(testRequest.method, converted.request.method)
        }
    }

    @Test
    fun testSerializableHttpRequestConversion() {
        val serializable = SerializableHttpRequest.from(testRequest)
        val converted = serializable.toHttpRequest()

        assertEquals(testRequest.method, converted.method)
        assertEquals(testRequest.url, converted.url)
        assertEquals(testRequest.headers, converted.headers)
        assertEquals(testRequest.body, converted.body)
        assertEquals(testRequest.contentType, converted.contentType)
    }

    @Test
    fun testCollectionStateCreation() {
        val state = CollectionState(
            version = 1,
            collections = listOf(
                SerializableCollection.from(testCollection)
            )
        )

        assertEquals(1, state.version)
        assertEquals(1, state.collections.size)
    }

    @Test
    fun testEmptyCollection() {
        val collection = RequestCollection.create("Empty")
        val found = collection.findItemById("anything")

        assertNull(found)
        assertEquals(0, collection.items.size)
    }

    @Test
    fun testDeepNestingFind() {
        // Create deeply nested structure
        val deepFolder = CollectionItem.Folder(
            id = "level-3",
            name = "Level 3",
            children = listOf(
                CollectionItem.Request(
                    id = "deep-request",
                    name = "Deep Request",
                    request = testRequest
                )
            ),
            parentId = "level-2"
        )

        val midFolder = CollectionItem.Folder(
            id = "level-2",
            name = "Level 2",
            children = listOf(deepFolder),
            parentId = "level-1"
        )

        val topFolder = CollectionItem.Folder(
            id = "level-1",
            name = "Level 1",
            children = listOf(midFolder)
        )

        val collection = RequestCollection(
            id = "test",
            name = "Test",
            items = listOf(topFolder)
        )

        val found = collection.findItemById("deep-request")

        assertNotNull(found)
        assertEquals("deep-request", found?.id)
    }
}
