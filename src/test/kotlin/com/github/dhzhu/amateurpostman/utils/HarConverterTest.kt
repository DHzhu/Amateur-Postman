package com.github.dhzhu.amateurpostman.utils

import com.github.dhzhu.amateurpostman.models.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HarConverterTest {

    // ── isStaticResource ──────────────────────────────────────────────────────

    @Test
    fun `xhr entry is not static`() {
        assertFalse(HarConverter.isStaticResource(entry("GET", "https://api.example.com/data", resourceType = "xhr")))
    }

    @Test
    fun `fetch entry is not static`() {
        assertFalse(HarConverter.isStaticResource(entry("POST", "https://api.example.com/login", resourceType = "fetch")))
    }

    @Test
    fun `image resource type is static`() {
        assertTrue(HarConverter.isStaticResource(entry("GET", "https://cdn.example.com/logo.png", resourceType = "image")))
    }

    @Test
    fun `font resource type is static`() {
        assertTrue(HarConverter.isStaticResource(entry("GET", "https://cdn.example.com/font.woff2", resourceType = "font")))
    }

    @Test
    fun `stylesheet resource type is static`() {
        assertTrue(HarConverter.isStaticResource(entry("GET", "https://cdn.example.com/app.css", resourceType = "stylesheet")))
    }

    @Test
    fun `media resource type is static`() {
        assertTrue(HarConverter.isStaticResource(entry("GET", "https://cdn.example.com/video.mp4", resourceType = "media")))
    }

    @Test
    fun `image MIME type is static even without resourceType`() {
        assertTrue(HarConverter.isStaticResource(entry("GET", "https://cdn.example.com/icon.png", mimeType = "image/png")))
    }

    @Test
    fun `text slash css MIME type is static`() {
        assertTrue(HarConverter.isStaticResource(entry("GET", "https://cdn.example.com/style.css", mimeType = "text/css")))
    }

    @Test
    fun `application slash json MIME type is not static`() {
        assertFalse(HarConverter.isStaticResource(entry("GET", "https://api.example.com/data", mimeType = "application/json")))
    }

    @Test
    fun `no resourceType and no MIME type is not static`() {
        assertFalse(HarConverter.isStaticResource(entry("GET", "https://api.example.com/data")))
    }

    // ── groupByHost ───────────────────────────────────────────────────────────

    @Test
    fun `entries are grouped by host`() {
        val entries = listOf(
            entry("GET", "https://api.example.com/users"),
            entry("POST", "https://api.example.com/login"),
            entry("GET", "https://cdn.example.com/data")
        )

        val groups = HarConverter.groupByHost(entries, filterStatic = false)

        assertEquals(2, groups.size)
        assertEquals(2, groups["api.example.com"]?.size)
        assertEquals(1, groups["cdn.example.com"]?.size)
    }

    @Test
    fun `static resources are filtered by default`() {
        val entries = listOf(
            entry("GET", "https://api.example.com/data", resourceType = "xhr"),
            entry("GET", "https://cdn.example.com/logo.png", resourceType = "image"),
            entry("GET", "https://cdn.example.com/app.css", resourceType = "stylesheet")
        )

        val groups = HarConverter.groupByHost(entries)

        assertEquals(1, groups.size)
        assertEquals(1, groups["api.example.com"]?.size)
    }

    @Test
    fun `filterStatic false keeps static resources`() {
        val entries = listOf(
            entry("GET", "https://api.example.com/data", resourceType = "xhr"),
            entry("GET", "https://cdn.example.com/logo.png", resourceType = "image")
        )

        val groups = HarConverter.groupByHost(entries, filterStatic = false)

        assertEquals(2, groups.size)
    }

    @Test
    fun `host is lowercased`() {
        val entries = listOf(entry("GET", "https://API.EXAMPLE.COM/data"))

        val groups = HarConverter.groupByHost(entries, filterStatic = false)

        assertTrue(groups.containsKey("api.example.com"))
    }

    // ── toHttpRequest ─────────────────────────────────────────────────────────

    @Test
    fun `converts GET request correctly`() {
        val e = entry("GET", "https://api.example.com/users")
        val req = HarConverter.toHttpRequest(e)

        assertEquals(HttpMethod.GET, req.method)
        assertEquals("https://api.example.com/users", req.url)
    }

    @Test
    fun `converts POST request with JSON body`() {
        val e = entryWithBody(
            method = "POST",
            url = "https://api.example.com/users",
            mimeType = "application/json",
            text = """{"name":"Alice"}"""
        )
        val req = HarConverter.toHttpRequest(e)

        assertEquals(HttpMethod.POST, req.method)
        assertNotNull(req.body)
        assertEquals("""{"name":"Alice"}""", req.body?.content)
        assertEquals(BodyType.JSON, req.body?.type)
    }

    @Test
    fun `converts POST with urlencoded body from params`() {
        val e = entryWithUrlencodedParams(
            "POST", "https://api.example.com/login",
            listOf("username" to "alice", "password" to "secret")
        )
        val req = HarConverter.toHttpRequest(e)

        assertEquals(BodyType.FORM_URLENCODED, req.body?.type)
        val content = req.body?.content ?: ""
        // Values are URL-encoded (plain ASCII stays the same)
        assertTrue(content.contains("username=alice"), "content: $content")
        assertTrue(content.contains("password=secret"), "content: $content")
    }

    @Test
    fun `urlencoded params with special characters are encoded`() {
        val e = entryWithUrlencodedParams(
            "POST", "https://api.example.com/login",
            listOf("email" to "alice@example.com", "note" to "a b&c=d")
        )
        val req = HarConverter.toHttpRequest(e)

        val content = req.body?.content ?: ""
        // @ and special chars should be percent-encoded
        assertFalse(content.contains("alice@example.com"), "@ should be encoded")
        assertTrue(content.contains("alice%40example.com"), "content: $content")
    }

    @Test
    fun `converts multipart body into MultipartPart list`() {
        val params = listOf(
            HarParser.HarParam("username", "alice"),
            HarParser.HarParam("file", "", fileName = "photo.jpg", contentType = "image/jpeg")
        )
        val e = entryWithMultipartParams("POST", "https://api.example.com/upload", params)
        val req = HarConverter.toHttpRequest(e)

        assertEquals(BodyType.MULTIPART, req.body?.type)
        val parts = req.body?.multipartData
        assertNotNull(parts)
        assertEquals(2, parts!!.size)

        val textPart = parts[0] as? MultipartPart.TextField
        assertNotNull(textPart)
        assertEquals("username", textPart!!.key)
        assertEquals("alice", textPart.value)

        val filePart = parts[1] as? MultipartPart.FileField
        assertNotNull(filePart)
        assertEquals("file", filePart!!.key)
        assertEquals("photo.jpg", filePart.fileName)
        assertEquals("image/jpeg", filePart.contentType)
    }

    @Test
    fun `skips HTTP2 pseudo headers`() {
        val e = entryWithHeaders(
            "GET", "https://api.example.com/data",
            listOf(
                HarParser.HarNameValue(":authority", "api.example.com"),
                HarParser.HarNameValue(":method", "GET"),
                HarParser.HarNameValue("Authorization", "Bearer token")
            )
        )
        val req = HarConverter.toHttpRequest(e)

        assertFalse(req.headers.containsKey(":authority"))
        assertFalse(req.headers.containsKey(":method"))
        assertTrue(req.headers.containsKey("Authorization"))
    }

    @Test
    fun `duplicate headers are merged with comma separator`() {
        val e = entryWithHeaders(
            "GET", "https://api.example.com/data",
            listOf(
                HarParser.HarNameValue("Cache-Control", "no-cache"),
                HarParser.HarNameValue("Cache-Control", "no-store")
            )
        )
        val req = HarConverter.toHttpRequest(e)

        assertEquals("no-cache, no-store", req.headers["Cache-Control"])
    }

    @Test
    fun `unknown HTTP method defaults to GET with warning`() {
        val e = entry("PURGE", "https://api.example.com/cache")
        val warnings = mutableListOf<String>()
        val req = HarConverter.toHttpRequest(e, warnings)

        assertEquals(HttpMethod.GET, req.method)
        assertEquals(1, warnings.size)
        assertTrue(warnings[0].contains("PURGE"))
    }

    // ── toCollection ──────────────────────────────────────────────────────────

    @Test
    fun `single host creates flat list without folder wrapper`() {
        val entries = listOf(
            entry("GET", "https://api.example.com/users"),
            entry("POST", "https://api.example.com/login")
        )

        val result = HarConverter.toCollection("my-recording", entries)

        assertTrue(result.isSuccess)
        val items = result.collection!!.items
        assertEquals(2, items.size)
        assertTrue(items.all { it is CollectionItem.Request })
    }

    @Test
    fun `multiple hosts creates one folder per host`() {
        val entries = listOf(
            entry("GET", "https://api.example.com/users"),
            entry("GET", "https://auth.example.com/token")
        )

        val result = HarConverter.toCollection("recording", entries)

        assertTrue(result.isSuccess)
        val items = result.collection!!.items
        assertEquals(2, items.size)
        assertTrue(items.all { it is CollectionItem.Folder })
        val folderNames = items.map { it.name }.toSet()
        assertTrue(folderNames.contains("api.example.com"))
        assertTrue(folderNames.contains("auth.example.com"))
    }

    @Test
    fun `request names include method and path`() {
        val entries = listOf(entry("GET", "https://api.example.com/users/42"))

        val result = HarConverter.toCollection("test", entries)

        assertTrue(result.isSuccess)
        val requestItem = result.collection!!.items[0] as CollectionItem.Request
        assertEquals("GET /users/42", requestItem.name)
    }

    @Test
    fun `collection name is set correctly`() {
        val result = HarConverter.toCollection("my-har", listOf(entry("GET", "https://api.example.com/")))

        assertEquals("my-har", result.collection?.name)
    }

    @Test
    fun `empty name returns error`() {
        val result = HarConverter.toCollection("  ", listOf(entry("GET", "https://api.example.com/")))

        assertFalse(result.isSuccess)
        assertNotNull(result.error)
    }

    @Test
    fun `empty entries list returns error`() {
        val result = HarConverter.toCollection("test", emptyList())

        assertFalse(result.isSuccess)
        assertNotNull(result.error)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun entry(
        method: String,
        url: String,
        resourceType: String? = null,
        mimeType: String = ""
    ) = HarParser.HarEntry(
        request = HarParser.HarRequest(method = method, url = url),
        response = HarParser.HarResponse(
            status = 200,
            content = HarParser.HarContent(mimeType = mimeType)
        ),
        _resourceType = resourceType
    )

    private fun entryWithBody(method: String, url: String, mimeType: String, text: String) =
        HarParser.HarEntry(
            request = HarParser.HarRequest(
                method = method,
                url = url,
                postData = HarParser.HarPostData(mimeType = mimeType, text = text)
            ),
            response = HarParser.HarResponse(status = 200)
        )

    private fun entryWithUrlencodedParams(
        method: String,
        url: String,
        params: List<Pair<String, String>>
    ) = HarParser.HarEntry(
        request = HarParser.HarRequest(
            method = method,
            url = url,
            postData = HarParser.HarPostData(
                mimeType = "application/x-www-form-urlencoded",
                params = params.map { HarParser.HarParam(it.first, it.second) }
            )
        ),
        response = HarParser.HarResponse(status = 200)
    )

    private fun entryWithMultipartParams(
        method: String,
        url: String,
        params: List<HarParser.HarParam>
    ) = HarParser.HarEntry(
        request = HarParser.HarRequest(
            method = method,
            url = url,
            postData = HarParser.HarPostData(
                mimeType = "multipart/form-data",
                params = params
            )
        ),
        response = HarParser.HarResponse(status = 200)
    )

    private fun entryWithHeaders(
        method: String,
        url: String,
        headers: List<HarParser.HarNameValue>
    ) = HarParser.HarEntry(
        request = HarParser.HarRequest(method = method, url = url, headers = headers),
        response = HarParser.HarResponse(status = 200)
    )
}
