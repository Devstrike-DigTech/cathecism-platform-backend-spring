package com.catechism.platform.graphql

import com.catechism.platform.service.SubtitleService
import com.catechism.platform.service.SubtitleWithTranslation
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.util.*

@Controller
class SubtitleResolver(
    private val subtitleService: SubtitleService
) {

    @QueryMapping
    fun subtitles(@Argument actId: UUID, @Argument language: String = "en"): List<SubtitleDTO> {
        return subtitleService.getSubtitlesByAct(actId, language)
            .map { it.toDTO(language) }
    }

    @QueryMapping
    fun subtitle(@Argument id: UUID, @Argument language: String = "en"): SubtitleDTO? {
        val result = subtitleService.getSubtitleById(id, language) ?: return null
        return result.toDTO(language)
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun createSubtitle(@Argument input: CreateSubtitleInput): SubtitleDTO {
        val subtitle = subtitleService.createSubtitle(
            actId = input.actId,
            subtitleNumber = input.subtitleNumber,
            displayOrder = input.displayOrder ?: 0,
            languageCode = input.language ?: "en",
            title = input.title,
            description = input.description
        )

        val result = subtitleService.getSubtitleById(subtitle.id, input.language ?: "en")
            ?: throw IllegalStateException("Subtitle was created but not found")

        return result.toDTO(input.language ?: "en")
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun updateSubtitle(@Argument id: UUID, @Argument input: UpdateSubtitleInput): SubtitleDTO {
        val subtitle = subtitleService.updateSubtitle(
            id = id,
            displayOrder = input.displayOrder,
            languageCode = input.language,
            title = input.title,
            description = input.description
        )

        val result = subtitleService.getSubtitleById(id, input.language ?: "en")
            ?: throw IllegalStateException("Subtitle not found after update")

        return result.toDTO(input.language ?: "en")
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteSubtitle(@Argument id: UUID): Boolean {
        return subtitleService.deleteSubtitle(id)
    }

    // Helper extension function
    private fun SubtitleWithTranslation.toDTO(language: String): SubtitleDTO {
        return SubtitleDTO(
            id = subtitle.id,
            subtitleNumber = subtitle.subtitleNumber,
            title = translation.title,
            description = translation.description,
            displayOrder = subtitle.displayOrder,
            availableLanguages = subtitleService.getAvailableLanguages(subtitle.id)
        )
    }
}

// DTOs
data class SubtitleDTO(
    val id: UUID,
    val subtitleNumber: Int,
    val title: String,
    val description: String?,
    val displayOrder: Int,
    val availableLanguages: List<String>
)

data class CreateSubtitleInput(
    val actId: UUID,
    val subtitleNumber: Int,
    val displayOrder: Int?,
    val language: String?,
    val title: String,
    val description: String?
)

data class UpdateSubtitleInput(
    val displayOrder: Int?,
    val language: String?,
    val title: String?,
    val description: String?
)