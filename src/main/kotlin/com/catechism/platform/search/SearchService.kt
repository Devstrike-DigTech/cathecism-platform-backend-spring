package com.catechism.platform.search

import com.catechism.platform.domain.explanation.ExplanationStatus
import com.catechism.platform.repository.*
import com.catechism.platform.repository.explanation.ExplanationSubmissionRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.elasticsearch.core.ElasticsearchOperations
import org.springframework.data.elasticsearch.core.SearchHit
import org.springframework.data.elasticsearch.core.query.Criteria
import org.springframework.data.elasticsearch.core.query.CriteriaQuery
import org.springframework.data.elasticsearch.core.query.StringQuery
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@ConditionalOnProperty(name = ["search.enabled"], havingValue = "true", matchIfMissing = false)
class SearchService(
    private val questionSearchRepo: QuestionSearchRepository,
    private val explanationSearchRepo: ExplanationSearchRepository,
    private val questionRepository: CatechismQuestionRepository,
    private val questionTranslationRepository: CatechismQuestionTranslationRepository,
    private val explanationRepository: ExplanationSubmissionRepository,
    private val elasticsearchOperations: ElasticsearchOperations
) {

    // =====================================================
    // SEARCH — QUESTIONS
    // =====================================================

    /**
     * Full-text search across question and answer text.
     * Supports fuzzy matching and catechism synonyms (e.g. "god" matches "lord").
     */
    fun searchQuestions(request: SearchRequest): SearchResponse<QuestionDocument> {
        val pageable = PageRequest.of(
            request.page,
            request.size,
            Sort.by(Sort.Direction.DESC, "_score")
        )

        val query = buildQuestionQuery(request)
        val hits = elasticsearchOperations.search(query, QuestionDocument::class.java)

        return SearchResponse(
            results = hits.searchHits.map { it.content },
            totalHits = hits.totalHits,
            page = request.page,
            size = request.size,
            query = request.query,
            facets = buildQuestionFacets(hits.searchHits.map { it.content })
        )
    }

    private fun buildQuestionQuery(request: SearchRequest): StringQuery {
        val mustClauses = mutableListOf<String>()
        val filterClauses = mutableListOf<String>()

        // Full-text match
        if (request.query.isNotBlank()) {
            mustClauses.add("""
                {
                  "multi_match": {
                    "query": "${request.query.escapeJson()}",
                    "fields": ["questionText^3", "answerText^2", "category", "subtitleTitle", "actTitle"],
                    "type": "best_fields",
                    "fuzziness": "AUTO",
                    "minimum_should_match": "75%"
                  }
                }
            """.trimIndent())
        }

        // Filters
        request.bookletId?.let {
            filterClauses.add("""{"term": {"bookletId": "$it"}}""")
        }
        request.languageCode?.let {
            filterClauses.add("""{"term": {"languageCode": "$it"}}""")
        }
        request.category?.let {
            filterClauses.add("""{"term": {"category": "${it.escapeJson()}"}}""")
        }
        request.cccParagraph?.let {
            filterClauses.add("""{"term": {"cccParagraphNumbers": $it}}""")
        }
        request.bibleBook?.let {
            filterClauses.add("""{"term": {"bibleBooks": "${it.escapeJson()}"}}""")
        }

        val mustJson = if (mustClauses.isEmpty()) """{"match_all": {}}"""
        else mustClauses.joinToString(",")

        val filterJson = if (filterClauses.isEmpty()) ""
        else """, "filter": [${filterClauses.joinToString(",")}]"""

        val json = """
            {
              "bool": {
                "must": [$mustJson]
                $filterJson
              }
            }
        """.trimIndent()

        val pageable = PageRequest.of(request.page, request.size)
        return StringQuery(json, pageable)
    }

    private fun buildQuestionFacets(results: List<QuestionDocument>): Map<String, List<FacetEntry>> {
        return mapOf(
            "category" to results.groupingBy { it.category ?: "Uncategorised" }
                .eachCount()
                .map { FacetEntry(it.key, it.value) }
                .sortedByDescending { it.count },
            "language" to results.groupingBy { it.languageCode }
                .eachCount()
                .map { FacetEntry(it.key, it.value) }
                .sortedByDescending { it.count },
            "act" to results.groupingBy { it.actTitle ?: "No Act" }
                .eachCount()
                .map { FacetEntry(it.key, it.value) }
                .sortedByDescending { it.count }
        )
    }

    // =====================================================
    // SEARCH — EXPLANATIONS
    // =====================================================

    /**
     * Full-text search over approved explanation text content.
     */
    fun searchExplanations(request: SearchRequest): SearchResponse<ExplanationDocument> {
        val pageable = PageRequest.of(request.page, request.size)

        val page = if (request.query.isBlank()) {
            val status = request.status ?: "APPROVED"
            val lang = request.languageCode ?: "en"
            explanationSearchRepo.findBySubmissionStatusAndLanguageCode(status, lang, pageable)
        } else {
            explanationSearchRepo.searchApprovedByText(request.query, pageable)
        }

        return SearchResponse(
            results = page.content,
            totalHits = page.totalElements,
            page = request.page,
            size = request.size,
            query = request.query,
            facets = mapOf(
                "contentType" to page.content
                    .groupingBy { it.contentType }
                    .eachCount()
                    .map { FacetEntry(it.key, it.value) }
                    .sortedByDescending { it.count }
            )
        )
    }

    // =====================================================
    // SUGGESTIONS (autocomplete)
    // =====================================================

    /**
     * Returns up to `limit` question text suggestions starting with the given prefix.
     * Used for search-as-you-type / autocomplete.
     */
    fun suggestQuestions(prefix: String, languageCode: String = "en", limit: Int = 5): List<String> {
        if (prefix.length < 2) return emptyList()

        val json = """
            {
              "bool": {
                "must": {
                  "match_phrase_prefix": {
                    "questionText": {
                      "query": "${prefix.escapeJson()}",
                      "max_expansions": 10
                    }
                  }
                },
                "filter": {"term": {"languageCode": "$languageCode"}}
              }
            }
        """.trimIndent()

        val pageable = PageRequest.of(0, limit)
        val hits = elasticsearchOperations.search(
            StringQuery(json, pageable),
            QuestionDocument::class.java
        )

        return hits.searchHits.map { it.content.questionText }.distinct()
    }

    // =====================================================
    // INDEXING
    // =====================================================

    /**
     * Index a single question (all its translations).
     * Called after create/update.
     */
    @Transactional(readOnly = true)
    fun indexQuestion(questionId: UUID) {
        val question = questionRepository.findById(questionId).orElse(null) ?: return
        val translations = questionTranslationRepository.findByQuestionId(questionId)

        val cccNumbers = question.cccReferences
            .map { it.cccParagraph.paragraphNumber }
        val bibleBooks = question.bibleReferences
            .map { it.bibleReference.book }

        translations.forEach { translation ->
            val doc = QuestionDocument(
                id = "${questionId}_${translation.languageCode}",
                bookletId = question.booklet.id.toString(),
                bookletName = question.booklet.name,
                questionNumber = question.questionNumber,
                category = question.category,
                subtitleTitle = question.subtitle?.let { sub ->
                    sub.translations.find { it.languageCode == translation.languageCode }?.title
                        ?: sub.translations.firstOrNull()?.title
                },
                actTitle = question.subtitle?.act?.let { act ->
                    act.translations.find { it.languageCode == translation.languageCode }?.title
                        ?: act.translations.firstOrNull()?.title
                },
                questionText = translation.questionText,
                answerText = translation.answerText,
                languageCode = translation.languageCode,
                cccParagraphNumbers = cccNumbers,
                bibleBooks = bibleBooks
            )
            questionSearchRepo.save(doc)
        }
    }

    /**
     * Index a single approved explanation.
     */
    @Transactional(readOnly = true)
    fun indexExplanation(explanationId: UUID) {
        val explanation = explanationRepository.findById(explanationId).orElse(null) ?: return

        // Only index approved explanations
        if (explanation.submissionStatus != ExplanationStatus.APPROVED) return

        val doc = ExplanationDocument(
            id = explanationId.toString(),
            questionId = explanation.question.id.toString(),
            questionNumber = explanation.question.questionNumber,
            submitterId = explanation.submitter.id.toString(),
            submitterName = explanation.submitter.name,
            languageCode = explanation.languageCode,
            contentType = explanation.contentType.name,
            textContent = explanation.textContent,
            submissionStatus = explanation.submissionStatus.name,
            qualityScore = explanation.qualityScore,
            helpfulCount = explanation.helpfulCount,
            approvedAt = explanation.approvedAt
        )
        explanationSearchRepo.save(doc)
    }

    /**
     * Remove an explanation from the index (e.g. when rejected or deleted).
     */
    fun removeExplanation(explanationId: UUID) {
        explanationSearchRepo.deleteById(explanationId.toString())
    }

    /**
     * Remove a question from the index (all language variants).
     */
    fun removeQuestion(questionId: UUID) {
        val translations = questionTranslationRepository.findByQuestionId(questionId)
        translations.forEach { translation ->
            questionSearchRepo.deleteById("${questionId}_${translation.languageCode}")
        }
    }

    /**
     * Full reindex of all questions and approved explanations.
     * Admin operation — potentially slow on large datasets.
     */
    @Transactional(readOnly = true)
    fun reindexAll(): ReindexResult {
        var questionsIndexed = 0
        var explanationsIndexed = 0

        // Reindex all questions
        questionRepository.findAll().forEach { question ->
            indexQuestion(question.id)
            questionsIndexed++
        }

        // Reindex all approved explanations
        explanationRepository.findBySubmissionStatus(ExplanationStatus.APPROVED).forEach { explanation ->
            indexExplanation(explanation.id)
            explanationsIndexed++
        }

        return ReindexResult(
            questionsIndexed = questionsIndexed,
            explanationsIndexed = explanationsIndexed,
            success = true
        )
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private fun String.escapeJson() = this
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
}

// =====================================================
// REQUEST / RESPONSE TYPES
// =====================================================

data class SearchRequest(
    val query: String = "",
    val bookletId: String? = null,
    val languageCode: String? = null,
    val category: String? = null,
    val cccParagraph: Int? = null,
    val bibleBook: String? = null,
    val status: String? = null,
    val page: Int = 0,
    val size: Int = 20
)

data class SearchResponse<T>(
    val results: List<T>,
    val totalHits: Long,
    val page: Int,
    val size: Int,
    val query: String,
    val facets: Map<String, List<FacetEntry>> = emptyMap()
) {
    val totalPages: Int get() = if (size == 0) 0 else ((totalHits + size - 1) / size).toInt()
    val hasMore: Boolean get() = page < totalPages - 1
}

data class FacetEntry(
    val value: String,
    val count: Int
)

data class ReindexResult(
    val questionsIndexed: Int,
    val explanationsIndexed: Int,
    val success: Boolean
)