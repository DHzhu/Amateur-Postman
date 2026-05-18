package com.github.dhzhu.amateurpostman.utils

import com.intellij.openapi.diagnostic.thisLogger
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A lightweight HTTP/1.1 server using only standard Java APIs (java.net.ServerSocket).
 *
 * This replaces the usage of com.sun.net.httpserver.HttpServer which is flagged
 * as internal API by the IntelliJ Plugin Verifier.
 *
 * Protocol simplifications:
 * - No chunked transfer encoding
 * - No HTTPS (localhost only)
 * - No keep-alive (one request per connection)
 * - No HTTP pipelining
 */
class SimpleHttpServer {

    private val logger = thisLogger()
    private var serverSocket: ServerSocket? = null
    private var executor: ExecutorService? = null
    private val running = AtomicBoolean(false)
    private val handlers = ConcurrentHashMap<String, (SimpleHttpExchange) -> Unit>()
    private val activeSockets = ConcurrentHashMap.newKeySet<Socket>()

    /**
     * Registers a handler for a given path prefix.
     */
    fun createContext(path: String, handler: (SimpleHttpExchange) -> Unit) {
        handlers[path] = handler
    }

    /**
     * Starts the server on the specified port.
     * @param port Port to bind to (0 = auto-assign)
     * @return The actual port the server is listening on
     */
    fun start(port: Int): Int {
        val ss = ServerSocket()
        ss.reuseAddress = true
        ss.bind(InetSocketAddress(port))
        serverSocket = ss

        val exec = Executors.newCachedThreadPool { r ->
            Thread(r, "SimpleHttpServer-${ss.localPort}").apply { isDaemon = true }
        }
        executor = exec
        running.set(true)

        exec.submit {
            while (running.get()) {
                try {
                    val socket = ss.accept()
                    exec.submit { handleConnection(socket) }
                } catch (e: IOException) {
                    if (running.get()) {
                        logger.warn("Accept failed", e)
                    }
                }
            }
        }

        logger.info("SimpleHttpServer started on port ${ss.localPort}")
        return ss.localPort
    }

    /**
     * Stops the server and releases resources.
     */
    fun stop() {
        running.set(false)
        try {
            serverSocket?.close()
        } catch (_: IOException) {
        }
        // Close all active client sockets to interrupt blocking I/O
        activeSockets.forEach { socket ->
            try { socket.close() } catch (_: IOException) {}
        }
        activeSockets.clear()
        executor?.shutdownNow()
        serverSocket = null
        executor = null
        logger.info("SimpleHttpServer stopped")
    }

    /**
     * The address the server is bound to.
     */
    val address: InetSocketAddress
        get() = InetSocketAddress(
            serverSocket?.inetAddress ?: java.net.InetAddress.getLocalHost(),
            serverSocket?.localPort ?: 0
        )

    private fun handleConnection(socket: Socket) {
        activeSockets.add(socket)
        try {
            socket.use { sock ->
                sock.soTimeout = 30_000 // 30s read timeout
                val input = BufferedInputStream(sock.getInputStream())
                val output = BufferedOutputStream(sock.getOutputStream())

                val exchange = parseRequest(input) ?: return
                val handler = findHandler(exchange.requestUri.path)
                    ?: findHandler("/") // fallback to root handler

                if (handler != null) {
                    handler(exchange)
                } else {
                    exchange.sendResponse(
                        404,
                        mapOf("Content-Type" to "text/plain"),
                        "Not Found".toByteArray(Charsets.UTF_8)
                    )
                }

                output.write(exchange.responseBytes)
                output.flush()
            }
        } catch (e: Exception) {
            logger.debug("Connection handling error", e)
        } finally {
            activeSockets.remove(socket)
        }
    }

    private fun findHandler(path: String): ((SimpleHttpExchange) -> Unit)? {
        // Exact match first
        handlers[path]?.let { return it }
        // Prefix match (longest first)
        return handlers.entries
            .filter { path.startsWith(it.key) }
            .maxByOrNull { it.key.length }
            ?.value
    }

    private fun parseRequest(input: BufferedInputStream): SimpleHttpExchange? {
        // Read request line
        val requestLine = readLine(input) ?: return null
        val parts = requestLine.split(" ", limit = 3)
        if (parts.size < 2) return null

        val method = parts[0]
        val uriStr = parts[1]
        val uri = URI(uriStr)

        // Read headers
        val headers = mutableMapOf<String, MutableList<String>>()
        while (true) {
            val line = readLine(input) ?: break
            if (line.isEmpty()) break // Empty line marks end of headers

            val colonIdx = line.indexOf(':')
            if (colonIdx > 0) {
                val name = line.substring(0, colonIdx).trim()
                val value = line.substring(colonIdx + 1).trim()
                headers.getOrPut(name) { mutableListOf() }.add(value)
            }
        }

        // Read body if Content-Length is present
        val contentLength = headers["Content-Length"]?.firstOrNull()?.toLongOrNull() ?: 0L
        val body = if (contentLength > 0) {
            val buffer = ByteArray(contentLength.toInt())
            var totalRead = 0
            while (totalRead < contentLength) {
                val bytesRead = input.read(buffer, totalRead, (contentLength - totalRead).toInt())
                if (bytesRead == -1) break
                totalRead += bytesRead
            }
            String(buffer, 0, totalRead, Charsets.UTF_8)
        } else {
            null
        }

        return SimpleHttpExchange(
            requestUri = uri,
            requestMethod = method,
            requestHeaders = headers,
            requestBody = body
        )
    }

    private fun readLine(input: BufferedInputStream): String? {
        val sb = StringBuilder()
        var prev = -1
        while (true) {
            val b = input.read()
            if (b == -1) {
                return if (sb.isEmpty() && prev == -1) null else sb.toString()
            }
            if (b == '\n'.code && prev == '\r'.code) {
                sb.deleteCharAt(sb.length - 1) // remove the \r
                return sb.toString()
            }
            sb.append(b.toChar())
            prev = b
        }
    }
}

/**
 * Represents a single HTTP request/response exchange.
 */
class SimpleHttpExchange(
    val requestUri: URI,
    val requestMethod: String,
    val requestHeaders: Map<String, List<String>>,
    val requestBody: String?
) {
    @Volatile
    var responseBytes: ByteArray = ByteArray(0)
        private set

    /**
     * Sends an HTTP response.
     */
    fun sendResponse(statusCode: Int, headers: Map<String, String>, body: ByteArray) {
        val reasonPhrase = STATUS_PHRASES[statusCode] ?: "Unknown"
        val sb = StringBuilder()
        sb.append("HTTP/1.1 $statusCode $reasonPhrase\r\n")
        headers.forEach { (name, value) ->
            sb.append("$name: $value\r\n")
        }
        sb.append("Content-Length: ${body.size}\r\n")
        sb.append("Connection: close\r\n")
        sb.append("\r\n")

        val headerBytes = sb.toString().toByteArray(Charsets.ISO_8859_1)
        responseBytes = headerBytes + body
    }

    companion object {
        private val STATUS_PHRASES = mapOf(
            200 to "OK",
            201 to "Created",
            204 to "No Content",
            301 to "Moved Permanently",
            302 to "Found",
            304 to "Not Modified",
            400 to "Bad Request",
            401 to "Unauthorized",
            403 to "Forbidden",
            404 to "Not Found",
            405 to "Method Not Allowed",
            408 to "Request Timeout",
            409 to "Conflict",
            413 to "Payload Too Large",
            500 to "Internal Server Error",
            502 to "Bad Gateway",
            503 to "Service Unavailable"
        )
    }
}
