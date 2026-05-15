package com.github.dhzhu.amateurpostman.services.grpc

import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.Descriptors.MethodDescriptor
import com.google.protobuf.Descriptors.ServiceDescriptor
import com.google.protobuf.DynamicMessage
import com.google.protobuf.util.JsonFormat
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

/**
 * ProtoParser — dynamically parses .proto files without requiring pre-compilation.
 *
 * Strategy:
 *   1. Invoke `protoc --descriptor_set_out` to produce a binary FileDescriptorSet.
 *   2. Load the binary descriptor and reconstruct the full descriptor graph,
 *      resolving imports in dependency order.
 *   3. Extract Service/Method definitions from the resolved descriptors.
 *
 * This avoids any compile-time code generation (protoc-gen-grpc-kotlin) and
 * keeps the plugin dependency-free of user toolchains beyond `protoc`.
 */
class ProtoParser {

    companion object {
        private val LOG = Logger.getLogger(ProtoParser::class.java.name)

        /**
         * Well-known type package prefix — these are shipped with protobuf-java
         * and should always be resolvable without explicit .proto include paths.
         */
        private const val GOOGLE_PROTOBUF_PKG = "google.protobuf"
    }

    /**
     * Parse a single .proto file, resolving all its imports.
     *
     * @param protoFile  The target .proto file.
     * @param importPaths  Additional directories that `protoc` will search for imports.
     *                    The directory containing [protoFile] is always added automatically.
     * @return [ProtoParseResult] with parsed services, or an error message on failure.
     */
    fun parse(protoFile: File, importPaths: List<File> = emptyList()): ProtoParseResult {
        if (!protoFile.exists()) {
            return ProtoParseResult(
                protoFilePath = protoFile.absolutePath,
                services = emptyList(),
                error = "File not found: ${protoFile.absolutePath}"
            )
        }

        return try {
            val descriptorSetFile = compileToDescriptorSet(protoFile, importPaths)
            val fileDescriptors = loadFileDescriptors(descriptorSetFile)
            val services = extractServices(protoFile.nameWithoutExtension, fileDescriptors)

            ProtoParseResult(
                protoFilePath = protoFile.absolutePath,
                services = services
            )
        } catch (e: ProtocNotFoundException) {
            ProtoParseResult(
                protoFilePath = protoFile.absolutePath,
                services = emptyList(),
                error = "protoc not found. Please install Protocol Buffers compiler (protoc) " +
                        "and ensure it is on your PATH. Download from: https://github.com/protocolbuffers/protobuf/releases"
            )
        } catch (e: Exception) {
            ProtoParseResult(
                protoFilePath = protoFile.absolutePath,
                services = emptyList(),
                error = "Failed to parse proto file: ${e.message}"
            )
        }
    }

    /**
     * Parse a binary FileDescriptorSet directly (e.g. pre-generated via protoc).
     * This is the fallback path when `protoc` is unavailable or the user provides descriptors.
     */
    fun parseFromDescriptorSet(descriptorSetFile: File, targetProtoName: String? = null): ProtoParseResult {
        return try {
            val fileDescriptors = loadFileDescriptors(descriptorSetFile)
            val services = extractServices(targetProtoName, fileDescriptors)
            ProtoParseResult(
                protoFilePath = descriptorSetFile.absolutePath,
                services = services
            )
        } catch (e: Exception) {
            ProtoParseResult(
                protoFilePath = descriptorSetFile.absolutePath,
                services = emptyList(),
                error = "Failed to load descriptor set: ${e.message}"
            )
        }
    }

    // ─── Private helpers ────────────────────────────────────────────────────────

    /**
     * Invokes `protoc` to produce a binary FileDescriptorSet that includes
     * all transitive dependencies.
     */
    private fun compileToDescriptorSet(protoFile: File, extraImportPaths: List<File>): File {
        val protocPath = findProtoc()
            ?: throw ProtocNotFoundException("protoc not found on PATH")

        val outputFile = Files.createTempFile("amateur_postman_proto_", ".pb").toFile()
        outputFile.deleteOnExit()

        val importDirs = buildList {
            add(protoFile.parentFile)
            addAll(extraImportPaths)
        }.map { "--proto_path=${it.absolutePath}" }

        val cmd = buildList {
            add(protocPath)
            addAll(importDirs)
            add("--descriptor_set_out=${outputFile.absolutePath}")
            add("--include_imports") // include all transitive dependencies in the output
            add(protoFile.absolutePath)
        }

        LOG.fine("Running protoc: ${cmd.joinToString(" ")}")

        val process = ProcessBuilder(cmd)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor(30, TimeUnit.SECONDS)

        if (!exitCode || process.exitValue() != 0) {
            throw IllegalStateException(
                "protoc failed (exit=${if (exitCode) process.exitValue() else "timeout"}):\n$output"
            )
        }

        return outputFile
    }

