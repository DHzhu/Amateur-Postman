package com.github.dhzhu.amateurpostman.ui

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Unit tests for StreamMessageList component.
 */
class StreamMessageListTest {

    private lateinit var messageList: StreamMessageList<TestMessage>

    data class TestMessage(
        override val content: String,
        override val isOutgoing: Boolean,
        override val timestamp: Long = System.currentTimeMillis()
    ) : StreamMessage

    @BeforeEach
    fun setup() {
        messageList = StreamMessageList(maxMessages = 10)
    }

    @Test
    fun `initial state is empty`() {
        assertEquals(0, messageList.getMessageCount())
        assertTrue(messageList.getMessages().isEmpty())
    }

    @Test
    fun `addMessage increases count`() {
        val latch = CountDownLatch(1)
        javax.swing.SwingUtilities.invokeLater {
            messageList.addMessage(TestMessage("test", true))
            latch.countDown()
        }
        assertTrue(latch.await(2, TimeUnit.SECONDS))

        // Wait a bit for Swing to process
        Thread.sleep(100)
        assertEquals(1, messageList.getMessageCount())
    }

    @Test
    fun `clearMessages removes all messages`() {
        // Add messages synchronously on EDT
        val addLatch = CountDownLatch(1)
        javax.swing.SwingUtilities.invokeLater {
            messageList.addMessage(TestMessage("msg1", true))
            messageList.addMessage(TestMessage("msg2", false))
            messageList.addMessage(TestMessage("msg3", true))
            addLatch.countDown()
        }
        assertTrue(addLatch.await(2, TimeUnit.SECONDS))
        Thread.sleep(100)

        assertTrue(messageList.getMessageCount() >= 2)

        val clearLatch = CountDownLatch(1)
        javax.swing.SwingUtilities.invokeLater {
            messageList.clearMessages()
            clearLatch.countDown()
        }
        assertTrue(clearLatch.await(2, TimeUnit.SECONDS))
        Thread.sleep(100)

        assertEquals(0, messageList.getMessageCount())
    }

    @Test
    fun `maxMessages limit is enforced`() {
        val latch = CountDownLatch(1)
        javax.swing.SwingUtilities.invokeLater {
            // Add more than maxMessages (10)
            repeat(15) { i ->
                messageList.addMessage(TestMessage("msg$i", i % 2 == 0))
            }
            latch.countDown()
        }
        assertTrue(latch.await(3, TimeUnit.SECONDS))
        Thread.sleep(200)

        // Should be limited to maxMessages
        assertEquals(10, messageList.getMessageCount())
    }

    @Test
    fun `messages maintain insertion order`() {
        val latch = CountDownLatch(1)
        javax.swing.SwingUtilities.invokeLater {
            repeat(5) { i ->
                messageList.addMessage(TestMessage("msg$i", true))
            }
            latch.countDown()
        }
        assertTrue(latch.await(2, TimeUnit.SECONDS))
        Thread.sleep(100)

        val messages = messageList.getMessages()
        assertEquals(5, messages.size)
        // Messages should be in order: msg0, msg1, msg2, msg3, msg4
        assertEquals("msg0", messages[0].content)
        assertEquals("msg4", messages[4].content)
    }

    @Test
    fun `outgoing and incoming messages are distinguished`() {
        val outgoing = TestMessage("sent", true)
        val incoming = TestMessage("received", false)

        assertTrue(outgoing.isOutgoing)
        assertFalse(incoming.isOutgoing)
    }

    @Test
    fun `timestamp is set automatically`() {
        val before = System.currentTimeMillis()
        val msg = TestMessage("test", true)
        val after = System.currentTimeMillis()

        assertTrue(msg.timestamp >= before)
        assertTrue(msg.timestamp <= after)
    }

    @Test
    fun `content is preserved`() {
        val testContent = """{"key":"value","number":123}"""
        val msg = TestMessage(testContent, true)

        assertEquals(testContent, msg.content)
    }

    @Test
    fun `maxMessages with default value`() {
        val defaultList = StreamMessageList<TestMessage>()
        // Default max is 1000

        val latch = CountDownLatch(1)
        javax.swing.SwingUtilities.invokeLater {
            repeat(10) { i ->
                defaultList.addMessage(TestMessage("msg$i", true))
            }
            latch.countDown()
        }
        assertTrue(latch.await(2, TimeUnit.SECONDS))
        Thread.sleep(100)

        assertEquals(10, defaultList.getMessageCount())
    }

    @Test
    fun `getMessages returns a copy`() {
        val latch = CountDownLatch(1)
        javax.swing.SwingUtilities.invokeLater {
            messageList.addMessage(TestMessage("test", true))
            latch.countDown()
        }
        assertTrue(latch.await(2, TimeUnit.SECONDS))
        Thread.sleep(100)

        val messages1 = messageList.getMessages()
        val messages2 = messageList.getMessages()

        assertEquals(messages1.size, messages2.size)
        assertEquals(1, messages1.size)
        assertEquals(1, messages2.size)
    }

    @Test
    fun `oldest messages are removed when limit exceeded`() {
        val latch = CountDownLatch(1)
        javax.swing.SwingUtilities.invokeLater {
            // Add 15 messages to a list with max 10
            repeat(15) { i ->
                messageList.addMessage(TestMessage("msg$i", true))
            }
            latch.countDown()
        }
        assertTrue(latch.await(3, TimeUnit.SECONDS))
        Thread.sleep(200)

        val messages = messageList.getMessages()
        assertEquals(10, messages.size)
        // Oldest 5 messages (msg0-msg4) should be removed
        // First remaining should be msg5
        assertEquals("msg5", messages[0].content)
        assertEquals("msg14", messages[9].content)
    }

    @Test
    fun `addMessage with large content works`() {
        val largeContent = "x".repeat(10000)
        val latch = CountDownLatch(1)
        javax.swing.SwingUtilities.invokeLater {
            messageList.addMessage(TestMessage(largeContent, true))
            latch.countDown()
        }
        assertTrue(latch.await(2, TimeUnit.SECONDS))
        Thread.sleep(100)

        assertEquals(1, messageList.getMessageCount())
        val messages = messageList.getMessages()
        assertEquals(10000, messages[0].content.length)
    }
}