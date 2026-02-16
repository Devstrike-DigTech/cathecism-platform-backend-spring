package com.catechism.platform.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "bible_reference_translation",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["bible_reference_id", "language_code"])
    ]
)
data class BibleReferenceTranslation(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bible_reference_id", nullable = false)
    val bibleReference: BibleReference,

    @Column(name = "language_code", nullable = false)
    val languageCode: String,

    @Column(name = "verse_text", nullable = false, columnDefinition = "TEXT")
    var verseText: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
