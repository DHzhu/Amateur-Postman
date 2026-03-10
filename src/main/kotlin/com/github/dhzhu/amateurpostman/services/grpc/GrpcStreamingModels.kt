package com.github.dhzhu.amateurpostman.services.grpc

import com.github.dhzhu.amateurpostman.ui.StreamMessage

/**
 * Streaming connection state for gRPC streaming calls.
 */
enum class GrpcStreamState {
    IDLE,           // No active stream
    STREAMING,      // Actively streaming
    COMPLETED,      // Server completed (onCompleted)
    ERROR           // Error occurred (onError)
}

/**
 * Individual message in a gRPC stream.
 *
 * @param content JSON content of the message
 * @param isOutgoing true = sent by client, false = received from server
 * @param timestamp Unix timestamp in milliseconds
 */
data class GrpcStreamMessage(
    override val content: String,
    override val isOutgoing: Boolean,
    override val timestamp: Long = System.currentTimeMillis()
) : StreamMessage

/**
 * Streaming call result for stream lifecycle events.
 */
sealed class GrpcStreamResult {
    data class MessageReceived(val message: GrpcStreamMessage) : GrpcStreamResult()
    data class MessageSent(val message: GrpcStreamMessage) : GrpcStreamResult()
    object StreamCompleted : GrpcStreamResult()
    data class StreamError(val statusCode: String, val description: String?) : GrpcStreamResult()
}

/**
 * gRPC streaming method type.
 */
enum class GrpcMethodType {
    UNARY,
    SERVER_STREAMING,
    CLIENT_STREAMING,
    BIDI_STREAMING
}

/**
 * Configuration for a gRPC streaming call.
 *
 * @param host Server hostname
 * @param port Server port
 * @param serviceName Full service name
 * @param methodName Method name
 * @param metadata gRPC metadata headers
 * @param useTls Whether to use TLS
 * @param deadlineSeconds Call deadline in seconds
 */
data class GrpcStreamRequest(
    val host: String,
    val port: Int,
    val serviceName: String,
    val methodName: String,
    val metadata: Map<String, String> = emptyMap(),
    val useTls: Boolean = false,
    val deadlineSeconds: Long = 30L
)