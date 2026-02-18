package com.catechism.platform.domain.explanation

import com.catechism.platform.domain.AppUser
import com.catechism.platform.domain.CatechismQuestion
import jakarta.persistence.*
import java.time.Instant
import java.util.*

@Entity
@Table(name = "explanation_submission")
data class ExplanationSubmission(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    val question: CatechismQuestion,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submitter_id", nullable = false)
    val submitter: AppUser,

    @Column(name = "language_code", nullable = false, length = 10)
    val languageCode: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 20)
    val contentType: ExplanationContentType,

    // Text content (for TEXT type)
    @Column(name = "text_content", columnDefinition = "TEXT")
    var textContent: String? = null,

    // File metadata (for AUDIO/VIDEO type)
    @Column(name = "file_url")
    var fileUrl: String? = null,

    @Column(name = "file_size_bytes")
    var fileSizeBytes: Long? = null,

    @Column(name = "file_mime_type", length = 100)
    var fileMimeType: String? = null,

    @Column(name = "duration_seconds")
    var durationSeconds: Int? = null,

    // Submission metadata
    @Enumerated(EnumType.STRING)
    @Column(name = "submission_status", nullable = false, length = 20)
    var submissionStatus: ExplanationStatus = ExplanationStatus.PENDING,

    @Column(name = "quality_score")
    var qualityScore: Int? = null,

    @Column(name = "view_count")
    var viewCount: Int = 0,

    @Column(name = "helpful_count")
    var helpfulCount: Int = 0,

    // Timestamps
    @Column(name = "submitted_at", nullable = false)
    val submittedAt: Instant = Instant.now(),

    @Column(name = "reviewed_at")
    var reviewedAt: Instant? = null,

    @Column(name = "approved_at")
    var approvedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    // Relationships
    @OneToMany(mappedBy = "explanation", cascade = [CascadeType.ALL], orphanRemoval = true)
    val reviews: MutableList<ExplanationReview> = mutableListOf(),

    @OneToMany(mappedBy = "explanation", cascade = [CascadeType.ALL], orphanRemoval = true)
    val flags: MutableList<ExplanationFlag> = mutableListOf(),

    @OneToMany(mappedBy = "explanation", cascade = [CascadeType.ALL], orphanRemoval = true)
    val votes: MutableList<ExplanationVote> = mutableListOf()
) {
    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }

    /**
     * Calculate quality score based on reviews and votes
     */
    fun calculateQualityScore(): Int {
        if (reviews.isEmpty() && votes.isEmpty()) {
            return 50 // Default neutral score
        }

        val avgModeratorRating = if (reviews.isNotEmpty()) {
            reviews.mapNotNull { it.qualityRating }.average() * 20 // Convert 1-5 to 0-100 scale
        } else {
            50.0
        }

        val totalVotes = votes.size
        val helpfulVotes = votes.count { it.isHelpful }
        val helpfulRatio = if (totalVotes > 0) {
            (helpfulVotes.toDouble() / totalVotes) * 100
        } else {
            50.0
        }

        val viewEngagement = if (viewCount > 0) {
            ((helpfulCount.toDouble() / viewCount) * 100).coerceIn(0.0, 100.0)
        } else {
            50.0
        }

        val openFlagCount = flags.count { it.flagStatus == FlagStatus.OPEN }
        val flagPenalty = (openFlagCount * 10).coerceAtMost(50) // Max 50 point penalty

        val score = (
                (avgModeratorRating * 0.4) +
                        (helpfulRatio * 0.3) +
                        (viewEngagement * 0.2) +
                        (50 * 0.1) // Base component
                ) - flagPenalty

        return score.toInt().coerceIn(0, 100)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExplanationSubmission) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

enum class ExplanationContentType {
    TEXT,
    AUDIO,
    VIDEO
}

enum class ExplanationStatus {
    PENDING,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    FLAGGED
}