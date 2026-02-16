package com.catechism.platform.graphql

import com.catechism.platform.service.CCCParagraphService
import com.catechism.platform.service.CCCParagraphWithTranslation
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
class CCCParagraphResolver(
    private val cccParagraphService: CCCParagraphService
) {

    @QueryMapping
    fun cccParagraph(@Argument number: Int, @Argument language: String = "en"): CCCParagraphDTO? {
        val result = cccParagraphService.getCCCParagraph(number, language) ?: return null
        return result.toDTO()
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun addCCCParagraph(@Argument input: CreateCCCParagraphInput): CCCParagraphDTO {
        val paragraph = cccParagraphService.addCCCParagraph(
            paragraphNumber = input.paragraphNumber,
            edition = input.edition ?: "2nd Edition",
            languageCode = input.language ?: "en",
            paragraphText = input.paragraphText,
            licensed = input.licensed ?: false
        )

        val result = cccParagraphService.getCCCParagraph(paragraph.paragraphNumber, input.language ?: "en")
            ?: throw IllegalStateException("CCC paragraph was created but not found")

        return result.toDTO()
    }

    // Helper extension function
    private fun CCCParagraphWithTranslation.toDTO(): CCCParagraphDTO {
        return CCCParagraphDTO(
            id = paragraph.id,
            paragraphNumber = paragraph.paragraphNumber,
            text = translation.paragraphText,
            edition = paragraph.edition,
            licensed = translation.licensed
        )
    }
}

// DTOs
data class CCCParagraphDTO(
    val id: java.util.UUID,
    val paragraphNumber: Int,
    val text: String,
    val edition: String,
    val licensed: Boolean
)

data class CreateCCCParagraphInput(
    val paragraphNumber: Int,
    val edition: String?,
    val language: String?,
    val paragraphText: String,
    val licensed: Boolean?
)