package com.github.dhzhu.amateurpostman.services.grpc

/**
 * Represents a parsed gRPC service definition extracted from a .proto file.
 */
data class GrpcServiceInfo(
    val name: String,
    val fullName: String,
    val methods: List<GrpcMethodInfo>
)

/**
 * Represents a single gRPC method within a service.
 */
data class GrpcMethodInfo(
    val name: String,
    val fullName: String,
    val inputTypeName: String,
    val outputTypeName: String,
    val clientStreaming: Boolean,
    val serverStreaming: Boolean
) {
    val isUnary: Boolean get() = !clientStreaming && !serverStreaming
    val isServerStreaming: Boolean get() = !clientStreaming && serverStreaming
    val isClientStreaming: Boolean get() = clientStreaming && !serverStreaming
    val isBidirectional: Boolean get() = clientStreaming && serverStreaming
}

/**
 * Result of parsing a .proto file.
 */
data class ProtoParseResult(
    val protoFilePath: String,
    val services: List<GrpcServiceInfo>,
    val error: String? = null
) {
    val isSuccess: Boolean get() = error == null
}
