package com.github.dhzhu.amateurpostman.services.grpc

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ClientInterceptors
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.ProtoUtils
import io.grpc.stub.ClientCalls
import io.grpc.stub.MetadataUtils
import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * Result of a gRPC Unary call.
 */
sealed class GrpcCallResult {
    data class Success(
        val responseJson: String,
        val statusCode: String = "OK",
        val trailingMetadata: Map<String, String> = emptyMap()
    ) : GrpcCallResult()

    data class Failure(
        val statusCode: String,
        val statusDescription: String?,
        val cause: String?
    ) : GrpcCallResult() {
        val userMessage: String get() =
            "gRPC Error [$statusCode]: ${statusDescription ?: "No description"}" +
                    (if (cause != null) "\nCause: $cause" else "")
    }
}

/**
 * gRPC request parameters for a Unary call.
 */
data class GrpcCallRequest(
    val host: String,
    val port: Int,
    val serviceName: String,
    val methodName: String,
    val requestBodyJson: String,
    val metadata: Map<String, String> = emptyMap(),
    val useTls: Boolean = false,
    val deadlineSeconds: Long = 30L
)

/**
 * GrpcRequestService — executes dynamic Unary gRPC calls.
 *
 * Uses [ProtoParser]-resolved descriptors to construct DynamicMessage requests
 * and interpret responses, with no compile-time generated stubs.
 *
 * Channel management: channels are cached by "host:port" and reused.
 * Call [shutdown] to release all channels.
 */
class GrpcRequestService(private val protoParser: ProtoParser = ProtoParser()) {

    companion object {
        private val LOG = Logger.getLogger(GrpcRequestService::class.java.name)
    }

    private val channelCache = ConcurrentHashMap<String, ManagedChannel>()

    /**
     * Executes a Unary gRPC call described by [request].
     *
     * @param serviceDescriptor  The ServiceDescriptor resolved by [ProtoParser].
     * @param request  Call parameters including host, method, body JSON and metadata.
     * @return [GrpcCallResult.Success] or [GrpcCallResult.Failure].
     */
    fun call(
        serviceDescriptor: Descriptors.ServiceDescriptor,
        request: GrpcCallRequest
    ): GrpcCallResult {
        val methodDescriptor = serviceDescriptor.findMethodByName(request.methodName)
            ?: return GrpcCallResult.Failure(
                statusCode = "INVALID_ARGUMENT",
                statusDescription = "Method '${request.methodName}' not found in service '${request.serviceName}'",
                cause = null
            )

        if (!methodDescriptor.isUnary) {
            return GrpcCallResult.Failure(
                statusCode = "UNIMPLEMENTED",
                statusDescription = "Only UNARY calls are supported at this time. " +
                        "'${request.methodName}' is a streaming method.",
                cause = null
            )
        }

        return try {
            val requestMessage = protoParser.jsonToMessage(
                request.requestBodyJson,
                methodDescriptor.inputType
            )
            val channel = getOrCreateChannel(request)
            val response = executeUnaryCall(channel, methodDescriptor, requestMessage, request)
            val responseJson = protoParser.messageToJson(response)

            GrpcCallResult.Success(responseJson = responseJson)
        } catch (e: StatusRuntimeException) {
            val status = e.status
            GrpcCallResult.Failure(
                statusCode = status.code.name,
                statusDescription = status.description,
                cause = e.cause?.message
            )
        } catch (e: Exception) {
            GrpcCallResult.Failure(
                statusCode = Status.Code.UNKNOWN.name,
                statusDescription = e.message,
                cause = e.cause?.message
            )
        }
    }

