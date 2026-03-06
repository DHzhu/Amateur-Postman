package com.github.dhzhu.amateurpostman.services.grpc

import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import com.google.protobuf.Descriptors.FileDescriptor
import com.google.protobuf.DynamicMessage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * Tests for ProtoParser covering 4 complexity levels.
 *
 * Tests that require `protoc` are skipped gracefully (via Assumptions)
 * when protoc is not available on PATH.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProtoParserTest {

    private val parser = ProtoParser()
    private val testDataDir = File("src/test/testData/proto")
    private var protocAvailable = false

    @BeforeAll
    fun detectProtoc() {
        protocAvailable = try {
            val pb = ProcessBuilder("protoc", "--version").start()
            pb.waitFor(5, TimeUnit.SECONDS) && pb.exitValue() == 0
        } catch (_: Exception) {
            false
        }
        println("[ProtoParserTest] protoc available: $protocAvailable")
    }

    // ─── Complexity Level 1: Simple service ────────────────────────────────────

    @Test
    fun `level 1 - simple service with two methods is parsed correctly`() {
        assumeTrue(protocAvailable, "protoc not available — skipping proto parse test")

        val result = parser.parse(testDataDir.resolve("greeter.proto"))
        assertNull(result.error, "Parse error: ${result.error}")
        assertTrue(result.isSuccess)

        val service = result.services.single { it.name == "GreeterService" }
        assertEquals(2, service.methods.size, "GreeterService should have 2 methods")

        val sayHello = service.methods.single { it.name == "SayHello" }
        assertTrue(sayHello.isUnary, "SayHello should be a unary call")
        assertTrue(sayHello.inputTypeName.contains("HelloRequest"), "Input type mismatch")
        assertTrue(sayHello.outputTypeName.contains("HelloReply"), "Output type mismatch")
    }

    // ─── Complexity Level 2: Nested messages & repeated fields ─────────────────

    @Test
    fun `level 2 - nested messages and repeated fields are extracted`() {
        assumeTrue(protocAvailable, "protoc not available — skipping proto parse test")

        val result = parser.parse(testDataDir.resolve("nested.proto"))
        assertNull(result.error, "Parse error: ${result.error}")
        assertTrue(result.isSuccess)
        assertTrue(result.services.isNotEmpty())

        val service = result.services.single { it.name == "OrderService" }
        assertEquals(2, service.methods.size)

        val createOrder = service.methods.single { it.name == "CreateOrder" }
        assertTrue(createOrder.inputTypeName.contains("CreateOrderRequest"))
        assertTrue(createOrder.outputTypeName.contains("CreateOrderResponse"))
    }

    // ─── Complexity Level 3: Well-Known Types ─────────────────────────────────

    @Test
    fun `level 3 - well-known types (Timestamp, Struct, StringValue) are handled`() {
        assumeTrue(protocAvailable, "protoc not available — skipping proto parse test")

        val result = parser.parse(testDataDir.resolve("well_known.proto"))
        assertNull(result.error, "Parse error: ${result.error}")
        assertTrue(result.isSuccess)
        assertTrue(result.services.isNotEmpty())

        val service = result.services.single { it.name == "EventService" }
        assertEquals(2, service.methods.size)

        val recordEvent = service.methods.single { it.name == "RecordEvent" }
        assertTrue(recordEvent.inputTypeName.contains("RecordEventRequest"))
    }

    // ─── Complexity Level 4: Multiple services + enum ─────────────────────────

    @Test
    fun `level 4 - multiple services are all extracted from one file`() {
        assumeTrue(protocAvailable, "protoc not available — skipping proto parse test")

        val result = parser.parse(testDataDir.resolve("multi_service.proto"))
        assertNull(result.error, "Parse error: ${result.error}")
        assertTrue(result.isSuccess)

        val serviceNames = result.services.map { it.name }.toSet()
        assertTrue("UserService" in serviceNames, "UserService should be present, got: $serviceNames")
        assertTrue("AuthService" in serviceNames, "AuthService should be present, got: $serviceNames")

        val userService = result.services.single { it.name == "UserService" }
        assertEquals(4, userService.methods.size, "UserService should have 4 methods")

        val authService = result.services.single { it.name == "AuthService" }
        assertEquals(3, authService.methods.size, "AuthService should have 3 methods")
    }

    // ─── JSON ↔ DynamicMessage conversion ─────────────────────────────────────

    @Test
    fun `json-to-message and message-to-json round-trip for simple message`() {
        assumeTrue(protocAvailable, "protoc not available — skipping JSON round-trip test")

        val descriptorSet = buildDescriptorSet("greeter.proto")
        val fileDescriptors = loadDescriptors(descriptorSet)
        val greeterFd = fileDescriptors.values.first { it.name == "greeter.proto" }
        val helloRequestDesc = greeterFd.findMessageTypeByName("HelloRequest")
        assertNotNull(helloRequestDesc, "HelloRequest descriptor should exist")

        val json = """{"name": "Alice", "age": 30}"""
        val message: DynamicMessage = parser.jsonToMessage(json, helloRequestDesc!!)

        val roundTripped = parser.messageToJson(message)
        assertTrue(roundTripped.contains("\"name\": \"Alice\""), "name field should survive round-trip")
        assertTrue(roundTripped.contains("\"age\": 30"), "age field should survive round-trip")
    }

    @Test
    fun `generateJsonTemplate produces valid JSON with default values`() {
        assumeTrue(protocAvailable, "protoc not available — skipping template test")

        val descriptorSet = buildDescriptorSet("greeter.proto")
        val fileDescriptors = loadDescriptors(descriptorSet)
        val greeterFd = fileDescriptors.values.first { it.name == "greeter.proto" }
        val helloRequestDesc = greeterFd.findMessageTypeByName("HelloRequest")!!

        val template = parser.generateJsonTemplate(helloRequestDesc)
        assertTrue(template.contains("name"), "Template should have 'name' field")
        assertTrue(template.contains("age"), "Template should have 'age' field")
    }

    @Test
    fun `describeFields returns correct type descriptions`() {
        assumeTrue(protocAvailable, "protoc not available — skipping describeFields test")

        val descriptorSet = buildDescriptorSet("greeter.proto")
        val fileDescriptors = loadDescriptors(descriptorSet)
        val greeterFd = fileDescriptors.values.first { it.name == "greeter.proto" }
        val helloRequestDesc = greeterFd.findMessageTypeByName("HelloRequest")!!

        val fields = parser.describeFields(helloRequestDesc)
        assertEquals("string", fields["name"])
        assertEquals("int32", fields["age"])
    }

    @Test
    fun `parse returns error result for non-existent file`() {
        // This test does NOT require protoc — it tests file-not-found handling
        val result = parser.parse(File("/non/existent/path.proto"))
        assertFalse(result.isSuccess)
        assertNotNull(result.error)
        assertTrue(result.services.isEmpty())
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun buildDescriptorSet(protoFileName: String): File {
        val protoFile = testDataDir.resolve(protoFileName)
        val out = Files.createTempFile("test_desc_", ".pb").toFile()
        out.deleteOnExit()

        val cmd = listOf(
            "protoc",
            "--proto_path=${protoFile.parentFile.absolutePath}",
            "--descriptor_set_out=${out.absolutePath}",
            "--include_imports",
            protoFile.absolutePath
        )
        val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor(15, TimeUnit.SECONDS)
        check(process.exitValue() == 0) { "protoc failed for $protoFileName:\n$output" }
        return out
    }

    private fun loadDescriptors(descriptorSetFile: File): Map<String, FileDescriptor> {
        val fds = FileDescriptorSet.parseFrom(descriptorSetFile.readBytes())
        val protoByName = fds.fileList.associateBy { it.name }
        val resolved = mutableMapOf<String, FileDescriptor>()

        fun resolve(proto: FileDescriptorProto) {
            if (resolved.containsKey(proto.name)) return
            proto.dependencyList.mapNotNull { protoByName[it] }.forEach { resolve(it) }
            val deps = proto.dependencyList.mapNotNull { resolved[it] }.toTypedArray()
            resolved[proto.name] = FileDescriptor.buildFrom(proto, deps)
        }

        fds.fileList.forEach { resolve(it) }
        return resolved
    }
}
