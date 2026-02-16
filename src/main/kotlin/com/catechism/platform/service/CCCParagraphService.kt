package com.catechism.platform.service

import com.catechism.platform.domain.CCCParagraph
import com.catechism.platform.domain.CCCParagraphTranslation
import com.catechism.platform.repository.CCCParagraphRepository
import com.catechism.platform.repository.CCCParagraphTranslationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class CCCParagraphService(
    private val cccParagraphRepository: CCCParagraphRepository,
    private val cccTranslationRepository: CCCParagraphTranslationRepository
) {

    /**
     * Get CCC paragraph by number
     */
    fun getCCCParagraph(paragraphNumber: Int, languageCode: String = "en"): CCCParagraphWithTranslation? {
        val paragraph = cccParagraphRepository.findByParagraphNumber(paragraphNumber) ?: return null
        val translation = getTranslationOrFallback(paragraph.id, languageCode) ?: return null
        return CCCParagraphWithTranslation(paragraph = paragraph, translation = translation)
    }

    /**
     * Add a new CCC paragraph with translation
     */
    fun addCCCParagraph(
        paragraphNumber: Int,
        edition: String = "2nd Edition",
        languageCode: String,
        paragraphText: String,
        licensed: Boolean = false
    ): CCCParagraph {
        // Check if paragraph already exists
        val existing = cccParagraphRepository.findByParagraphNumber(paragraphNumber)
        if (existing != null) {
            // Paragraph exists, just add translation if not present
            val existingTranslation = cccTranslationRepository
                .findByCccParagraphIdAndLanguageCode(existing.id, languageCode)

            if (existingTranslation != null) {
                throw IllegalArgumentException("CCC paragraph $paragraphNumber already has translation in $languageCode")
            }

            // Add new translation
            val translation = CCCParagraphTranslation(
                cccParagraph = existing,
                languageCode = languageCode,
                paragraphText = paragraphText,
                licensed = licensed
            )
            cccTranslationRepository.save(translation)

            return existing
        }

        // Create new paragraph
        val paragraph = CCCParagraph(
            paragraphNumber = paragraphNumber,
            edition = edition
        )
        val savedParagraph = cccParagraphRepository.save(paragraph)

        // Create translation
        val translation = CCCParagraphTranslation(
            cccParagraph = savedParagraph,
            languageCode = languageCode,
            paragraphText = paragraphText,
            licensed = licensed
        )
        cccTranslationRepository.save(translation)

        return savedParagraph
    }

    /**
     * Get available languages for a CCC paragraph
     */
    fun getAvailableLanguages(paragraphNumber: Int): List<String> {
        val paragraph = cccParagraphRepository.findByParagraphNumber(paragraphNumber) ?: return emptyList()
        return paragraph.translations.map { it.languageCode }
    }

    /**
     * Get translation with fallback to English
     */
    private fun getTranslationOrFallback(
        paragraphId: UUID,
        languageCode: String
    ): CCCParagraphTranslation? {
        // Try requested language
        val translation = cccTranslationRepository.findByCccParagraphIdAndLanguageCode(paragraphId, languageCode)
        if (translation != null) return translation

        // Fall back to English
        if (languageCode != "en") {
            return cccTranslationRepository.findByCccParagraphIdAndLanguageCode(paragraphId, "en")
        }

        return null
    }
}

/**
 * Data class for CCC paragraph with translation
 */
data class CCCParagraphWithTranslation(
    val paragraph: CCCParagraph,
    val translation: CCCParagraphTranslation
)