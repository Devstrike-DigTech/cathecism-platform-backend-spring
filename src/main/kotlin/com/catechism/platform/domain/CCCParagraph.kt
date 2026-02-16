package com.catechism.platform.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "ccc_paragraph")
data class CCCParagraph(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(name = "paragraph_number", nullable = false, unique = true)
    val paragraphNumber: Int,

    @Column(nullable = false)
    var edition: String = "2nd Edition",

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "cccParagraph", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val translations: MutableList<CCCParagraphTranslation> = mutableListOf()
)
