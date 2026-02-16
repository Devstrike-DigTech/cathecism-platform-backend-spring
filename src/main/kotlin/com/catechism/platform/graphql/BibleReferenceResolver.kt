package com.catechism.platform.graphql

import com.catechism.platform.service.BibleReferenceService
import com.catechism.platform.service.BibleReferenceWithTranslation
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.util.*

@Controller
class BibleReferenceResolver(
    private val bibleReferenceService: BibleReferenceService
) {

    @QueryMapping
    fun bibleReferenceById(@Argument id: UUID, @Argument language: String = "en"): BibleReferenceDTO? {
        val result = bibleReferenceService.getBibleReference(id, language) ?: return null
        return result.toDTO()
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun addBibleReference(@Argument input: CreateBibleReferenceInput): BibleReferenceDTO {
        val reference = bibleReferenceService.addBibleReference(
            book = input.book,
            chapter = input.chapter,
            verseStart = input.verseStart,
            verseEnd = input.verseEnd,
            translation = input.translation ?: "RSV-CE",
            languageCode = input.language ?: "en",
            verseText = input.verseText
        )

        val result = bibleReferenceService.getBibleReference(reference.id, input.language ?: "en")
            ?: throw IllegalStateException("Bible reference was created but not found")

        return result.toDTO()
    }

    // Helper extension function
    private fun BibleReferenceWithTranslation.toDTO(): BibleReferenceDTO {
        return BibleReferenceDTO(
            id = reference.id,
            reference = reference.getFormattedReference(),
            text = translation.verseText,
            book = reference.book,
            chapter = reference.chapter,
            verseStart = reference.verseStart,
            verseEnd = reference.verseEnd,
            translation = reference.translation
        )
    }
}

// DTOs
data class BibleReferenceDTO(
    val id: UUID,
    val reference: String,
    val text: String,
    val book: String,
    val chapter: Int,
    val verseStart: Int,
    val verseEnd: Int?,
    val translation: String
)

data class CreateBibleReferenceInput(
    val book: String,
    val chapter: Int,
    val verseStart: Int,
    val verseEnd: Int?,
    val translation: String?,
    val language: String?,
    val verseText: String
)