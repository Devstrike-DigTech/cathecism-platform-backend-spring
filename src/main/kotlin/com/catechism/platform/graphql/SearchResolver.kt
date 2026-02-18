package com.catechism.platform.graphql

import com.catechism.platform.search.*
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Controller
import java.util.UUID

@Controller
@ConditionalOnProperty(name = ["search.enabled"], havingValue = "true", matchIfMissing = false)
class SearchResolver(
    private val searchService: SearchService
) {

    @QueryMapping
    fun searchQuestions(@Argument input: SearchInputDTO): QuestionSearchResultDTO {
        val request = SearchRequest(
            query       = input.query ?: "",
            bookletId   = input.bookletId?.toString(),
            languageCode = input.languageCode,
            category    = input.category,
            cccParagraph = input.cccParagraph,
            bibleBook   = input.bibleBook,
            page        = input.page ?: 0,
            size        = (input.size ?: 20).coerceIn(1, 50)
        )

        val response = searchService.searchQuestions(request)

        return QuestionSearchResultDTO(
            results    = response.results.map { it.toHitDTO() },
            totalHits  = response.totalHits,
            page       = response.page,
            size       = response.size,
            totalPages = response.totalPages,
            hasMore    = response.hasMore,
            query      = response.query,
            facets     = response.facets.map { (name, buckets) ->
                FacetGroupDTO(name = name, buckets = buckets.map { FacetEntryDTO(it.value, it.count) })
            }
        )
    }

    @QueryMapping
    fun searchExplanations(@Argument input: SearchInputDTO): ExplanationSearchResultDTO {
        val request = SearchRequest(
            query        = input.query ?: "",
            languageCode = input.languageCode,
            status       = "APPROVED",
            page         = input.page ?: 0,
            size         = (input.size ?: 20).coerceIn(1, 50)
        )

        val response = searchService.searchExplanations(request)

        return ExplanationSearchResultDTO(
            results    = response.results.map { it.toHitDTO() },
            totalHits  = response.totalHits,
            page       = response.page,
            size       = response.size,
            totalPages = response.totalPages,
            hasMore    = response.hasMore,
            query      = response.query,
            facets     = response.facets.map { (name, buckets) ->
                FacetGroupDTO(name = name, buckets = buckets.map { FacetEntryDTO(it.value, it.count) })
            }
        )
    }

    @QueryMapping
    fun searchSuggestions(
        @Argument prefix: String,
        @Argument languageCode: String = "en",
        @Argument limit: Int = 5
    ): List<String> {
        return searchService.suggestQuestions(prefix, languageCode, limit.coerceIn(1, 10))
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun reindexAll(): ReindexResultDTO {
        val result = searchService.reindexAll()
        return ReindexResultDTO(
            questionsIndexed     = result.questionsIndexed,
            explanationsIndexed  = result.explanationsIndexed,
            success              = result.success
        )
    }

    // =====================================================
    // Mappers
    // =====================================================

    private fun QuestionDocument.toHitDTO() = QuestionSearchHitDTO(
        id                  = UUID.fromString(id.substringBefore("_")),
        bookletName         = bookletName,
        questionNumber      = questionNumber,
        category            = category,
        subtitleTitle       = subtitleTitle,
        actTitle            = actTitle,
        questionText        = questionText,
        answerText          = answerText,
        languageCode        = languageCode,
        cccParagraphNumbers = cccParagraphNumbers,
        bibleBooks          = bibleBooks
    )

    private fun ExplanationDocument.toHitDTO() = ExplanationSearchHitDTO(
        id             = UUID.fromString(id),
        questionId     = UUID.fromString(questionId),
        questionNumber = questionNumber,
        submitterName  = submitterName,
        languageCode   = languageCode,
        contentType    = contentType,
        textContent    = textContent,
        qualityScore   = qualityScore,
        helpfulCount   = helpfulCount
    )
}

// =====================================================
// DTOs
// =====================================================

data class SearchInputDTO(
    val query: String? = null,
    val bookletId: UUID? = null,
    val languageCode: String? = null,
    val category: String? = null,
    val cccParagraph: Int? = null,
    val bibleBook: String? = null,
    val page: Int? = null,
    val size: Int? = null
)

data class FacetEntryDTO(val value: String, val count: Int)
data class FacetGroupDTO(val name: String, val buckets: List<FacetEntryDTO>)

data class QuestionSearchHitDTO(
    val id: UUID,
    val bookletName: String,
    val questionNumber: Int,
    val category: String?,
    val subtitleTitle: String?,
    val actTitle: String?,
    val questionText: String,
    val answerText: String,
    val languageCode: String,
    val cccParagraphNumbers: List<Int>,
    val bibleBooks: List<String>
)

data class QuestionSearchResultDTO(
    val results: List<QuestionSearchHitDTO>,
    val totalHits: Long,
    val page: Int,
    val size: Int,
    val totalPages: Int,
    val hasMore: Boolean,
    val query: String,
    val facets: List<FacetGroupDTO>
)

data class ExplanationSearchHitDTO(
    val id: UUID,
    val questionId: UUID,
    val questionNumber: Int,
    val submitterName: String,
    val languageCode: String,
    val contentType: String,
    val textContent: String?,
    val qualityScore: Int?,
    val helpfulCount: Int
)

data class ExplanationSearchResultDTO(
    val results: List<ExplanationSearchHitDTO>,
    val totalHits: Long,
    val page: Int,
    val size: Int,
    val totalPages: Int,
    val hasMore: Boolean,
    val query: String,
    val facets: List<FacetGroupDTO>
)

data class ReindexResultDTO(
    val questionsIndexed: Int,
    val explanationsIndexed: Int,
    val success: Boolean
)