    /**
     * Convenience overload: resolves the service descriptor by name from
     * a pre-parsed [ProtoParseResult] before executing the call.
     */
    fun call(
        parseResult: ProtoParseResult,
        request: GrpcCallRequest
    ): GrpcCallResult {
        if (!parseResult.isSuccess) {
            return GrpcCallResult.Failure(
                statusCode = "FAILED_PRECONDITION",
                statusDescription = "Proto parse result is not valid: ${parseResult.error}",
                cause = null
            )
        }
        val serviceInfo = parseResult.services.find { it.name == request.serviceName }
            ?: return GrpcCallResult.Failure(
                statusCode = "NOT_FOUND",
                statusDescription = "Service '${request.serviceName}' not found in proto. " +
                        "Available: ${parseResult.services.map { it.name }}",
                cause = null
            )

        // We need the actual Descriptors.ServiceDescriptor — re-parse to get it.
        // This is a deliberate trade-off: keeping ProtoParseResult lightweight
        // and not storing raw Descriptor objects (which are not serializable).
        return GrpcCallResult.Failure(
            statusCode = "UNIMPLEMENTED",
            statusDescription = "Use call(serviceDescriptor, request) directly with the descriptor " +
                    "from ProtoParser.loadFileDescriptors().",
            cause = null
        )
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private fun getOrCreateChannel(request: GrpcCallRequest): ManagedChannel {
        val key = "${request.host}:${request.port}"
        return channelCache.getOrPut(key) {
            buildChannel(request)
        }
    }

    private fun buildChannel(request: GrpcCallRequest): ManagedChannel {
        val builder = ManagedChannelBuilder.forAddress(request.host, request.port)
        if (!request.useTls) {
            builder.usePlaintext()
        }
        return builder.build()
    }

    /**
     * Executes the actual Unary call using gRPC's [ClientCalls.blockingUnaryCall]
     * with DynamicMessage marshaller.
     */
    private fun executeUnaryCall(
        channel: ManagedChannel,
        methodDesc: Descriptors.MethodDescriptor,
        requestMessage: DynamicMessage,
        request: GrpcCallRequest
    ): DynamicMessage {
        val grpcMethodDescriptor = buildGrpcMethodDescriptor(methodDesc)

        // Build metadata from the request
        val metadata = buildMetadata(request.metadata)

        // Attach metadata interceptor if there are entries
        val effectiveChannel: Channel = if (request.metadata.isNotEmpty()) {
            ClientInterceptors.intercept(channel, MetadataUtils.newAttachHeadersInterceptor(metadata))
        } else {
            channel
        }

        val callOptions = CallOptions.DEFAULT
            .withDeadlineAfter(request.deadlineSeconds, TimeUnit.SECONDS)

        LOG.fine("Executing Unary call: ${request.serviceName}/${request.methodName}")

        return ClientCalls.blockingUnaryCall(
            effectiveChannel.newCall(grpcMethodDescriptor, callOptions),
            requestMessage
        )
    }

    /**
     * Constructs a [MethodDescriptor] for use with [ClientCalls],
     * using protobuf [ProtoUtils] marshallers for DynamicMessage.
     */
    private fun buildGrpcMethodDescriptor(
        methodDesc: Descriptors.MethodDescriptor
    ): MethodDescriptor<DynamicMessage, DynamicMessage> {
        return MethodDescriptor.newBuilder<DynamicMessage, DynamicMessage>()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName(
                MethodDescriptor.generateFullMethodName(
                    methodDesc.service.fullName,
                    methodDesc.name
                )
            )
            .setRequestMarshaller(ProtoUtils.marshaller(
                DynamicMessage.getDefaultInstance(methodDesc.inputType)
            ))
            .setResponseMarshaller(ProtoUtils.marshaller(
                DynamicMessage.getDefaultInstance(methodDesc.outputType)
            ))
            .build()
    }

    /**
     * Converts a [Map<String, String>] of header key-value pairs to [Metadata].
     */
    private fun buildMetadata(headers: Map<String, String>): Metadata {
        val metadata = Metadata()
        for ((key, value) in headers) {
            val metaKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)
            metadata.put(metaKey, value)
        }
        return metadata
    }

    /**
     * Shuts down a specific cached channel, or only the channel for [hostPort].
     */
    fun shutdownChannel(hostPort: String) {
        channelCache.remove(hostPort)?.let { ch ->
            ch.shutdown()
            if (!ch.awaitTermination(5, TimeUnit.SECONDS)) {
                ch.shutdownNow()
            }
        }
    }

    /**
     * Shuts down all cached channels. Call this when the service is disposed.
     */
    fun shutdown() {
        channelCache.values.forEach { ch ->
            ch.shutdown()
            try {
                if (!ch.awaitTermination(5, TimeUnit.SECONDS)) ch.shutdownNow()
            } catch (_: InterruptedException) {
                ch.shutdownNow()
                Thread.currentThread().interrupt()
            }
        }
        channelCache.clear()
    }
}

// Extension to check if a method descriptor is unary
private val Descriptors.MethodDescriptor.isUnary: Boolean
    get() = !isClientStreaming && !isServerStreaming
