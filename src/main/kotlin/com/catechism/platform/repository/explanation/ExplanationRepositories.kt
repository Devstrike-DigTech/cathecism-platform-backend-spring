package com.catechism.platform.repository.explanation

import com.catechism.platform.domain.*
import com.catechism.platform.domain.explanation.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID


@Repository
interface ExplanationSubmissionRepository : JpaRepository<ExplanationSubmission, UUID> {
    fun findByQuestionId(questionId: UUID): List<ExplanationSubmission>
    fun findBySubmitterId(submitterId: UUID): List<ExplanationSubmission>
    fun findBySubmissionStatus(status: ExplanationStatus): List<ExplanationSubmission>
    fun findByQuestionIdAndSubmissionStatus(questionId: UUID, status: ExplanationStatus): List<ExplanationSubmission>
    fun findByQuestionIdAndLanguageCode(questionId: UUID, languageCode: String): List<ExplanationSubmission>

    @Query("""
        SELECT e FROM ExplanationSubmission e 
        WHERE e.submissionStatus IN :statuses 
        ORDER BY 
            CASE e.submissionStatus 
                WHEN 'FLAGGED' THEN 1 
                WHEN 'UNDER_REVIEW' THEN 2 
                WHEN 'PENDING' THEN 3 
            END,
            e.submittedAt ASC
    """)
    fun findModerationQueue(@Param("statuses") statuses: List<ExplanationStatus>): List<ExplanationSubmission>
}

@Repository
interface ExplanationReviewRepository : JpaRepository<ExplanationReview, UUID> {
    fun findByExplanationId(explanationId: UUID): List<ExplanationReview>
    fun findByReviewerId(reviewerId: UUID): List<ExplanationReview>
    fun findByExplanationIdAndReviewerId(explanationId: UUID, reviewerId: UUID): ExplanationReview?
}

@Repository
interface ExplanationFlagRepository : JpaRepository<ExplanationFlag, UUID> {
    fun findByExplanationId(explanationId: UUID): List<ExplanationFlag>
    fun findByFlaggerId(flaggerId: UUID): List<ExplanationFlag>
    fun findByFlagStatus(status: FlagStatus): List<ExplanationFlag>
    fun findByExplanationIdAndFlaggerId(explanationId: UUID, flaggerId: UUID): ExplanationFlag?
    fun findByModeratorId(moderatorId: UUID): List<ExplanationFlag>
}

@Repository
interface ExplanationVoteRepository : JpaRepository<ExplanationVote, UUID> {
    fun findByExplanationId(explanationId: UUID): List<ExplanationVote>
    fun findByUserId(userId: UUID): List<ExplanationVote>
    fun findByExplanationIdAndUserId(explanationId: UUID, userId: UUID): ExplanationVote?

    @Query("SELECT COUNT(v) FROM ExplanationVote v WHERE v.explanation.id = :explanationId AND v.isHelpful = true")
    fun countHelpfulVotes(@Param("explanationId") explanationId: UUID): Long

    @Query("SELECT COUNT(v) FROM ExplanationVote v WHERE v.explanation.id = :explanationId AND v.isHelpful = false")
    fun countUnhelpfulVotes(@Param("explanationId") explanationId: UUID): Long
}

@Repository
interface FileUploadRepository : JpaRepository<FileUpload, UUID> {
    fun findByUploaderId(uploaderId: UUID): List<FileUpload>
    fun findByUploadType(uploadType: FileUploadType): List<FileUpload>
    fun findByProcessingStatus(status: ProcessingStatus): List<FileUpload>
    fun findByVirusScanStatus(status: VirusScanStatus): List<FileUpload>
    fun findByIsPublic(isPublic: Boolean): List<FileUpload>
}
