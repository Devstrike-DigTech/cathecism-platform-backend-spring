package com.catechism.platform.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "catechism_booklet")
data class CatechismBooklet(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var name: String,

    @Column
    var diocese: String? = null,

    @Column(nullable = false)
    var version: String,

    @Column(name = "language_default", nullable = false)
    var languageDefault: String = "en",

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "booklet", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    val questions: MutableList<CatechismQuestion> = mutableListOf()
)
