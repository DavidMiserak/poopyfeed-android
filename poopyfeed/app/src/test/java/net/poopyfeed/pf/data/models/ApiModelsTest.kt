package net.poopyfeed.pf.data.models

import kotlin.test.assertEquals
import org.junit.Test

class ApiModelsTest {

  @Test
  fun `PaginatedResponse totalPages divides count by page size`() {
    val response = PaginatedResponse(count = 20, results = (1..20).toList())
    assertEquals(1, response.totalPages)
  }

  @Test
  fun `PaginatedResponse totalPages rounds up`() {
    val response = PaginatedResponse(count = 21, results = (1..20).toList())
    assertEquals(2, response.totalPages)
  }

  @Test
  fun `PaginatedResponse totalPages single page`() {
    val response = PaginatedResponse(count = 3, results = listOf(1, 2, 3))
    assertEquals(1, response.totalPages)
  }

  @Test
  fun `PaginatedResponse totalPages zero when count zero`() {
    val response = PaginatedResponse(count = 0, results = emptyList<String>())
    assertEquals(0, response.totalPages)
  }

  @Test
  fun `PaginatedResponse totalPages last page partial`() {
    val response = PaginatedResponse(count = 25, results = (1..5).toList())
    assertEquals(2, response.totalPages)
  }
}
