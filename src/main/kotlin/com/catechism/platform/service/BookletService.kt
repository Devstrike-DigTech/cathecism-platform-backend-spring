package com.catechism.platform.service

import com.catechism.platform.domain.CatechismBooklet
import com.catechism.platform.repository.CatechismBookletRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class BookletService(
    private val bookletRepository: CatechismBookletRepository
) {

    fun getAllBooklets(): List<CatechismBooklet> {
        return bookletRepository.findAll()
    }

    fun getBookletById(id: UUID): CatechismBooklet? {
        return bookletRepository.findById(id).orElse(null)
    }

    fun createBooklet(
        name: String,
        version: String,
        diocese: String? = null,
        languageDefault: String = "en"
    ): CatechismBooklet {
        val booklet = CatechismBooklet(
            name = name,
            version = version,
            diocese = diocese,
            languageDefault = languageDefault
        )
        return bookletRepository.save(booklet)
    }
}