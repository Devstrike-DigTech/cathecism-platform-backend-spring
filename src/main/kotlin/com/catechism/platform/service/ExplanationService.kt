package com.catechism.platform.service

import com.catechism.platform.domain.explanation.*
import com.catechism.platform.repository.AppUserRepository
import com.catechism.platform.repository.CatechismQuestionRepository
import com.catechism.platform.repository.explanation.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
@Transactional
class ExplanationService(
    private val explanationRepository: ExplanationSubmissionRepository,
    private val questionRepository: CatechismQuestionRepository,
    private val userRepository: AppUserRepository,
    private val fileUploadRepository: FileUploadRepository
) {

    /**
     * Submit a text explanation
     */
    fun submitTextExplanation(
        questionId: UUID,
        submitterId: UUID,
        languageCode: String,
        textContent: String
    ): ExplanationSubmission {
        // Validate question exists
        val question = questionRepository.findById(questionId).orElseThrow {
            IllegalArgumentException("Question not found: $questionId")
        }

        // Validate submitter exists
        val submitter = userRepository.findById(submitterId).orElseThrow {
            IllegalArgumentException("User not found: $submitterId")
        }

        // Validate text content
        require(textContent.isNotBlank()) {
            "Text content cannot be blank"
        }

        // Create explanation
        val explanation = ExplanationSubmission(
            question = question,
            submitter = submitter,
            languageCode = languageCode,
            contentType = ExplanationContentType.TEXT,
            textContent = textContent,
            submissionStatus = ExplanationStatus.PENDING
        )

        return explanationRepository.save(explanation)
    }

    /**
     * Submit an audio or video explanation
     */
    fun submitFileExplanation(
        questionId: UUID,
        submitterId: UUID,
        languageCode: String,
        contentType: ExplanationContentType,
        fileId: UUID
    ): ExplanationSubmission {
        require(contentType in listOf(ExplanationContentType.AUDIO, ExplanationContentType.VIDEO)) {
            "Content type must be AUDIO or VIDEO for file submissions"
        }

        // Validate question exists
        val question = questionRepository.findById(questionId).orElseThrow {
            IllegalArgumentException("Question not found: $questionId")
        }

        // Validate submitter exists
        val submitter = userRepository.findById(submitterId).orElseThrow {
            IllegalArgumentException("User not found: $submitterId")
        }

        // Validate file exists and is safe
        val file = fileUploadRepository.findById(fileId).orElseThrow {
            IllegalArgumentException("File not found: $fileId")
        }

        require(file.uploader.id == submitterId) {
            "File must be uploaded by the same user submitting the explanation"
        }

        require(file.isSafe()) {
            "File has not passed security checks"
        }

        // Validate file type matches content type
        when (contentType) {
            ExplanationContentType.AUDIO -> require(file.uploadType == FileUploadType.AUDIO) {
                "File type must be AUDIO"
            }
            ExplanationContentType.VIDEO -> require(file.uploadType == FileUploadType.VIDEO) {
                "File type must be VIDEO"
            }
            else -> {}
        }

        // Create explanation
        val explanation = ExplanationSubmission(
            question = question,
            submitter = submitter,
            languageCode = languageCode,
            contentType = contentType,
            fileUrl = file.filePath,
            fileSizeBytes = file.fileSizeBytes,
            fileMimeType = file.mimeType,
            submissionStatus = ExplanationStatus.PENDING
        )

        return explanationRepository.save(explanation)
    }

    /**
     * Get explanation by ID
     */
    fun getExplanationById(id: UUID): ExplanationSubmission? {
        return explanationRepository.findById(id).orElse(null)
    }

    /**
     * Get all explanations for a question
     */
    fun getExplanationsForQuestion(
        questionId: UUID,
        status: ExplanationStatus? = null,
        languageCode: String? = null
    ): List<ExplanationSubmission> {
        return when {
            status != null && languageCode != null -> {
                explanationRepository.findByQuestionIdAndSubmissionStatus(questionId, status)
                    .filter { it.languageCode == languageCode }
            }
            status != null -> {
                explanationRepository.findByQuestionIdAndSubmissionStatus(questionId, status)
            }
            languageCode != null -> {
                explanationRepository.findByQuestionIdAndLanguageCode(questionId, languageCode)
            }
            else -> {
                explanationRepository.findByQuestionId(questionId)
            }
        }
    }

    /**
     * Get approved explanations for a question (public view)
     */
    fun getApprovedExplanations(questionId: UUID, languageCode: String): List<ExplanationSubmission> {
        return explanationRepository.findByQuestionIdAndSubmissionStatus(questionId, ExplanationStatus.APPROVED)
            .filter { it.languageCode == languageCode }
            .sortedByDescending { it.qualityScore ?: 0 }
    }

    /**
     * Get user's submissions
     */
    fun getUserSubmissions(userId: UUID): List<ExplanationSubmission> {
        return explanationRepository.findBySubmitterId(userId)
            .sortedByDescending { it.submittedAt }
    }

    /**
     * Update explanation status
     */
    fun updateStatus(id: UUID, newStatus: ExplanationStatus): ExplanationSubmission {
        val explanation = explanationRepository.findById(id).orElseThrow {
            IllegalArgumentException("Explanation not found: $id")
        }

        explanation.submissionStatus = newStatus
        explanation.updatedAt = Instant.now()

        when (newStatus) {
            ExplanationStatus.UNDER_REVIEW -> {
                explanation.reviewedAt = Instant.now()
            }
            ExplanationStatus.APPROVED -> {
                explanation.approvedAt = Instant.now()
            }
            else -> {}
        }

        return explanationRepository.save(explanation)
    }

    /**
     * Increment view count
     */
    fun recordView(id: UUID) {
        val explanation = explanationRepository.findById(id).orElse(null) ?: return
        explanation.viewCount++
        explanation.updatedAt = Instant.now()
        explanationRepository.save(explanation)
    }

    /**
     * Update quality score
     */
    fun updateQualityScore(id: UUID): ExplanationSubmission {
        val explanation = explanationRepository.findById(id).orElseThrow {
            IllegalArgumentException("Explanation not found: $id")
        }

        val calculatedScore = explanation.calculateQualityScore()
        explanation.qualityScore = calculatedScore
        explanation.updatedAt = Instant.now()

        return explanationRepository.save(explanation)
    }

    /**
     * Delete explanation (admin only)
     */
    fun deleteExplanation(id: UUID): Boolean {
        if (!explanationRepository.existsById(id)) {
            return false
        }
        explanationRepository.deleteById(id)
        return true
    }

    /**
     * Get explanations pending moderation
     */
    fun getPendingExplanations(): List<ExplanationSubmission> {
        return explanationRepository.findBySubmissionStatus(ExplanationStatus.PENDING)
            .sortedBy { it.submittedAt }
    }

    /**
     * Get moderation queue (prioritized)
     */
    fun getModerationQueue(): List<ExplanationSubmission> {
        val statuses = listOf(
            ExplanationStatus.PENDING,
            ExplanationStatus.UNDER_REVIEW,
            ExplanationStatus.FLAGGED
        )
        return explanationRepository.findModerationQueue(statuses)
    }
}