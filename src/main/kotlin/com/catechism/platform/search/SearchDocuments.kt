package com.catechism.platform.search

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.*
import java.time.Instant
import java.util.UUID

// =====================================================
// QUESTION DOCUMENT
// =====================================================

@Document(indexName = "catechism_questions")
@Setting(settingPath = "/elasticsearch/question-settings.json")
data class QuestionDocument(
    @Id
    val id: String,

    @Field(type = FieldType.Keyword)
    val bookletId: String,

    @Field(type = FieldType.Keyword)
    val bookletName: String,

    @Field(type = FieldType.Integer)
    val questionNumber: Int,

    @Field(type = FieldType.Keyword)
    val category: String? = null,

    @Field(type = FieldType.Keyword)
    val subtitleTitle: String? = null,

    @Field(type = FieldType.Keyword)
    val actTitle: String? = null,

    // Multilingual text fields — analysed for full-text search
    @MultiField(
        mainField = Field(type = FieldType.Text, analyzer = "catechism_analyzer"),
        otherFields = [
            InnerField(suffix = "keyword", type = FieldType.Keyword)
        ]
    )
    val questionText: String,

    @MultiField(
        mainField = Field(type = FieldType.Text, analyzer = "catechism_analyzer"),
        otherFields = [
            InnerField(suffix = "keyword", type = FieldType.Keyword)
        ]
    )
    val answerText: String,

    @Field(type = FieldType.Keyword)
    val languageCode: String,

    // CCC and Bible reference numbers for filtering
    @Field(type = FieldType.Integer)
    val cccParagraphNumbers: List<Int> = emptyList(),

    @Field(type = FieldType.Keyword)
    val bibleBooks: List<String> = emptyList(),

    @Field(type = FieldType.Date)
    val indexedAt: Instant = Instant.now()
)

// =====================================================
// EXPLANATION DOCUMENT
// =====================================================

@Document(indexName = "catechism_explanations")
data class ExplanationDocument(
    @Id
    val id: String,

    @Field(type = FieldType.Keyword)
    val questionId: String,

    @Field(type = FieldType.Integer)
    val questionNumber: Int,

    @Field(type = FieldType.Keyword)
    val submitterId: String,

    @Field(type = FieldType.Keyword)
    val submitterName: String,

    @Field(type = FieldType.Keyword)
    val languageCode: String,

    @Field(type = FieldType.Keyword)
    val contentType: String,  // TEXT, AUDIO, VIDEO

    @Field(type = FieldType.Text, analyzer = "catechism_analyzer")
    val textContent: String? = null,

    @Field(type = FieldType.Keyword)
    val submissionStatus: String,

    @Field(type = FieldType.Integer)
    val qualityScore: Int? = null,

    @Field(type = FieldType.Integer)
    val helpfulCount: Int = 0,

    @Field(type = FieldType.Date)
    val approvedAt: Instant? = null,

    @Field(type = FieldType.Date)
    val indexedAt: Instant = Instant.now()
)