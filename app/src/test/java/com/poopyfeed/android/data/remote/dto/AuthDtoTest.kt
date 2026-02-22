package com.poopyfeed.android.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `LoginRequest serializes correctly`() {
        val request = LoginRequest(email = "test@example.com", password = "password123")
        val serialized = json.encodeToString(LoginRequest.serializer(), request)
        assertEquals("""{"email":"test@example.com","password":"password123"}""", serialized)
    }

    @Test
    fun `LoginRequest deserializes correctly`() {
        val jsonStr = """{"email":"test@example.com","password":"password123"}"""
        val request = json.decodeFromString<LoginRequest>(jsonStr)
        assertEquals("test@example.com", request.email)
        assertEquals("password123", request.password)
    }

    @Test
    fun `SignupRequest serializes correctly`() {
        val request = SignupRequest(email = "new@example.com", password = "secret123")
        val serialized = json.encodeToString(SignupRequest.serializer(), request)
        assertEquals("""{"email":"new@example.com","password":"secret123"}""", serialized)
    }

    @Test
    fun `SignupRequest deserializes correctly`() {
        val jsonStr = """{"email":"new@example.com","password":"secret123"}"""
        val request = json.decodeFromString<SignupRequest>(jsonStr)
        assertEquals("new@example.com", request.email)
        assertEquals("secret123", request.password)
    }

    @Test
    fun `TokenResponse deserializes auth_token correctly`() {
        val jsonStr = """{"auth_token":"abc123token"}"""
        val response = json.decodeFromString<TokenResponse>(jsonStr)
        assertEquals("abc123token", response.authToken)
    }

    @Test
    fun `AllauthResponse deserializes correctly`() {
        val jsonStr = """{"status":200,"data":{"user":{"id":1,"email":"test@example.com"}}}"""
        val response = json.decodeFromString<AllauthResponse>(jsonStr)
        assertEquals(200, response.status)
        assertEquals(1, response.data.user.id)
        assertEquals("test@example.com", response.data.user.email)
    }

    @Test
    fun `AllauthResponse ignores unknown keys`() {
        val jsonStr = """{"status":200,"data":{"user":{"id":1,"email":"test@example.com","extra":"field"}},"meta":{}}"""
        val response = json.decodeFromString<AllauthResponse>(jsonStr)
        assertEquals(200, response.status)
        assertEquals("test@example.com", response.data.user.email)
    }
}
