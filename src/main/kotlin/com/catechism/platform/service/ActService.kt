package com.catechism.platform.service

import com.catechism.platform.domain.CatechismAct
import com.catechism.platform.domain.CatechismActTranslation
import com.catechism.platform.repository.CatechismActRepository
import com.catechism.platform.repository.CatechismActTranslationRepository
import com.catechism.platform.repository.CatechismBookletRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
@Transactional
class ActService(
    private val actRepository: CatechismActRepository,
    private val actTranslationRepository: CatechismActTranslationRepository,
    private val bookletRepository: CatechismBookletRepository
) {

    /**
     * Get all acts for a booklet
     */
    fun getActsByBooklet(bookletId: UUID, languageCode: String = "en"): List<ActWithTranslation> {
        val acts = actRepository.findByBookletIdOrderByDisplayOrder(bookletId)
        return acts.mapNotNull { act ->
            val translation = getTranslationOrFallback(act.id, languageCode, act.booklet.languageDefault)
            translation?.let {
                ActWithTranslation(act = act, translation = it)
            }
        }
    }

    /**
     * Get a single act
     */
    fun getActById(id: UUID, languageCode: String = "en"): ActWithTranslation? {
        val act = actRepository.findById(id).orElse(null) ?: return null
        val translation = getTranslationOrFallback(id, languageCode, act.booklet.languageDefault)
            ?: return null
        return ActWithTranslation(act = act, translation = translation)
    }

    /**
     * Create a new act with translation
     */
    fun createAct(
        bookletId: UUID,
        actNumber: Int,
        displayOrder: Int = 0,
        languageCode: String,
        title: String,
        description: String? = null
    ): CatechismAct {
        // Verify booklet exists
        val booklet = bookletRepository.findById(bookletId).orElseThrow {
            IllegalArgumentException("Booklet not found: $bookletId")
        }

        // Check if act number is unique within booklet
        val existing = actRepository.findAll()
            .find { it.booklet.id == bookletId && it.actNumber == actNumber }

        if (existing != null) {
            throw IllegalArgumentException("Act number $actNumber already exists in this booklet")
        }

        // Create act
        val act = CatechismAct(
            booklet = booklet,
            actNumber = actNumber,
            displayOrder = displayOrder
        )
        val savedAct = actRepository.save(act)

        // Create translation
        val translation = CatechismActTranslation(
            act = savedAct,
            languageCode = languageCode,
            title = title,
            description = description
        )
        actTranslationRepository.save(translation)

        return savedAct
    }

    /**
     * Update act (display order, or add/update translation)
     */
    fun updateAct(
        id: UUID,
        displayOrder: Int?,
        languageCode: String?,
        title: String?,
        description: String?
    ): CatechismAct {
        val act = actRepository.findById(id).orElseThrow {
            IllegalArgumentException("Act not found: $id")
        }

        // Update display order if provided
        var updatedAct = act
        if (displayOrder != null && displayOrder != act.displayOrder) {
            updatedAct = act.copy(
                displayOrder = displayOrder,
                updatedAt = Instant.now()
            )
            updatedAct = actRepository.save(updatedAct)
        }

        // Update or create translation if language provided
        if (languageCode != null && (title != null || description != null)) {
            val existingTranslation = actTranslationRepository
                .findByActIdAndLanguageCode(id, languageCode)

            if (existingTranslation != null) {
                // Update existing translation
                existingTranslation.title = title ?: existingTranslation.title
                existingTranslation.description = description ?: existingTranslation.description
                existingTranslation.updatedAt = Instant.now()
                actTranslationRepository.save(existingTranslation)
            } else if (title != null) {
                // Create new translation (title is required)
                val newTranslation = CatechismActTranslation(
                    act = updatedAct,
                    languageCode = languageCode,
                    title = title,
                    description = description
                )
                actTranslationRepository.save(newTranslation)
            }
        }

        return updatedAct
    }

    /**
     * Delete an act (cascades to subtitles, questions become unlinked)
     */
    fun deleteAct(id: UUID): Boolean {
        if (!actRepository.existsById(id)) {
            return false
        }
        actRepository.deleteById(id)
        return true
    }

    /**
     * Get available languages for an act
     */
    fun getAvailableLanguages(actId: UUID): List<String> {
        return actTranslationRepository.findByActId(actId)
            .map { it.languageCode }
    }

    /**
     * Get translation with fallback logic
     */
    private fun getTranslationOrFallback(
        actId: UUID,
        languageCode: String,
        defaultLanguage: String
    ): CatechismActTranslation? {
        // Try requested language
        val translation = actTranslationRepository.findByActIdAndLanguageCode(actId, languageCode)
        if (translation != null) return translation

        // Try default language
        if (languageCode != defaultLanguage) {
            val defaultTranslation = actTranslationRepository
                .findByActIdAndLanguageCode(actId, defaultLanguage)
            if (defaultTranslation != null) return defaultTranslation
        }

        // Fall back to English
        if (languageCode != "en" && defaultLanguage != "en") {
            return actTranslationRepository.findByActIdAndLanguageCode(actId, "en")
        }

        return null
    }
}

/**
 * Data class for Act with translation
 */
data class ActWithTranslation(
    val act: CatechismAct,
    val translation: CatechismActTranslation
)