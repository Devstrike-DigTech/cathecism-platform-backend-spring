package com.catechism.platform.graphql

import com.catechism.platform.domain.CatechismBooklet
import com.catechism.platform.service.BookletService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
class BookletMutationResolver(
    private val bookletService: BookletService
) {

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun createBooklet(@Argument input: CreateBookletInput): CatechismBooklet {
        return bookletService.createBooklet(
            name = input.name,
            version = input.version,
            diocese = input.diocese,
            languageDefault = input.languageDefault ?: "en"
        )
    }
}

data class CreateBookletInput(
    val name: String,
    val diocese: String?,
    val version: String,
    val languageDefault: String?
)