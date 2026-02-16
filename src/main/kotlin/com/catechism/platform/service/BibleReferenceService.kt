package com.catechism.platform.service

import com.catechism.platform.domain.BibleReference
import com.catechism.platform.domain.BibleReferenceTranslation
import com.catechism.platform.repository.BibleReferenceRepository
import com.catechism.platform.repository.BibleReferenceTranslationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class BibleReferenceService(
    private val bibleReferenceRepository: BibleReferenceRepository,
    private val bibleTranslationRepository: BibleReferenceTranslationRepository
) {

    /**
     * Get Bible reference by ID
     */
    fun getBibleReference(id: UUID, languageCode: String = "en"): BibleReferenceWithTranslation? {
        val reference = bibleReferenceRepository.findById(id).orElse(null) ?: return null
        val translation = getTranslationOrFallback(id, languageCode) ?: return null
        return BibleReferenceWithTranslation(reference = reference, translation = translation)
    }

    /**
     * Add a new Bible reference with translation
     */
    fun addBibleReference(
        book: String,
        chapter: Int,
        verseStart: Int,
        verseEnd: Int?,
        translation: String = "RSV-CE",
        languageCode: String,
        verseText: String
    ): BibleReference {
        // Check if reference already exists
        val existing = bibleReferenceRepository.findByBookAndChapterAndVerseStartAndVerseEndAndTranslation(
            book, chapter, verseStart, verseEnd, translation
        )

        if (existing != null) {
            // Reference exists, just add translation if not present
            val existingTranslation = bibleTranslationRepository
                .findByBibleReferenceIdAndLanguageCode(existing.id, languageCode)

            if (existingTranslation != null) {
                throw IllegalArgumentException("Bible reference $book $chapter:$verseStart already has translation in $languageCode")
            }

            // Add new translation
            val verseTranslation = BibleReferenceTranslation(
                bibleReference = existing,
                languageCode = languageCode,
                verseText = verseText
            )
            bibleTranslationRepository.save(verseTranslation)

            return existing
        }

        // Create new reference
        val reference = BibleReference(
            book = book,
            chapter = chapter,
            verseStart = verseStart,
            verseEnd = verseEnd,
            translation = translation
        )
        val savedReference = bibleReferenceRepository.save(reference)

        // Create translation
        val verseTranslation = BibleReferenceTranslation(
            bibleReference = savedReference,
            languageCode = languageCode,
            verseText = verseText
        )
        bibleTranslationRepository.save(verseTranslation)

        return savedReference
    }

    /**
     * Get available languages for a Bible reference
     */
    fun getAvailableLanguages(referenceId: UUID): List<String> {
        val reference = bibleReferenceRepository.findById(referenceId).orElse(null) ?: return emptyList()
        return reference.translations.map { it.languageCode }
    }

    /**
     * Get translation with fallback to English
     */
    private fun getTranslationOrFallback(
        referenceId: UUID,
        languageCode: String
    ): BibleReferenceTranslation? {
        // Try requested language
        val translation = bibleTranslationRepository.findByBibleReferenceIdAndLanguageCode(referenceId, languageCode)
        if (translation != null) return translation

        // Fall back to English
        if (languageCode != "en") {
            return bibleTranslationRepository.findByBibleReferenceIdAndLanguageCode(referenceId, "en")
        }

        return null
    }
}

/**
 * Data class for Bible reference with translation
 */
data class BibleReferenceWithTranslation(
    val reference: BibleReference,
    val translation: BibleReferenceTranslation
)