package com.catechism.platform.graphql

import com.catechism.platform.service.ActService
import com.catechism.platform.service.ActWithTranslation
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.util.*

@Controller
class ActResolver(
    private val actService: ActService
) {

    @QueryMapping
    fun acts(@Argument bookletId: UUID, @Argument language: String = "en"): List<ActDTO> {
        return actService.getActsByBooklet(bookletId, language)
            .map { it.toDTO(language) }
    }

    @QueryMapping
    fun act(@Argument id: UUID, @Argument language: String = "en"): ActDTO? {
        val result = actService.getActById(id, language) ?: return null
        return result.toDTO(language)
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun createAct(@Argument input: CreateActInput): ActDTO {
        val act = actService.createAct(
            bookletId = input.bookletId,
            actNumber = input.actNumber,
            displayOrder = input.displayOrder ?: 0,
            languageCode = input.language ?: "en",
            title = input.title,
            description = input.description
        )

        val result = actService.getActById(act.id, input.language ?: "en")
            ?: throw IllegalStateException("Act was created but not found")

        return result.toDTO(input.language ?: "en")
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun updateAct(@Argument id: UUID, @Argument input: UpdateActInput): ActDTO {
        val act = actService.updateAct(
            id = id,
            displayOrder = input.displayOrder,
            languageCode = input.language,
            title = input.title,
            description = input.description
        )

        val result = actService.getActById(id, input.language ?: "en")
            ?: throw IllegalStateException("Act not found after update")

        return result.toDTO(input.language ?: "en")
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteAct(@Argument id: UUID): Boolean {
        return actService.deleteAct(id)
    }

    // Helper extension function
    private fun ActWithTranslation.toDTO(language: String): ActDTO {
        return ActDTO(
            id = act.id,
            actNumber = act.actNumber,
            title = translation.title,
            description = translation.description,
            displayOrder = act.displayOrder,
            availableLanguages = actService.getAvailableLanguages(act.id)
        )
    }
}

// DTOs
data class ActDTO(
    val id: UUID,
    val actNumber: Int,
    val title: String,
    val description: String?,
    val displayOrder: Int,
    val availableLanguages: List<String>
)

data class CreateActInput(
    val bookletId: UUID,
    val actNumber: Int,
    val displayOrder: Int?,
    val language: String?,
    val title: String,
    val description: String?
)

data class UpdateActInput(
    val displayOrder: Int?,
    val language: String?,
    val title: String?,
    val description: String?
)