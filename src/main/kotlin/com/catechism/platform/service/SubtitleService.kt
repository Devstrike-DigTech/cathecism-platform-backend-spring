package com.catechism.platform.service

import com.catechism.platform.domain.CatechismActSubtitle
import com.catechism.platform.domain.CatechismActSubtitleTranslation
import com.catechism.platform.repository.CatechismActRepository
import com.catechism.platform.repository.CatechismActSubtitleRepository
import com.catechism.platform.repository.CatechismActSubtitleTranslationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
@Transactional
class SubtitleService(
    private val subtitleRepository: CatechismActSubtitleRepository,
    private val subtitleTranslationRepository: CatechismActSubtitleTranslationRepository,
    private val actRepository: CatechismActRepository
) {

    /**
     * Get all subtitles for an act
     */
    fun getSubtitlesByAct(actId: UUID, languageCode: String = "en"): List<SubtitleWithTranslation> {
        val subtitles = subtitleRepository.findByActIdOrderByDisplayOrder(actId)
        val act = actRepository.findById(actId).orElse(null)
        val defaultLanguage = act?.booklet?.languageDefault ?: "en"

        return subtitles.mapNotNull { subtitle ->
            val translation = getTranslationOrFallback(subtitle.id, languageCode, defaultLanguage)
            translation?.let {
                SubtitleWithTranslation(subtitle = subtitle, translation = it)
            }
        }
    }

    /**
     * Get a single subtitle
     */
    fun getSubtitleById(id: UUID, languageCode: String = "en"): SubtitleWithTranslation? {
        val subtitle = subtitleRepository.findById(id).orElse(null) ?: return null
        val defaultLanguage = subtitle.act.booklet.languageDefault
        val translation = getTranslationOrFallback(id, languageCode, defaultLanguage)
            ?: return null
        return SubtitleWithTranslation(subtitle = subtitle, translation = translation)
    }

    /**
     * Create a new subtitle with translation
     */
    fun createSubtitle(
        actId: UUID,
        subtitleNumber: Int,
        displayOrder: Int = 0,
        languageCode: String,
        title: String,
        description: String? = null
    ): CatechismActSubtitle {
        // Verify act exists
        val act = actRepository.findById(actId).orElseThrow {
            IllegalArgumentException("Act not found: $actId")
        }

        // Check if subtitle number is unique within act
        val existing = subtitleRepository.findAll()
            .find { it.act.id == actId && it.subtitleNumber == subtitleNumber }

        if (existing != null) {
            throw IllegalArgumentException("Subtitle number $subtitleNumber already exists in this act")
        }

        // Create subtitle
        val subtitle = CatechismActSubtitle(
            act = act,
            subtitleNumber = subtitleNumber,
            displayOrder = displayOrder
        )
        val savedSubtitle = subtitleRepository.save(subtitle)

        // Create translation
        val translation = CatechismActSubtitleTranslation(
            subtitle = savedSubtitle,
            languageCode = languageCode,
            title = title,
            description = description
        )
        subtitleTranslationRepository.save(translation)

        return savedSubtitle
    }

    /**
     * Update subtitle (display order or translations)
     */
    fun updateSubtitle(
        id: UUID,
        displayOrder: Int?,
        languageCode: String?,
        title: String?,
        description: String?
    ): CatechismActSubtitle {
        val subtitle = subtitleRepository.findById(id).orElseThrow {
            IllegalArgumentException("Subtitle not found: $id")
        }

        // Update display order if provided
        var updatedSubtitle = subtitle
        if (displayOrder != null && displayOrder != subtitle.displayOrder) {
            updatedSubtitle = subtitle.copy(
                displayOrder = displayOrder,
                updatedAt = Instant.now()
            )
            updatedSubtitle = subtitleRepository.save(updatedSubtitle)
        }

        // Update or create translation if language provided
        if (languageCode != null && (title != null || description != null)) {
            val existingTranslation = subtitleTranslationRepository
                .findBySubtitleIdAndLanguageCode(id, languageCode)

            if (existingTranslation != null) {
                // Update existing translation
                existingTranslation.title = title ?: existingTranslation.title
                existingTranslation.description = description ?: existingTranslation.description
                existingTranslation.updatedAt = Instant.now()
                subtitleTranslationRepository.save(existingTranslation)
            } else if (title != null) {
                // Create new translation (title is required)
                val newTranslation = CatechismActSubtitleTranslation(
                    subtitle = updatedSubtitle,
                    languageCode = languageCode,
                    title = title,
                    description = description
                )
                subtitleTranslationRepository.save(newTranslation)
            }
        }

        return updatedSubtitle
    }

    /**
     * Delete a subtitle (questions become unlinked, not deleted)
     */
    fun deleteSubtitle(id: UUID): Boolean {
        if (!subtitleRepository.existsById(id)) {
            return false
        }
        subtitleRepository.deleteById(id)
        return true
    }

    /**
     * Get available languages for a subtitle
     */
    fun getAvailableLanguages(subtitleId: UUID): List<String> {
        return subtitleTranslationRepository.findBySubtitleId(subtitleId)
            .map { it.languageCode }
    }

    /**
     * Get translation with fallback logic
     */
    private fun getTranslationOrFallback(
        subtitleId: UUID,
        languageCode: String,
        defaultLanguage: String
    ): CatechismActSubtitleTranslation? {
        // Try requested language
        val translation = subtitleTranslationRepository
            .findBySubtitleIdAndLanguageCode(subtitleId, languageCode)
        if (translation != null) return translation

        // Try default language
        if (languageCode != defaultLanguage) {
            val defaultTranslation = subtitleTranslationRepository
                .findBySubtitleIdAndLanguageCode(subtitleId, defaultLanguage)
            if (defaultTranslation != null) return defaultTranslation
        }

        // Fall back to English
        if (languageCode != "en" && defaultLanguage != "en") {
            return subtitleTranslationRepository.findBySubtitleIdAndLanguageCode(subtitleId, "en")
        }

        return null
    }
}

/**
 * Data class for Subtitle with translation
 */
data class SubtitleWithTranslation(
    val subtitle: CatechismActSubtitle,
    val translation: CatechismActSubtitleTranslation
)