    /**
     * Finds the `protoc` binary on the system PATH.
     */
    private fun findProtoc(): String? {
        val candidates = listOf("protoc") +
                System.getenv("PATH").orEmpty().split(File.pathSeparator)
                    .map { "$it/protoc" }

        return candidates.firstOrNull { path ->
            try {
                val pb = ProcessBuilder(path, "--version").start()
                pb.waitFor(5, TimeUnit.SECONDS)
                pb.exitValue() == 0
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Loads a binary FileDescriptorSet and reconstructs the full descriptor
     * graph in dependency order (imports must come before dependents).
     */
    private fun loadFileDescriptors(descriptorSetFile: File): Map<String, FileDescriptor> {
        val fds = FileInputStream(descriptorSetFile).use { input ->
            FileDescriptorSet.parseFrom(input)
        }

        // Build a dependency-ordered map: name → FileDescriptorProto
        val protoByName = fds.fileList.associateBy { it.name }
        val resolved = mutableMapOf<String, FileDescriptor>()

        fun resolve(proto: FileDescriptorProto) {
            if (resolved.containsKey(proto.name)) return
            // Resolve all imports first
            for (dep in proto.dependencyList) {
                val depProto = protoByName[dep]
                    ?: continue // Well-known types are already in protobuf-java's classpath
                resolve(depProto)
            }
            val deps = proto.dependencyList
                .mapNotNull { resolved[it] }
                .toTypedArray()
            resolved[proto.name] = FileDescriptor.buildFrom(proto, deps)
        }

        fds.fileList.forEach { resolve(it) }
        return resolved
    }

    /**
     * Extracts all gRPC services from the resolved FileDescriptors.
     *
     * @param targetName  If set, only services from the file matching this name are returned.
     *                    If null, services from all files are returned.
     */
    private fun extractServices(
        targetName: String?,
        fileDescriptors: Map<String, FileDescriptor>
    ): List<GrpcServiceInfo> {
        return fileDescriptors.values
            .filter { fd ->
                // Skip google well-known types
                !fd.`package`.startsWith(GOOGLE_PROTOBUF_PKG) &&
                        (targetName == null || fd.name.contains(targetName))
            }
            .flatMap { fd -> fd.services.map { it.toGrpcServiceInfo() } }
    }

    private fun ServiceDescriptor.toGrpcServiceInfo(): GrpcServiceInfo {
        return GrpcServiceInfo(
            name = name,
            fullName = fullName,
            methods = methods.map { it.toGrpcMethodInfo() }
        )
    }

    private fun MethodDescriptor.toGrpcMethodInfo(): GrpcMethodInfo {
        return GrpcMethodInfo(
            name = name,
            fullName = fullName,
            inputTypeName = inputType.fullName,
            outputTypeName = outputType.fullName,
            clientStreaming = isClientStreaming,
            serverStreaming = isServerStreaming
        )
    }

    // ─── JSON ↔ DynamicMessage ───────────────────────────────────────────────

    /**
     * Converts a JSON string to a [DynamicMessage] based on the given [Descriptor].
     *
     * @throws com.google.protobuf.InvalidProtocolBufferException on parse failure.
     */
    fun jsonToMessage(json: String, descriptor: Descriptor): DynamicMessage {
        val builder = DynamicMessage.newBuilder(descriptor)
        JsonFormat.parser()
            .ignoringUnknownFields()
            .merge(json, builder)
        return builder.build()
    }

    /**
     * Converts a [DynamicMessage] to a JSON string.
     */
    fun messageToJson(message: DynamicMessage): String {
        return JsonFormat.printer()
            .alwaysPrintFieldsWithNoPresence()
            .preservingProtoFieldNames()
            .print(message)
    }

    /**
     * Generates an empty JSON template for the given [Descriptor],
     * useful for pre-populating the request body editor.
     */
    fun generateJsonTemplate(descriptor: Descriptor): String {
        val empty = DynamicMessage.getDefaultInstance(descriptor)
        return JsonFormat.printer()
            .alwaysPrintFieldsWithNoPresence()
            .preservingProtoFieldNames()
            .print(empty)
    }

    /**
     * Returns a map of field name → type description for a given message [Descriptor].
     * Useful for building UI hints.
     */
    fun describeFields(descriptor: Descriptor): Map<String, String> {
        return descriptor.fields.associate { field ->
            val typeName = when (field.type) {
                FieldDescriptor.Type.MESSAGE -> field.messageType.fullName
                FieldDescriptor.Type.ENUM -> "enum:${field.enumType.fullName}"
                else -> field.type.name.lowercase()
            }
            val labelPrefix = if (field.isRepeated) "repeated " else ""
            field.name to "$labelPrefix$typeName"
        }
    }
}

/** Thrown when the `protoc` binary cannot be found on the system. */
class ProtocNotFoundException(message: String) : Exception(message)
