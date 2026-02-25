package com.filenest.photo.data.api

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

class RetKtTest {

    @Test
    @DisplayName("isRetOk should return true when code is 200")
    fun `isRetOk should return true when code is 200`() {
        // Given
        val ret = Ret(code = 200, data = "test")

        // When
        val result = isRetOk(ret)

        // Then
        assertTrue(result)
    }

    @Test
    @DisplayName("isRetOk should return false when code is not 200")
    fun `isRetOk should return false when code is not 200`() {
        // Given
        val ret = Ret(code = 400, message = "Bad Request")

        // When
        val result = isRetOk(ret)

        // Then
        assertFalse(result)
    }

    @Test
    @DisplayName("isRetOk should return false when ret is null")
    fun `isRetOk should return false when ret is null`() {
        // When
        val result = isRetOk(null)

        // Then
        assertFalse(result)
    }

    @Test
    @DisplayName("retMsg should return message when present")
    fun `retMsg should return message when present`() {
        // Given
        val ret = Ret<Any?>(code = 400, message = "Error message")

        // When
        val result = retMsg(ret)

        // Then
        assertEquals("Error message", result)
    }

    @Test
    @DisplayName("retMsg should return default message when message is null")
    fun `retMsg should return default message when message is null`() {
        // Given
        val ret = Ret<Any?>(code = 200, data = "success")

        // When
        val result = retMsg(ret)

        // Then
        assertEquals("未知异常", result)
    }
}
