package com.catechism.platform.service

import com.catechism.platform.domain.explanation.*
import com.catechism.platform.repository.AppUserRepository
import com.catechism.platform.repository.explanation.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

@Service
@Transactional
class FileUploadService(
    private val fileUploadRepository: FileUploadRepository,
    private val userRepository: AppUserRepository,
    @Value("\${app.file-upload.base-path:./uploads}") private val baseUploadPath: String,
    @Value("\${app.file-upload.max-file-size:104857600}") private val maxFileSize: Long = 100 * 1024 * 1024 // 100MB
) {

    // Allowed MIME types
    private val allowedAudioTypes = setOf(
        "audio/mpeg", "audio/mp3", "audio/mp4", "audio/m4a",
        "audio/wav", "audio/ogg", "audio/webm"
    )

    private val allowedVideoTypes = setOf(
        "video/mp4", "video/mpeg", "video/webm",
        "video/ogg", "video/quicktime"
    )

    private val allowedImageTypes = setOf(
        "image/jpeg", "image/jpg", "image/png",
        "image/gif", "image/webp"
    )

    init {
        // Create upload directory if it doesn't exist
        val uploadDir = File(baseUploadPath)
        if (!uploadDir.exists()) {
            uploadDir.mkdirs()
        }
    }

    /**
     * Upload a file
     */
    fun uploadFile(
        file: MultipartFile,
        uploaderId: UUID,
        uploadType: FileUploadType
    ): FileUpload {
        // Validate user exists
        val uploader = userRepository.findById(uploaderId).orElseThrow {
            IllegalArgumentException("User not found: $uploaderId")
        }

        // Validate file
        validateFile(file, uploadType)

        // Generate unique file name
        val originalFilename = file.originalFilename ?: throw IllegalArgumentException("File name is required")
        val extension = originalFilename.substringAfterLast(".", "")
        val uniqueFilename = "${UUID.randomUUID()}.$extension"

        // Create subdirectory for upload type
        val typeDir = File(baseUploadPath, uploadType.name.lowercase())
        if (!typeDir.exists()) {
            typeDir.mkdirs()
        }

        // Save file to disk
        val filePath = Paths.get(typeDir.absolutePath, uniqueFilename)
        Files.copy(file.inputStream, filePath, StandardCopyOption.REPLACE_EXISTING)

        // Create file upload record
        val fileUpload = FileUpload(
            uploader = uploader,
            fileName = originalFilename,
            filePath = filePath.toString(),
            fileSizeBytes = file.size,
            mimeType = file.contentType ?: "application/octet-stream",
            uploadType = uploadType,
            processingStatus = ProcessingStatus.PENDING,
            virusScanStatus = VirusScanStatus.PENDING
        )

        val savedFile = fileUploadRepository.save(fileUpload)

        // TODO: Trigger async virus scanning
        // TODO: Trigger async processing (transcoding, etc.)

        return savedFile
    }

    /**
     * Validate file before upload
     */
    private fun validateFile(file: MultipartFile, uploadType: FileUploadType) {
        // Check file size
        if (file.size > maxFileSize) {
            throw IllegalArgumentException(
                "File size exceeds maximum allowed size of ${maxFileSize / (1024 * 1024)}MB"
            )
        }

        // Check if file is empty
        if (file.isEmpty) {
            throw IllegalArgumentException("File is empty")
        }

        // Validate MIME type
        val mimeType = file.contentType ?: throw IllegalArgumentException("File type cannot be determined")

        val allowedTypes = when (uploadType) {
            FileUploadType.AUDIO -> allowedAudioTypes
            FileUploadType.VIDEO -> allowedVideoTypes
            FileUploadType.IMAGE -> allowedImageTypes
            FileUploadType.DOCUMENT -> emptySet() // Not implemented yet
        }

        if (mimeType !in allowedTypes) {
            throw IllegalArgumentException(
                "File type $mimeType not allowed for $uploadType uploads. " +
                        "Allowed types: ${allowedTypes.joinToString(", ")}"
            )
        }
    }

    /**
     * Get file by ID
     */
    fun getFileById(fileId: UUID): FileUpload? {
        return fileUploadRepository.findById(fileId).orElse(null)
    }

    /**
     * Get files by uploader
     */
    fun getFilesByUploader(uploaderId: UUID): List<FileUpload> {
        return fileUploadRepository.findByUploaderId(uploaderId)
            .sortedByDescending { it.uploadedAt }
    }

    /**
     * Update file processing status
     */
    fun updateProcessingStatus(fileId: UUID, status: ProcessingStatus): FileUpload {
        val file = fileUploadRepository.findById(fileId).orElseThrow {
            IllegalArgumentException("File not found: $fileId")
        }

        file.processingStatus = status
        return fileUploadRepository.save(file)
    }

    /**
     * Update virus scan status
     */
    fun updateVirusScanStatus(fileId: UUID, status: VirusScanStatus): FileUpload {
        val file = fileUploadRepository.findById(fileId).orElseThrow {
            IllegalArgumentException("File not found: $fileId")
        }

        file.virusScanStatus = status
        return fileUploadRepository.save(file)
    }

    /**
     * Mark file as public
     */
    fun setPublic(fileId: UUID, isPublic: Boolean): FileUpload {
        val file = fileUploadRepository.findById(fileId).orElseThrow {
            IllegalArgumentException("File not found: $fileId")
        }

        file.isPublic = isPublic
        return fileUploadRepository.save(file)
    }

    /**
     * Record file access
     */
    fun recordAccess(fileId: UUID) {
        val file = fileUploadRepository.findById(fileId).orElse(null) ?: return
        file.recordAccess()
        fileUploadRepository.save(file)
    }

    /**
     * Delete file
     */
    fun deleteFile(fileId: UUID): Boolean {
        val file = fileUploadRepository.findById(fileId).orElse(null) ?: return false

        // Delete physical file
        try {
            val path = Paths.get(file.filePath)
            Files.deleteIfExists(path)
        } catch (e: Exception) {
            // Log error but continue with database deletion
            println("Error deleting physical file: ${e.message}")
        }

        // Delete database record
        fileUploadRepository.delete(file)
        return true
    }

    /**
     * Get pending files for processing
     */
    fun getPendingFiles(): List<FileUpload> {
        return fileUploadRepository.findByProcessingStatus(ProcessingStatus.PENDING)
            .sortedBy { it.uploadedAt }
    }

    /**
     * Get files pending virus scan
     */
    fun getFilesForVirusScan(): List<FileUpload> {
        return fileUploadRepository.findByVirusScanStatus(VirusScanStatus.PENDING)
            .sortedBy { it.uploadedAt }
    }

    /**
     * Get infected files
     */
    fun getInfectedFiles(): List<FileUpload> {
        return fileUploadRepository.findByVirusScanStatus(VirusScanStatus.INFECTED)
    }

    /**
     * Get file content (for serving)
     */
    fun getFileContent(fileId: UUID): ByteArray? {
        val file = fileUploadRepository.findById(fileId).orElse(null) ?: return null

        // Check if file is safe to serve
        if (!file.isSafe()) {
            throw IllegalStateException("File is not safe to serve")
        }

        val path = Paths.get(file.filePath)
        if (!Files.exists(path)) {
            return null
        }

        // Record access
        recordAccess(fileId)

        return Files.readAllBytes(path)
    }

    /**
     * Get storage statistics
     */
    fun getStorageStatistics(): StorageStatistics {
        val allFiles = fileUploadRepository.findAll()

        val totalSize = allFiles.sumOf { it.fileSizeBytes }
        val audioSize = allFiles.filter { it.uploadType == FileUploadType.AUDIO }.sumOf { it.fileSizeBytes }
        val videoSize = allFiles.filter { it.uploadType == FileUploadType.VIDEO }.sumOf { it.fileSizeBytes }
        val imageSize = allFiles.filter { it.uploadType == FileUploadType.IMAGE }.sumOf { it.fileSizeBytes }

        return StorageStatistics(
            totalFiles = allFiles.size,
            totalSizeBytes = totalSize,
            audioFiles = allFiles.count { it.uploadType == FileUploadType.AUDIO },
            audioSizeBytes = audioSize,
            videoFiles = allFiles.count { it.uploadType == FileUploadType.VIDEO },
            videoSizeBytes = videoSize,
            imageFiles = allFiles.count { it.uploadType == FileUploadType.IMAGE },
            imageSizeBytes = imageSize
        )
    }
}

/**
 * Storage statistics
 */
data class StorageStatistics(
    val totalFiles: Int,
    val totalSizeBytes: Long,
    val audioFiles: Int,
    val audioSizeBytes: Long,
    val videoFiles: Int,
    val videoSizeBytes: Long,
    val imageFiles: Int,
    val imageSizeBytes: Long
)