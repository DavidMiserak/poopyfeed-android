package net.poopyfeed.pf.data.models

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class ApiModelsTest {

  @Test
  fun `PaginatedResponse hasNextPage true when next is non-null`() {
    val response =
        PaginatedResponse(
            count = 25, next = "http://api/children/?page=2", results = (1..20).toList())
    assertTrue(response.hasNextPage)
  }

  @Test
  fun `PaginatedResponse hasNextPage false when next is null`() {
    val response = PaginatedResponse(count = 3, next = null, results = listOf(1, 2, 3))
    assertFalse(response.hasNextPage)
  }

  @Test
  fun `PaginatedResponse hasNextPage false when count zero and next null`() {
    val response = PaginatedResponse(count = 0, next = null, results = emptyList<String>())
    assertFalse(response.hasNextPage)
  }
}
