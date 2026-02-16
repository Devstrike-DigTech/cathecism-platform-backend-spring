package com.catechism.platform.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "bible_reference",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["book", "chapter", "verse_start", "verse_end", "translation"])
    ]
)
data class BibleReference(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val book: String,

    @Column(nullable = false)
    val chapter: Int,

    @Column(name = "verse_start", nullable = false)
    val verseStart: Int,

    @Column(name = "verse_end")
    val verseEnd: Int? = null,

    @Column(nullable = false)
    var translation: String = "RSV-CE",

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "bibleReference", cascade = [CascadeType.ALL], fetch = FetchType.LAZY, orphanRemoval = true)
    val translations: MutableList<BibleReferenceTranslation> = mutableListOf()
) {
    // Helper method to format reference string
    fun getFormattedReference(): String {
        return if (verseEnd != null && verseEnd != verseStart) {
            "$book $chapter:$verseStart-$verseEnd"
        } else {
            "$book $chapter:$verseStart"
        }
    }
}
