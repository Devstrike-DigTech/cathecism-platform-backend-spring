package com.catechism.platform.service

import com.catechism.platform.domain.*
import com.catechism.platform.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
@Transactional
class QuestionService(
    private val questionRepository: CatechismQuestionRepository,
    private val questionTranslationRepository: CatechismQuestionTranslationRepository,
    private val bookletRepository: CatechismBookletRepository,
    private val subtitleRepository: CatechismActSubtitleRepository,
    private val cccReferenceRepository: QuestionCCCReferenceRepository,
    private val bibleReferenceRepository: QuestionBibleReferenceRepository,
    private val cccParagraphRepository: CCCParagraphRepository,
    private val bibleRefRepository: BibleReferenceRepository
) {

    /**
     * Get all questions for a booklet in a specific language
     */
    fun getQuestionsByBooklet(bookletId: UUID, languageCode: String = "en"): List<QuestionWithTranslation> {
        val questions = questionRepository.findByBookletId(bookletId)
        return questions.mapNotNull { question ->
            val translation = getTranslationOrFallback(question.id, languageCode)
            translation?.let {
                QuestionWithTranslation(
                    question = question,
                    translation = it
                )
            }
        }
    }

    /**
     * Get questions by subtitle
     */
    fun getQuestionsBySubtitle(subtitleId: UUID, languageCode: String = "en"): List<QuestionWithTranslation> {
        val questions = questionRepository.findAll().filter { it.subtitle?.id == subtitleId }
        return questions.mapNotNull { question ->
            val translation = getTranslationOrFallback(question.id, languageCode)
            translation?.let {
                QuestionWithTranslation(
                    question = question,
                    translation = it
                )
            }
        }
    }

    /**
     * Get a single question with full details
     */
    fun getQuestionById(id: UUID, languageCode: String = "en"): QuestionWithTranslation? {
        val question = questionRepository.findById(id).orElse(null) ?: return null
        val translation = getTranslationOrFallback(id, languageCode) ?: return null

        return QuestionWithTranslation(
            question = question,
            translation = translation
        )
    }

    /**
     * Create a new question with translation
     */
    fun createQuestion(
        bookletId: UUID,
        subtitleId: UUID?,
        questionNumber: Int,
        category: String?,
        languageCode: String,
        questionText: String,
        answerText: String,
        isOfficial: Boolean = true
    ): CatechismQuestion {
        // Verify booklet exists
        val booklet = bookletRepository.findById(bookletId).orElseThrow {
            IllegalArgumentException("Booklet not found: $bookletId")
        }

        // Verify subtitle exists if provided
        val subtitle = subtitleId?.let {
            subtitleRepository.findById(it).orElseThrow {
                IllegalArgumentException("Subtitle not found: $it")
            }
        }

        // Check if question number is unique within booklet
        val existing = questionRepository.findAll()
            .find { it.booklet.id == bookletId && it.questionNumber == questionNumber }

        if (existing != null) {
            throw IllegalArgumentException("Question number $questionNumber already exists in this booklet")
        }

        // Create question
        val question = CatechismQuestion(
            booklet = booklet,
            questionNumber = questionNumber,
            category = category,
            subtitle = subtitle
        )
        val savedQuestion = questionRepository.save(question)

        // Create translation
        val translation = CatechismQuestionTranslation(
            question = savedQuestion,
            languageCode = languageCode,
            questionText = questionText,
            answerText = answerText,
            isOfficial = isOfficial
        )
        questionTranslationRepository.save(translation)

        return savedQuestion
    }

    /**
     * Update question (move to different subtitle, change category)
     */
    fun updateQuestion(
        id: UUID,
        subtitleId: UUID?,
        category: String?
    ): CatechismQuestion {
        val question = questionRepository.findById(id).orElseThrow {
            IllegalArgumentException("Question not found: $id")
        }

        // Update subtitle if provided
        if (subtitleId != null) {
            val subtitle = subtitleRepository.findById(subtitleId).orElseThrow {
                IllegalArgumentException("Subtitle not found: $subtitleId")
            }
            // Use reflection or create a copy to update
            // Since data classes are immutable, we need to save a new instance
            val updated = question.copy(
                subtitle = subtitle,
                category = category ?: question.category,
                updatedAt = Instant.now()
            )
            return questionRepository.save(updated)
        }

        // Just update category
        if (category != null) {
            val updated = question.copy(
                category = category,
                updatedAt = Instant.now()
            )
            return questionRepository.save(updated)
        }

        return question
    }

    /**
     * Update question translation
     */
    fun updateQuestionTranslation(
        questionId: UUID,
        languageCode: String,
        questionText: String?,
        answerText: String?
    ): CatechismQuestionTranslation {
        val question = questionRepository.findById(questionId).orElseThrow {
            IllegalArgumentException("Question not found: $questionId")
        }

        val translation = questionTranslationRepository
            .findByQuestionIdAndLanguageCode(questionId, languageCode)
            ?: throw IllegalArgumentException("Translation not found for language: $languageCode")

        translation.questionText = questionText ?: translation.questionText
        translation.answerText = answerText ?: translation.answerText
        translation.updatedAt = Instant.now()

        return questionTranslationRepository.save(translation)
    }

    /**
     * Delete a question
     */
    fun deleteQuestion(id: UUID): Boolean {
        if (!questionRepository.existsById(id)) {
            return false
        }
        questionRepository.deleteById(id)
        return true
    }

    /**
     * Link question to CCC paragraph
     */
    fun linkToCCCParagraph(
        questionId: UUID,
        cccParagraphNumber: Int,
        order: Int = 0
    ): CatechismQuestion {
        val question = questionRepository.findById(questionId).orElseThrow {
            IllegalArgumentException("Question not found: $questionId")
        }

        val cccParagraph = cccParagraphRepository.findByParagraphNumber(cccParagraphNumber)
            ?: throw IllegalArgumentException("CCC paragraph not found: $cccParagraphNumber")

        // Check if link already exists
        val existingLink = cccReferenceRepository.findByQuestionIdOrderByReferenceOrder(questionId)
            .find { it.cccParagraph.id == cccParagraph.id }

        if (existingLink != null) {
            throw IllegalArgumentException("Question already linked to CCC paragraph $cccParagraphNumber")
        }

        val reference = QuestionCCCReference(
            question = question,
            cccParagraph = cccParagraph,
            referenceOrder = order
        )
        cccReferenceRepository.save(reference)

        return question
    }

    /**
     * Link question to Bible reference
     */
    fun linkToBibleReference(
        questionId: UUID,
        bibleReferenceId: UUID,
        order: Int = 0
    ): CatechismQuestion {
        val question = questionRepository.findById(questionId).orElseThrow {
            IllegalArgumentException("Question not found: $questionId")
        }

        val bibleReference = bibleRefRepository.findById(bibleReferenceId).orElseThrow {
            IllegalArgumentException("Bible reference not found: $bibleReferenceId")
        }

        // Check if link already exists
        val existingLink = bibleReferenceRepository.findByQuestionIdOrderByReferenceOrder(questionId)
            .find { it.bibleReference.id == bibleReferenceId }

        if (existingLink != null) {
            throw IllegalArgumentException("Question already linked to this Bible reference")
        }

        val reference = QuestionBibleReference(
            question = question,
            bibleReference = bibleReference,
            referenceOrder = order
        )
        bibleReferenceRepository.save(reference)

        return question
    }

    /**
     * Get translation with fallback logic
     * 1. Try requested language
     * 2. Fall back to booklet's default language
     * 3. Fall back to English
     */
    private fun getTranslationOrFallback(questionId: UUID, languageCode: String): CatechismQuestionTranslation? {
        // Try requested language
        val translation = questionTranslationRepository.findByQuestionIdAndLanguageCode(questionId, languageCode)
        if (translation != null) return translation

        // Get question to find booklet's default language
        val question = questionRepository.findById(questionId).orElse(null) ?: return null
        val defaultLanguage = question.booklet.languageDefault

        // Try booklet's default language
        if (languageCode != defaultLanguage) {
            val defaultTranslation = questionTranslationRepository
                .findByQuestionIdAndLanguageCode(questionId, defaultLanguage)
            if (defaultTranslation != null) return defaultTranslation
        }

        // Fall back to English
        if (languageCode != "en" && defaultLanguage != "en") {
            return questionTranslationRepository.findByQuestionIdAndLanguageCode(questionId, "en")
        }

        return null
    }

    /**
     * Get available languages for a question
     */
    fun getAvailableLanguages(questionId: UUID): List<String> {
        return questionTranslationRepository.findByQuestionId(questionId)
            .map { it.languageCode }
    }
}

/**
 * Data class to return question with its translation
 */
data class QuestionWithTranslation(
    val question: CatechismQuestion,
    val translation: CatechismQuestionTranslation
)