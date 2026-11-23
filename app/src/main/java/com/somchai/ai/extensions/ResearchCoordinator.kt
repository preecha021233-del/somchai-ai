package com.somchai.ai.extensions

data class ResearchReport(
    val query: String,
    val discovered: List<ResearchCandidate>,
    val pagesRead: List<WebPageContent>,
    val sourceErrors: List<String>,
    val pageErrors: List<String>
) {
    val meetsTwoSourceMinimum: Boolean get() = pagesRead.map { it.url }.distinct().size >= 2
}

/**
 * Coordinates discovery and actual page reads. A search result is never counted
 * as a read source unless the page reader returns its content successfully.
 */
class ResearchCoordinator(
    private val discovery: ResearchDiscoveryProvider,
    private val pageReader: ResearchPageReader,
    private val maxPages: Int = 5
) {
    fun research(query: String): ResearchReport {
        val clean = query.trim()
        require(clean.isNotEmpty() && clean.length <= 300) { "Search query is invalid" }
        require(maxPages in 1..10) { "Invalid research page limit" }
        val result = discovery.discover(clean)
        val pages = mutableListOf<WebPageContent>()
        val errors = mutableListOf<String>()
        val readUrls = mutableSetOf<String>()
        for (candidate in result.candidates.distinctBy { canonicalUrlKey(it.url) }) {
            if (pages.size >= maxPages) break
            runCatching { pageReader.read(candidate.url) }
                .onSuccess { page ->
                    if (readUrls.add(canonicalUrlKey(page.url))) pages.add(page)
                }
                .onFailure { errors.add("${candidate.url}: ${it.javaClass.simpleName}") }
        }
        return ResearchReport(
            clean,
            result.candidates,
            pages,
            result.sourceErrors,
            errors
        )
    }
    /**
     * Fragments are client-side only and default ports/host casing do not identify
     * separate web pages. Canonicalize before reading/counting to prevent duplicate
     * search candidates from satisfying the two-source minimum.
     */
    private fun canonicalUrlKey(address: String): String = runCatching {
        val uri = java.net.URI(address).normalize()
        val scheme = uri.scheme?.lowercase() ?: return@runCatching address.substringBefore('#')
        val host = uri.host?.lowercase()?.trimEnd('.')
            ?: return@runCatching address.substringBefore('#')
        val port = when {
            scheme == "https" && uri.port == 443 -> -1
            scheme == "http" && uri.port == 80 -> -1
            else -> uri.port
        }
        java.net.URI(scheme, null, host, port, uri.path, uri.query, null).toASCIIString()
    }.getOrDefault(address.substringBefore('#'))

}
