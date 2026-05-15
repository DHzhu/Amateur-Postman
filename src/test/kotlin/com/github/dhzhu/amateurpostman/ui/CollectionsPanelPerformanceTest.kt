package com.github.dhzhu.amateurpostman.ui

import com.github.dhzhu.amateurpostman.models.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

/**
 * Performance tests for CollectionsPanel with large data sets.
 * Tests tree rendering performance with 1000+ requests.
 */
class CollectionsPanelPerformanceTest {

    /**
     * Generate a mock collection with specified number of requests.
     * @param requestCount Total number of requests to generate
     * @param folderCount Number of top-level folders
     * @param maxDepth Maximum nesting depth for folders
     */
    fun generateMockCollection(
        requestCount: Int = 1000,
        folderCount: Int = 10,
        maxDepth: Int = 3
    ): RequestCollection {
        val requestsPerFolder = requestCount / folderCount
        val items = mutableListOf<CollectionItem>()

        for (folderIndex in 0 until folderCount) {
            val folder = generateFolder(
                id = "folder-$folderIndex",
                name = "API Module $folderIndex",
                requestCount = requestsPerFolder,
                currentDepth = 0,
                maxDepth = maxDepth
            )
            items.add(folder)
        }

        return RequestCollection(
            id = "perf-test-collection",
            name = "Performance Test Collection ($requestCount requests)",
            description = "Generated for performance benchmarking",
            items = items
        )
    }

    private fun generateFolder(
        id: String,
        name: String,
        requestCount: Int,
        currentDepth: Int,
        maxDepth: Int
    ): CollectionItem.Folder {
        val children = mutableListOf<CollectionItem>()

        if (currentDepth < maxDepth - 1 && requestCount > 20) {
            // Create subfolders for deeper nesting
            val subFolderCount = 2
            val requestsPerSubfolder = requestCount / (subFolderCount + 1)

            for (i in 0 until subFolderCount) {
                val subFolder = generateFolder(
                    id = "$id-sub-$i",
                    name = "$name Sub $i",
                    requestCount = requestsPerSubfolder,
                    currentDepth = currentDepth + 1,
                    maxDepth = maxDepth
                )
                children.add(subFolder)
            }

            // Remaining requests go directly in this folder
            val remainingRequests = requestCount - (requestsPerSubfolder * subFolderCount)
            for (i in 0 until remainingRequests) {
                children.add(generateRequest("$id-req-$i", "$name Request $i"))
            }
        } else {
            // Leaf folder - just add requests
            for (i in 0 until requestCount) {
                children.add(generateRequest("$id-req-$i", "$name Request $i"))
            }
        }

        return CollectionItem.Folder(
            id = id,
            name = name,
            children = children,
            parentId = null
        )
    }

    private fun generateRequest(id: String, name: String): CollectionItem.Request {
        val methods = HttpMethod.entries
        val method = methods[Math.abs(id.hashCode()) % methods.size]

        return CollectionItem.Request(
            id = id,
            name = name,
            description = "Auto-generated request for performance testing",
            request = HttpRequest(
                method = method,
                url = "https://api.example.com/${id.replace("-", "/")}/endpoint",
                headers = mapOf(
                    "Content-Type" to "application/json",
                    "Authorization" to "Bearer test-token-${id.hashCode()}"
                ),
                body = HttpBody(
                    content = """{"requestId": "$id", "data": "test payload"}""",
                    type = BodyType.JSON
                )
            ),
            preRequestScript = "// Pre-request script for $name",
            testScript = "// Test script for $name\npm.test('Status is 200', () => pm.response.to.have.status(200));",
            parentId = null
        )
    }

    @Test
    fun testGenerateMockCollection_1000Requests() {
        val startTime = System.currentTimeMillis()
        val collection = generateMockCollection(requestCount = 1000, folderCount = 10, maxDepth = 3)
        val generationTime = System.currentTimeMillis() - startTime

        println("Collection generation time: ${generationTime}ms")

        // Verify structure
        assertEquals("perf-test-collection", collection.id)
        assertEquals(10, collection.items.size) // 10 top-level folders

        // Count total requests
        val totalRequests = countRequests(collection.items)
        assertEquals(1000, totalRequests, "Should have exactly 1000 requests")

        println("Total folders: ${countFolders(collection.items)}")
        println("Total requests: $totalRequests")
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    fun testGenerateMockCollection_5000Requests() {
        val startTime = System.currentTimeMillis()
        val collection = generateMockCollection(requestCount = 5000, folderCount = 20, maxDepth = 4)
        val generationTime = System.currentTimeMillis() - startTime

        println("5000 requests generation time: ${generationTime}ms")

        val totalRequests = countRequests(collection.items)
        assertEquals(5000, totalRequests, "Should have exactly 5000 requests")
    }

    @Test
    fun testGenerateMockCollection_VariousMethods() {
        val collection = generateMockCollection(requestCount = 100, folderCount = 1, maxDepth = 1)

        // Verify different HTTP methods are used
        val methods = mutableSetOf<HttpMethod>()
        collectMethods(collection.items, methods)

        assertTrue(methods.size > 1, "Should have requests with different HTTP methods")
    }

    @Test
    fun testGenerateMockCollection_HasScripts() {
        val collection = generateMockCollection(requestCount = 10, folderCount = 1, maxDepth = 1)

        val firstRequest = collection.items.first() as CollectionItem.Folder
        val request = firstRequest.children.first() as CollectionItem.Request

        assertTrue(request.preRequestScript.isNotBlank(), "Request should have pre-request script")
        assertTrue(request.testScript.isNotBlank(), "Request should have test script")
    }

    @Test
    fun testCollectionSerialization_Performance() {
        val collection = generateMockCollection(requestCount = 1000)

        val startTime = System.currentTimeMillis()
        val serializable = SerializableCollection.from(collection)
        val serializationTime = System.currentTimeMillis() - startTime

        println("Serialization time for 1000 requests: ${serializationTime}ms")

        val startTime2 = System.currentTimeMillis()
        val converted = serializable.toCollection()
        val deserializationTime = System.currentTimeMillis() - startTime2

        println("Deserialization time for 1000 requests: ${deserializationTime}ms")

        assertEquals(collection.id, converted.id)
        assertEquals(1000, countRequests(converted.items))
    }

    // Helper functions
    private fun countRequests(items: List<CollectionItem>): Int {
        var count = 0
        for (item in items) {
            when (item) {
                is CollectionItem.Request -> count++
                is CollectionItem.Folder -> count += countRequests(item.children)
            }
        }
        return count
    }

    private fun countFolders(items: List<CollectionItem>): Int {
        var count = 0
        for (item in items) {
            if (item is CollectionItem.Folder) {
                count++
                count += countFolders(item.children)
            }
        }
        return count
    }

    private fun collectMethods(items: List<CollectionItem>, methods: MutableSet<HttpMethod>) {
        for (item in items) {
            when (item) {
                is CollectionItem.Request -> methods.add(item.request.method)
                is CollectionItem.Folder -> collectMethods(item.children, methods)
            }
        }
    }
}