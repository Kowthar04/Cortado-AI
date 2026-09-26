package com.example.cafeshopassignment.data.remote

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/** Parses the response shapes documented in backend/README.md. */
class CafeApiTest {
    private val server = MockWebServer()
    private lateinit var api: CafeApi

    @Before
    fun setUp() {
        server.start()
        api = RetrofitClient.create(server.url("/").toString(), { "id-token" })
    }

    @After
    fun tearDown() = server.shutdown()

    private fun json(body: String) = MockResponse().setHeader("Content-Type", "application/json").setBody(body)

    @Test
    fun `getMenu parses the menuItems envelope`() =
        runTest {
            server.enqueue(
                json("""{"menuItems":[{"id":"abc","name":"Latte","category":"Drinks","price":4.5,"availability":true}]}"""),
            )

            val item = api.getMenu().menuItems!!.single()

            assertEquals("Latte", item.name)
            assertEquals(4.5, item.price!!, 0.0)
            assertEquals("/api/menu", server.takeRequest().path)
        }

    @Test
    fun `getOrder parses the order envelope and escapes the id`() =
        runTest {
            server.enqueue(json("""{"order":{"id":"o 1","status":"Preparing","totalPrice":5.0}}"""))

            val order = api.getOrder("o 1").order!!

            assertEquals("Preparing", order.status)
            assertEquals("/api/orders/o%201", server.takeRequest().path)
        }
}
