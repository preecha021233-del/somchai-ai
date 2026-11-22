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
        result.candidates.distinctBy { it.url }.take(maxPages).forEach { candidate ->
            runCatching { pageReader.read(candidate.url) }
                .onSuccess { page ->
                    if (page.url !in pages.map { it.url }) pages.add(page)
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
}
