package com.catechism.platform.graphql

import com.catechism.platform.service.QuestionService
import com.catechism.platform.service.QuestionWithTranslation
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.util.*

@Controller
class QuestionResolver(
    private val questionService: QuestionService
) {

    @QueryMapping
    fun question(@Argument id: UUID, @Argument language: String = "en"): QuestionDTO? {
        val result = questionService.getQuestionById(id, language) ?: return null
        return result.toDTO(language)
    }

    @QueryMapping
    fun questions(@Argument bookletId: UUID, @Argument language: String = "en"): List<QuestionDTO> {
        return questionService.getQuestionsByBooklet(bookletId, language)
            .map { it.toDTO(language) }
    }

    @QueryMapping
    fun questionsBySubtitle(@Argument subtitleId: UUID, @Argument language: String = "en"): List<QuestionDTO> {
        return questionService.getQuestionsBySubtitle(subtitleId, language)
            .map { it.toDTO(language) }
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun createQuestion(@Argument input: CreateQuestionInput): QuestionDTO {
        val question = questionService.createQuestion(
            bookletId = input.bookletId,
            subtitleId = input.subtitleId,
            questionNumber = input.questionNumber,
            category = input.category,
            languageCode = input.language ?: "en",
            questionText = input.questionText,
            answerText = input.answerText,
            isOfficial = input.isOfficial ?: true
        )

        // Return the created question with translation
        val result = questionService.getQuestionById(question.id, input.language ?: "en")
            ?: throw IllegalStateException("Question was created but not found")

        return result.toDTO(input.language ?: "en")
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun updateQuestion(@Argument id: UUID, @Argument input: UpdateQuestionInput): QuestionDTO {
        // Update question metadata
        val question = questionService.updateQuestion(
            id = id,
            subtitleId = input.subtitleId,
            category = input.category
        )

        // Update translation if provided
        if (input.language != null && (input.questionText != null || input.answerText != null)) {
            questionService.updateQuestionTranslation(
                questionId = id,
                languageCode = input.language,
                questionText = input.questionText,
                answerText = input.answerText
            )
        }

        // Return updated question
        val result = questionService.getQuestionById(id, input.language ?: "en")
            ?: throw IllegalStateException("Question not found after update")

        return result.toDTO(input.language ?: "en")
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteQuestion(@Argument id: UUID): Boolean {
        return questionService.deleteQuestion(id)
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun linkQuestionToCCC(
        @Argument questionId: UUID,
        @Argument cccParagraphNumber: Int,
        @Argument order: Int = 0
    ): QuestionDTO {
        questionService.linkToCCCParagraph(questionId, cccParagraphNumber, order)

        val result = questionService.getQuestionById(questionId, "en")
            ?: throw IllegalStateException("Question not found after linking")

        return result.toDTO("en")
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun linkQuestionToBible(
        @Argument questionId: UUID,
        @Argument bibleReferenceId: UUID,
        @Argument order: Int = 0
    ): QuestionDTO {
        questionService.linkToBibleReference(questionId, bibleReferenceId, order)

        val result = questionService.getQuestionById(questionId, "en")
            ?: throw IllegalStateException("Question not found after linking")

        return result.toDTO("en")
    }

    // Helper extension function
    private fun QuestionWithTranslation.toDTO(language: String): QuestionDTO {
        return QuestionDTO(
            id = question.id,
            number = question.questionNumber,
            text = translation.questionText,
            answer = translation.answerText,
            category = question.category,
            translationOfficial = translation.isOfficial,
            availableLanguages = questionService.getAvailableLanguages(question.id)
        )
    }
}

// DTOs
data class QuestionDTO(
    val id: UUID,
    val number: Int,
    val text: String,
    val answer: String,
    val category: String?,
    val translationOfficial: Boolean,
    val availableLanguages: List<String>
)

data class CreateQuestionInput(
    val bookletId: UUID,
    val subtitleId: UUID?,
    val questionNumber: Int,
    val category: String?,
    val language: String?,
    val questionText: String,
    val answerText: String,
    val isOfficial: Boolean?
)

data class UpdateQuestionInput(
    val subtitleId: UUID?,
    val category: String?,
    val language: String?,
    val questionText: String?,
    val answerText: String?
)