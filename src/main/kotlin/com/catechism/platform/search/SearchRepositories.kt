package com.catechism.platform.search

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.elasticsearch.annotations.Query
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

interface QuestionSearchRepository : ElasticsearchRepository<QuestionDocument, String> {

    fun findByBookletId(bookletId: String, pageable: Pageable): Page<QuestionDocument>

    fun findByLanguageCode(languageCode: String, pageable: Pageable): Page<QuestionDocument>

    fun findByCategory(category: String, pageable: Pageable): Page<QuestionDocument>

    fun findByBookletIdAndLanguageCode(
        bookletId: String,
        languageCode: String,
        pageable: Pageable
    ): Page<QuestionDocument>

    @Query("""
        {
          "multi_match": {
            "query": "?0",
            "fields": ["questionText^3", "answerText^2", "category", "subtitleTitle", "actTitle"],
            "type": "best_fields",
            "fuzziness": "AUTO",
            "minimum_should_match": "75%"
          }
        }
    """)
    fun searchByText(query: String, pageable: Pageable): Page<QuestionDocument>
}

interface ExplanationSearchRepository : ElasticsearchRepository<ExplanationDocument, String> {

    fun findByQuestionId(questionId: String, pageable: Pageable): Page<ExplanationDocument>

    fun findBySubmissionStatusAndLanguageCode(
        status: String,
        languageCode: String,
        pageable: Pageable
    ): Page<ExplanationDocument>

    @Query("""
        {
          "bool": {
            "must": {
              "match": {
                "textContent": {
                  "query": "?0",
                  "fuzziness": "AUTO"
                }
              }
            },
            "filter": {
              "term": { "submissionStatus": "APPROVED" }
            }
          }
        }
    """)
    fun searchApprovedByText(query: String, pageable: Pageable): Page<ExplanationDocument>
}