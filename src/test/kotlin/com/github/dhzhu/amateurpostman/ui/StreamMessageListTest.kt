package com.github.dhzhu.amateurpostman.ui

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

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

    /** Flush the EDT queue — addMessage/clearMessages use invokeLater internally. */
    private fun flushEdt() {
        javax.swing.SwingUtilities.invokeAndWait {}
    }

    @Test
    fun `initial state is empty`() {
        assertEquals(0, messageList.getMessageCount())
        assertTrue(messageList.getMessages().isEmpty())
    }

    @Test
    fun `addMessage increases count`() {
        javax.swing.SwingUtilities.invokeAndWait {
            messageList.addMessage(TestMessage("test", true))
        }
        flushEdt()
        assertEquals(1, messageList.getMessageCount())
    }

    @Test
    fun `clearMessages removes all messages`() {
        javax.swing.SwingUtilities.invokeAndWait {
            messageList.addMessage(TestMessage("msg1", true))
            messageList.addMessage(TestMessage("msg2", false))
            messageList.addMessage(TestMessage("msg3", true))
        }
        flushEdt()
        assertTrue(messageList.getMessageCount() >= 2)

        javax.swing.SwingUtilities.invokeAndWait {
            messageList.clearMessages()
        }
        flushEdt()
        assertEquals(0, messageList.getMessageCount())
    }

    @Test
    fun `maxMessages limit is enforced`() {
        javax.swing.SwingUtilities.invokeAndWait {
            repeat(15) { i ->
                messageList.addMessage(TestMessage("msg$i", i % 2 == 0))
            }
        }
        flushEdt()
        assertEquals(10, messageList.getMessageCount())
    }

    @Test
    fun `messages maintain insertion order`() {
        javax.swing.SwingUtilities.invokeAndWait {
            repeat(5) { i ->
                messageList.addMessage(TestMessage("msg$i", true))
            }
        }
        flushEdt()

        val messages = messageList.getMessages()
        assertEquals(5, messages.size)
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

        javax.swing.SwingUtilities.invokeAndWait {
            repeat(10) { i ->
                defaultList.addMessage(TestMessage("msg$i", true))
            }
        }
        flushEdt()
        assertEquals(10, defaultList.getMessageCount())
    }

    @Test
    fun `getMessages returns a copy`() {
        javax.swing.SwingUtilities.invokeAndWait {
            messageList.addMessage(TestMessage("test", true))
        }
        flushEdt()

        val messages1 = messageList.getMessages()
        val messages2 = messageList.getMessages()

        assertEquals(messages1.size, messages2.size)
        assertEquals(1, messages1.size)
        assertEquals(1, messages2.size)
    }

    @Test
    fun `oldest messages are removed when limit exceeded`() {
        javax.swing.SwingUtilities.invokeAndWait {
            repeat(15) { i ->
                messageList.addMessage(TestMessage("msg$i", true))
            }
        }
        flushEdt()

        val messages = messageList.getMessages()
        assertEquals(10, messages.size)
        assertEquals("msg5", messages[0].content)
        assertEquals("msg14", messages[9].content)
    }

    @Test
    fun `addMessage with large content works`() {
        val largeContent = "x".repeat(10000)
        javax.swing.SwingUtilities.invokeAndWait {
            messageList.addMessage(TestMessage(largeContent, true))
        }
        flushEdt()

        assertEquals(1, messageList.getMessageCount())
        val messages = messageList.getMessages()
        assertEquals(10000, messages[0].content.length)
    }
}
