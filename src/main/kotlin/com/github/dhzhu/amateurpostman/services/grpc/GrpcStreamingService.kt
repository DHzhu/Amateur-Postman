package com.github.dhzhu.amateurpostman.services.grpc

import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientInterceptors
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.ProtoUtils
import io.grpc.stub.ClientCalls
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * GrpcStreamingService — manages gRPC streaming calls.
 *
 * Supports Server Streaming, Client Streaming, and Bidirectional Streaming.
 * Uses Kotlin Flow for reactive state and message observation.
 */
class GrpcStreamingService(
    private val protoParser: ProtoParser = ProtoParser()
) {
    companion object {
        private val LOG = Logger.getLogger(GrpcStreamingService::class.java.name)
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val channelCache = ConcurrentHashMap<String, ManagedChannel>()

    // State management
    private val _state = MutableStateFlow(GrpcStreamState.IDLE)
    val state: StateFlow<GrpcStreamState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<GrpcStreamMessage>(extraBufferCapacity = 256)
    val messages: SharedFlow<GrpcStreamMessage> = _messages.asSharedFlow()

    private val _messageHistory = mutableListOf<GrpcStreamMessage>()
    val messageHistory: List<GrpcStreamMessage> get() = _messageHistory.toList()

    private var _sentCount = 0
    val sentCount: Int get() = _sentCount

    private var _receivedCount = 0
    val receivedCount: Int get() = _receivedCount

    // Active stream observers
    private var requestObserver: StreamObserver<DynamicMessage>? = null
    private var currentChannel: ManagedChannel? = null

    /**
     * Starts a Server Streaming call.
     * Server sends multiple messages, client receives them.
     */
    fun startServerStreaming(
        serviceDescriptor: Descriptors.ServiceDescriptor,
        methodDescriptor: Descriptors.MethodDescriptor,
        request: GrpcStreamRequest,
        requestBodyJson: String
    ): Boolean {
        if (_state.value != GrpcStreamState.IDLE) {
            LOG.warning("Cannot start server streaming: stream already active")
            return false
        }

        return try {
            _state.value = GrpcStreamState.STREAMING
            clearHistory()

            val requestMessage = protoParser.jsonToMessage(requestBodyJson, methodDescriptor.inputType)
            val channel = getOrCreateChannel(request)
            currentChannel = channel

            val grpcMethodDesc = buildGrpcMethodDescriptor(methodDescriptor, MethodDescriptor.MethodType.SERVER_STREAMING)
            val effectiveChannel = attachMetadataInterceptor(channel, request.metadata)
            val callOptions = CallOptions.DEFAULT
                .withDeadlineAfter(request.deadlineSeconds, TimeUnit.SECONDS)

            val responseObserver = createResponseObserver()

            ClientCalls.asyncServerStreamingCall(
                effectiveChannel.newCall(grpcMethodDesc, callOptions),
                requestMessage,
                responseObserver
            )

            LOG.info("Started server streaming call: ${request.serviceName}/${request.methodName}")
            true
        } catch (e: Exception) {
            _state.value = GrpcStreamState.ERROR
            handleError(e)
            false
        }
    }

    /**
     * Starts a Client Streaming call.
     * Client sends multiple messages, server returns one response.
     */
    fun startClientStreaming(
        serviceDescriptor: Descriptors.ServiceDescriptor,
        methodDescriptor: Descriptors.MethodDescriptor,
        request: GrpcStreamRequest
    ): Boolean {
        if (_state.value != GrpcStreamState.IDLE) {
            LOG.warning("Cannot start client streaming: stream already active")
            return false
        }

        return try {
            _state.value = GrpcStreamState.STREAMING
            clearHistory()

            val channel = getOrCreateChannel(request)
            currentChannel = channel

            val grpcMethodDesc = buildGrpcMethodDescriptor(methodDescriptor, MethodDescriptor.MethodType.CLIENT_STREAMING)
            val effectiveChannel = attachMetadataInterceptor(channel, request.metadata)
            val callOptions = CallOptions.DEFAULT
                .withDeadlineAfter(request.deadlineSeconds, TimeUnit.SECONDS)

            val responseObserver = createResponseObserver()

            requestObserver = ClientCalls.asyncClientStreamingCall(
                effectiveChannel.newCall(grpcMethodDesc, callOptions),
                responseObserver
            )

            LOG.info("Started client streaming call: ${request.serviceName}/${request.methodName}")
            true
        } catch (e: Exception) {
            _state.value = GrpcStreamState.ERROR
            handleError(e)
            false
        }
    }

    /**
     * Starts a Bidirectional Streaming call.
     * Both client and server can send messages independently.
     */
    fun startBidiStreaming(
        serviceDescriptor: Descriptors.ServiceDescriptor,
        methodDescriptor: Descriptors.MethodDescriptor,
        request: GrpcStreamRequest
    ): Boolean {
        if (_state.value != GrpcStreamState.IDLE) {
            LOG.warning("Cannot start bidi streaming: stream already active")
            return false
        }

        return try {
            _state.value = GrpcStreamState.STREAMING
            clearHistory()

            val channel = getOrCreateChannel(request)
            currentChannel = channel

            val grpcMethodDesc = buildGrpcMethodDescriptor(methodDescriptor, MethodDescriptor.MethodType.BIDI_STREAMING)
            val effectiveChannel = attachMetadataInterceptor(channel, request.metadata)
            val callOptions = CallOptions.DEFAULT
                .withDeadlineAfter(request.deadlineSeconds, TimeUnit.SECONDS)

            val responseObserver = createResponseObserver()

            requestObserver = ClientCalls.asyncBidiStreamingCall(
                effectiveChannel.newCall(grpcMethodDesc, callOptions),
                responseObserver
            )

            LOG.info("Started bidirectional streaming call: ${request.serviceName}/${request.methodName}")
            true
        } catch (e: Exception) {
            _state.value = GrpcStreamState.ERROR
            handleError(e)
            false
        }
    }

    /**
     * Sends a message to the active stream (for Client/Bidi streaming).
     */
    fun sendMessage(methodDescriptor: Descriptors.MethodDescriptor, json: String): Boolean {
        if (_state.value != GrpcStreamState.STREAMING) {
            LOG.warning("Cannot send message: stream not active")
            return false
        }

        val observer = requestObserver
        if (observer == null) {
            LOG.warning("Cannot send message: no request observer (not a client/bidi stream?)")
            return false
        }

        return try {
            val message = protoParser.jsonToMessage(json, methodDescriptor.inputType)
            observer.onNext(message)

            val streamMessage = GrpcStreamMessage(content = json, isOutgoing = true)
            _sentCount++
            _messageHistory.add(streamMessage)
            scope.launch { _messages.emit(streamMessage) }

            LOG.fine("Sent streaming message")
            true
        } catch (e: Exception) {
            LOG.warning("Failed to send message: ${e.message}")
            false
        }
    }

    /**
     * Completes the client-side of the stream (for Client/Bidi streaming).
     */
    fun completeStream() {
        if (_state.value != GrpcStreamState.STREAMING) {
            LOG.warning("Cannot complete stream: not streaming")
            return
        }

        requestObserver?.onCompleted()
        LOG.info("Stream completed by client")
    }

    /**
     * Cancels the active stream.
     */
    fun cancelStream() {
        if (_state.value == GrpcStreamState.IDLE) {
            return
        }

        requestObserver?.onError(Status.CANCELLED.withDescription("Client cancelled").asRuntimeException())
        requestObserver = null
        _state.value = GrpcStreamState.IDLE
        LOG.info("Stream cancelled by client")
    }

    /**
     * Clears message history and counters.
     */
    fun clearHistory() {
        _messageHistory.clear()
        _sentCount = 0
        _receivedCount = 0
    }

    /**
     * Cleanup resources.
     */
    fun dispose() {
        cancelStream()
        scope.cancel()
        shutdownChannels()
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private fun createResponseObserver(): StreamObserver<DynamicMessage> {
        return object : StreamObserver<DynamicMessage> {
            override fun onNext(value: DynamicMessage) {
                try {
                    val json = protoParser.messageToJson(value)
                    val message = GrpcStreamMessage(content = json, isOutgoing = false)
                    _receivedCount++
                    _messageHistory.add(message)
                    scope.launch { _messages.emit(message) }
                } catch (e: Exception) {
                    LOG.warning("Failed to serialize response: ${e.message}")
                }
            }

            override fun onError(t: Throwable) {
                _state.value = GrpcStreamState.ERROR
                val (code, desc) = when (t) {
                    is StatusRuntimeException -> t.status.code.name to t.status.description
                    else -> Status.Code.UNKNOWN.name to t.message
                }
                scope.launch {
                    _messages.emit(
                        GrpcStreamMessage(
                            content = "Error [$code]: ${desc ?: "Unknown error"}",
                            isOutgoing = false
                        )
                    )
                }
                LOG.warning("Stream error: $code - $desc")
            }

            override fun onCompleted() {
                _state.value = GrpcStreamState.COMPLETED
                scope.launch {
                    _messages.emit(
                        GrpcStreamMessage(
                            content = "Stream completed",
                            isOutgoing = false
                        )
                    )
                }
                LOG.info("Stream completed by server")
            }
        }
    }

    private fun getOrCreateChannel(request: GrpcStreamRequest): ManagedChannel {
        val key = "${request.host}:${request.port}"
        return channelCache.getOrPut(key) {
            val builder = ManagedChannelBuilder.forAddress(request.host, request.port)
            if (!request.useTls) {
                builder.usePlaintext()
            }
            builder.build()
        }
    }

    private fun attachMetadataInterceptor(channel: ManagedChannel, metadata: Map<String, String>): Channel {
        if (metadata.isEmpty()) return channel

        val grpcMetadata = Metadata()
        for ((key, value) in metadata) {
            val metaKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)
            grpcMetadata.put(metaKey, value)
        }
        return ClientInterceptors.intercept(channel, io.grpc.stub.MetadataUtils.newAttachHeadersInterceptor(grpcMetadata))
    }

    private fun buildGrpcMethodDescriptor(
        methodDesc: Descriptors.MethodDescriptor,
        methodType: MethodDescriptor.MethodType
    ): MethodDescriptor<DynamicMessage, DynamicMessage> {
        return MethodDescriptor.newBuilder<DynamicMessage, DynamicMessage>()
            .setType(methodType)
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

    private fun handleError(e: Exception) {
        val (code, desc) = when (e) {
            is StatusRuntimeException -> e.status.code.name to e.status.description
            else -> Status.Code.UNKNOWN.name to e.message
        }
        scope.launch {
            _messages.emit(
                GrpcStreamMessage(
                    content = "Error [$code]: ${desc ?: "Unknown error"}",
                    isOutgoing = false
                )
            )
        }
        LOG.warning("Stream error: $code - $desc")
    }

    private fun shutdownChannels() {
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