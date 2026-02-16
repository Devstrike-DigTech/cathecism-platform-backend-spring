package com.catechism.platform.domain

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "catechism_act_subtitle_translation",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["subtitle_id", "language_code"])
    ]
)
data class CatechismActSubtitleTranslation(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subtitle_id", nullable = false)
    val subtitle: CatechismActSubtitle,

    @Column(name = "language_code", nullable = false)
    val languageCode: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var title: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
