package com.somchai.ai.extensions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WebSearchResultParserTest {
    private val reader = WebPageReader()

    @Test fun extractsPublicHttpsResultsAndDecodesRedirects() {
        val html = """
            <a class="result__a" href="https://example.org/article">Example article</a>
            <a class="result__a" href="https://duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.com%2Fpage">Second result</a>
        """.trimIndent()
        val results = WebSearchResultParser.parse(html, reader)
        assertEquals(2, results.size)
        assertEquals("https://example.org/article", results[0].url)
        assertEquals("https://example.com/page", results[1].url)
    }

    @Test fun rejectsHttpPrivateAndInvalidTargets() {
        val html = """
            <a class="result__a" href="http://example.org/insecure">Insecure</a>
            <a class="result__a" href="https://127.0.0.1/private">Private</a>
            <a class="result__a" href="https://example.net/safe">Safe</a>
        """.trimIndent()
        val results = WebSearchResultParser.parse(html, reader)
        assertEquals(1, results.size)
        assertTrue(results.single().url == "https://example.net/safe")
    }

    @Test fun ignoresDuplicateLinksAndHonorsLimit() {
        val html = """
            <a class="result__a" href="https://example.org/a">A one</a>
            <a class="result__a" href="https://example.org/a">A duplicate</a>
            <a class="result__a" href="https://example.org/b">B</a>
        """.trimIndent()
        val results = WebSearchResultParser.parse(html, reader, limit = 1)
        assertEquals(1, results.size)
        assertEquals("A one", results.single().title)
    }
}
