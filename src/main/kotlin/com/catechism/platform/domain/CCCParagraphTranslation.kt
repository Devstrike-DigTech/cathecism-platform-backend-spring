package com.catechism.platform.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "ccc_paragraph_translation",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["ccc_id", "language_code"])
    ]
)
data class CCCParagraphTranslation(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ccc_id", nullable = false)
    val cccParagraph: CCCParagraph,

    @Column(name = "language_code", nullable = false)
    val languageCode: String,

    @Column(name = "paragraph_text", nullable = false, columnDefinition = "TEXT")
    var paragraphText: String,

    @Column(nullable = false)
    var licensed: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
