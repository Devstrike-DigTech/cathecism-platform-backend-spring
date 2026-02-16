package com.catechism.platform.graphql

import com.catechism.platform.domain.CatechismBooklet
import com.catechism.platform.service.BookletService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import java.util.*

@Controller
class BookletQueryResolver(
    private val bookletService: BookletService
) {

    @QueryMapping
    fun booklets(): List<CatechismBooklet> {
        return bookletService.getAllBooklets()
    }

    @QueryMapping
    fun booklet(@Argument id: UUID): CatechismBooklet? {
        return bookletService.getBookletById(id)
    }
}