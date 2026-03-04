package net.poopyfeed.pf.data.models

import kotlin.test.assertEquals
import org.junit.Test

class ApiModelsTest {

  @Test
  fun `PaginatedResponse totalPages divides count by results size`() {
    val response = PaginatedResponse(count = 10, results = listOf("a", "b", "c", "d", "e"))
    assertEquals(2, response.totalPages)
  }

  @Test
  fun `PaginatedResponse totalPages rounds up`() {
    val response = PaginatedResponse(count = 11, results = listOf("a", "b", "c", "d", "e"))
    assertEquals(3, response.totalPages)
  }

  @Test
  fun `PaginatedResponse totalPages single page`() {
    val response = PaginatedResponse(count = 3, results = listOf(1, 2, 3))
    assertEquals(1, response.totalPages)
  }
}
