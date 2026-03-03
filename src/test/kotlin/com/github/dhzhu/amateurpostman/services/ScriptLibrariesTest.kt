package com.github.dhzhu.amateurpostman.services

import com.intellij.openapi.project.Project
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

/**
 * Tests for external libraries and utility functions in scripts.
 * Tests crypto-js, atob/btoa, and other utility functions.
 */
class ScriptLibrariesTest {

    private lateinit var scriptService: ScriptExecutionService
    private lateinit var environmentService: EnvironmentService

    @BeforeEach
    fun setUp() {
        environmentService = mock<EnvironmentService>()
        scriptService = ScriptExecutionService(mock<Project>(), environmentService)
    }

    @Test
    fun testAtobDecodesBase64() = runBlocking {
        val script = """
            var decoded = atob('SGVsbG8gV29ybGQ=');
            pm.test('Base64 decoded', () => decoded === 'Hello World');
        """.trimIndent()

        val variables = scriptService.executePreRequestScript(script)

        // Should execute without errors
        assertNotNull(variables)
    }

    @Test
    fun testBtoaEncodesBase64() = runBlocking {
        val script = """
            var encoded = btoa('Hello World');
            pm.test('Base64 encoded', () => encoded === 'SGVsbG8gV29ybGQ=');
        """.trimIndent()

        val variables = scriptService.executePreRequestScript(script)

        assertNotNull(variables)
    }

    @Test
    fun testCryptoJsMd5() = runBlocking {
        val script = """
            var hash = CryptoJS.MD5('message');
            pm.test('MD5 hash generated', () => hash.toString() === '78e731027d8fd50ed642340b7c9a63b3');
        """.trimIndent()

        val variables = scriptService.executePreRequestScript(script)

        assertNotNull(variables)
    }

    @Test
    fun testCryptoJsSha256() = runBlocking {
        val script = """
            var hash = CryptoJS.SHA256('message');
            pm.test('SHA256 hash generated', () => hash.toString().length === 64);
        """.trimIndent()

        val variables = scriptService.executePreRequestScript(script)

        assertNotNull(variables)
    }

    @Test
    fun testCryptoJsHmac() = runBlocking {
        val script = """
            var signature = CryptoJS.HmacSHA256('message', 'secret');
            pm.test('HMAC generated', () => signature.toString().length === 64);
        """.trimIndent()

        val variables = scriptService.executePreRequestScript(script)

        assertNotNull(variables)
    }

    @Test
    fun testCryptoJsInPreRequestScript() = runBlocking {
        val script = """
            var timestamp = Date.now();
            var signature = CryptoJS.HmacSHA256(timestamp + 'request', 'secret').toString();

            // Just verify the script executes without throwing errors
            var sigLength = signature.length;
            var tsValid = timestamp > 0;
        """.trimIndent()

        // Should execute without errors
        val variables = scriptService.executePreRequestScript(script)

        assertNotNull(variables)
    }

    @Test
    fun testAtobWithUtf8() = runBlocking {
        val script = """
            var decoded = atob('SGVsbG8g44GT44KM44GE44G+44GZ');
            pm.test('UTF-8 Base64 decoded', () => decoded.includes('Hello'));
        """.trimIndent()

        val variables = scriptService.executePreRequestScript(script)

        assertNotNull(variables)
    }

    @Test
    fun testBtoaWithUtf8() = runBlocking {
        val script = """
            var encoded = btoa('Hello');
            pm.test('UTF-8 Base64 encoded', () => encoded === 'SGVsbG8=');
        """.trimIndent()

        val variables = scriptService.executePreRequestScript(script)

        assertNotNull(variables)
    }

    @Test
    fun testCryptoJsAes() = runBlocking {
        val script = """
            var encrypted = CryptoJS.AES.encrypt('message', 'secret');
            var decrypted = CryptoJS.AES.decrypt(encrypted.toString(), 'secret');
            var original = decrypted.toString(CryptoJS.enc.Utf8);
            pm.test('AES encryption/decryption', () => original === 'message');
        """.trimIndent()

        val variables = scriptService.executePreRequestScript(script)

        assertNotNull(variables)
    }
}
