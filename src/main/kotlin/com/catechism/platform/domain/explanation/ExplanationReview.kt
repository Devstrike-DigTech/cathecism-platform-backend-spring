package com.catechism.platform.domain.explanation

import com.catechism.platform.domain.AppUser
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "explanation_review")
data class ExplanationReview(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "explanation_id", nullable = false)
    val explanation: ExplanationSubmission,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    val reviewer: AppUser,

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 20)
    val reviewStatus: ReviewStatus,

    @Column(name = "review_comments", columnDefinition = "TEXT")
    val reviewComments: String? = null,

    @Column(name = "quality_rating")
    val qualityRating: Int? = null, // 1-5

    // Review criteria scores (1-5)
    @Column(name = "accuracy_score")
    val accuracyScore: Int? = null,

    @Column(name = "clarity_score")
    val clarityScore: Int? = null,

    @Column(name = "theological_soundness_score")
    val theologicalSoundnessScore: Int? = null,

    @Column(name = "reviewed_at", nullable = false)
    val reviewedAt: Instant = Instant.now(),

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now()
) {
    init {
        // Validation
        qualityRating?.let {
            require(it in 1..5) { "Quality rating must be between 1 and 5" }
        }
        accuracyScore?.let {
            require(it in 1..5) { "Accuracy score must be between 1 and 5" }
        }
        clarityScore?.let {
            require(it in 1..5) { "Clarity score must be between 1 and 5" }
        }
        theologicalSoundnessScore?.let {
            require(it in 1..5) { "Theological soundness score must be between 1 and 5" }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExplanationReview) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

enum class ReviewStatus {
    APPROVED,
    REJECTED,
    NEEDS_REVISION
}