package com.catechism.platform.service

import com.catechism.platform.domain.*
import com.catechism.platform.repository.*
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.InputStreamReader
import java.util.*

@Service
@Transactional
class ContentImportService(
    private val bookletRepository: CatechismBookletRepository,
    private val questionRepository: CatechismQuestionRepository,
    private val questionTranslationRepository: CatechismQuestionTranslationRepository,
    private val actRepository: CatechismActRepository,
    private val actTranslationRepository: CatechismActTranslationRepository,
    private val subtitleRepository: CatechismActSubtitleRepository,
    private val subtitleTranslationRepository: CatechismActSubtitleTranslationRepository,
    private val cccParagraphRepository: CCCParagraphRepository,
    private val cccTranslationRepository: CCCParagraphTranslationRepository,
    private val bibleReferenceRepository: BibleReferenceRepository,
    private val bibleTranslationRepository: BibleReferenceTranslationRepository,
    private val questionCCCRepository: QuestionCCCReferenceRepository,
    private val questionBibleRepository: QuestionBibleReferenceRepository
) {

    // =====================================================
    // Questions Import
    // =====================================================

    /**
     * Expected CSV columns:
     * question_number, category, language_code, question_text, answer_text
     *
     * Optional columns:
     * act_number, subtitle_number, is_official
     */
    fun importQuestions(
        bookletId: UUID,
        file: MultipartFile,
        languageCode: String = "en"
    ): ImportResult {
        val booklet = bookletRepository.findById(bookletId).orElseThrow {
            IllegalArgumentException("Booklet not found: $bookletId")
        }

        val errors = mutableListOf<ImportError>()
        val created = mutableListOf<String>()
        val skipped = mutableListOf<String>()

        val csvFormat = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .setIgnoreEmptyLines(true)
            .build()

        val reader = InputStreamReader(file.inputStream)
        val parser = CSVParser(reader, csvFormat)

        for ((index, record) in parser.withIndex()) {
            val rowNum = index + 2 // +2 because header is row 1
            try {
                val questionNumber = record.get("question_number")?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid question_number")

                val questionText = record.get("question_text")
                    ?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("question_text is required")

                val answerText = record.get("answer_text")
                    ?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("answer_text is required")

                val lang = record.get("language_code")?.takeIf { it.isNotBlank() } ?: languageCode
                val category = record.get("category")?.takeIf { it.isNotBlank() }
                val isOfficial = record.get("is_official")?.toBooleanStrictOrNull() ?: true

                // Resolve optional subtitle
                val subtitle = resolveSubtitle(record, bookletId)

                // Check if question already exists in this booklet
                val existing = questionRepository.findByBookletIdAndQuestionNumber(bookletId, questionNumber)

                if (existing != null) {
                    // Add/update translation only
                    val existingTranslation = questionTranslationRepository
                        .findByQuestionIdAndLanguageCode(existing.id, lang)

                    if (existingTranslation != null) {
                        skipped.add("Row $rowNum: Q$questionNumber already has $lang translation")
                    } else {
                        val translation = CatechismQuestionTranslation(
                            question = existing,
                            languageCode = lang,
                            questionText = questionText,
                            answerText = answerText,
                            isOfficial = isOfficial
                        )
                        questionTranslationRepository.save(translation)
                        created.add("Row $rowNum: Added $lang translation for Q$questionNumber")
                    }
                } else {
                    // Create new question
                    val question = CatechismQuestion(
                        booklet = booklet,
                        subtitle = subtitle,
                        questionNumber = questionNumber,
                        category = category
                    )
                    val savedQuestion = questionRepository.save(question)

                    val translation = CatechismQuestionTranslation(
                        question = savedQuestion,
                        languageCode = lang,
                        questionText = questionText,
                        answerText = answerText,
                        isOfficial = isOfficial
                    )
                    questionTranslationRepository.save(translation)
                    created.add("Row $rowNum: Created Q$questionNumber")
                }
            } catch (e: Exception) {
                errors.add(ImportError(rowNum, e.message ?: "Unknown error"))
            }
        }

        parser.close()

        return ImportResult(
            totalRows = created.size + skipped.size + errors.size,
            created = created.size,
            skipped = skipped.size,
            failed = errors.size,
            errors = errors,
            messages = created + skipped
        )
    }

    // =====================================================
    // Acts & Subtitles Import
    // =====================================================

    /**
     * Expected CSV columns:
     * act_number, act_title, act_description, display_order, language_code
     */
    fun importActs(bookletId: UUID, file: MultipartFile): ImportResult {
        val booklet = bookletRepository.findById(bookletId).orElseThrow {
            IllegalArgumentException("Booklet not found: $bookletId")
        }

        val errors = mutableListOf<ImportError>()
        val created = mutableListOf<String>()
        val skipped = mutableListOf<String>()

        val csvFormat = CSVFormat.DEFAULT.builder()
            .setHeader().setSkipHeaderRecord(true).setTrim(true).setIgnoreEmptyLines(true).build()

        val parser = CSVParser(InputStreamReader(file.inputStream), csvFormat)

        for ((index, record) in parser.withIndex()) {
            val rowNum = index + 2
            try {
                val actNumber = record.get("act_number")?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid act_number")
                val title = record.get("act_title")?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("act_title is required")
                val description = record.get("act_description")?.takeIf { it.isNotBlank() }
                val displayOrder = record.get("display_order")?.toIntOrNull() ?: actNumber
                val lang = record.get("language_code")?.takeIf { it.isNotBlank() } ?: "en"

                val existing = actRepository.findByBookletIdOrderByDisplayOrder(bookletId)
                    .find { it.actNumber == actNumber }

                if (existing != null) {
                    val existingTranslation = actTranslationRepository
                        .findByActIdAndLanguageCode(existing.id, lang)
                    if (existingTranslation != null) {
                        skipped.add("Row $rowNum: Act $actNumber already has $lang translation")
                    } else {
                        actTranslationRepository.save(
                            CatechismActTranslation(act = existing, languageCode = lang, title = title, description = description)
                        )
                        created.add("Row $rowNum: Added $lang translation for Act $actNumber")
                    }
                } else {
                    val act = actRepository.save(
                        CatechismAct(booklet = booklet, actNumber = actNumber, displayOrder = displayOrder)
                    )
                    actTranslationRepository.save(
                        CatechismActTranslation(act = act, languageCode = lang, title = title, description = description)
                    )
                    created.add("Row $rowNum: Created Act $actNumber - $title")
                }
            } catch (e: Exception) {
                errors.add(ImportError(rowNum, e.message ?: "Unknown error"))
            }
        }

        parser.close()
        return ImportResult(
            totalRows = created.size + skipped.size + errors.size,
            created = created.size, skipped = skipped.size, failed = errors.size,
            errors = errors, messages = created + skipped
        )
    }

    /**
     * Expected CSV columns:
     * act_number, subtitle_number, subtitle_title, subtitle_description, display_order, language_code
     */
    fun importSubtitles(bookletId: UUID, file: MultipartFile): ImportResult {
        val errors = mutableListOf<ImportError>()
        val created = mutableListOf<String>()
        val skipped = mutableListOf<String>()

        val csvFormat = CSVFormat.DEFAULT.builder()
            .setHeader().setSkipHeaderRecord(true).setTrim(true).setIgnoreEmptyLines(true).build()

        val parser = CSVParser(InputStreamReader(file.inputStream), csvFormat)

        for ((index, record) in parser.withIndex()) {
            val rowNum = index + 2
            try {
                val actNumber = record.get("act_number")?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid act_number")
                val subtitleNumber = record.get("subtitle_number")?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid subtitle_number")
                val title = record.get("subtitle_title")?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("subtitle_title is required")
                val description = record.get("subtitle_description")?.takeIf { it.isNotBlank() }
                val displayOrder = record.get("display_order")?.toIntOrNull() ?: subtitleNumber
                val lang = record.get("language_code")?.takeIf { it.isNotBlank() } ?: "en"

                val act = actRepository.findByBookletIdOrderByDisplayOrder(bookletId)
                    .find { it.actNumber == actNumber }
                    ?: throw IllegalArgumentException("Act $actNumber not found in booklet")

                val existing = subtitleRepository.findByActIdOrderByDisplayOrder(act.id)
                    .find { it.subtitleNumber == subtitleNumber }

                if (existing != null) {
                    val existingTranslation = subtitleTranslationRepository
                        .findBySubtitleIdAndLanguageCode(existing.id, lang)
                    if (existingTranslation != null) {
                        skipped.add("Row $rowNum: Subtitle $subtitleNumber already has $lang translation")
                    } else {
                        subtitleTranslationRepository.save(
                            CatechismActSubtitleTranslation(subtitle = existing, languageCode = lang, title = title, description = description)
                        )
                        created.add("Row $rowNum: Added $lang translation for Subtitle $subtitleNumber")
                    }
                } else {
                    val subtitle = subtitleRepository.save(
                        CatechismActSubtitle(act = act, subtitleNumber = subtitleNumber, displayOrder = displayOrder)
                    )
                    subtitleTranslationRepository.save(
                        CatechismActSubtitleTranslation(subtitle = subtitle, languageCode = lang, title = title, description = description)
                    )
                    created.add("Row $rowNum: Created Subtitle $subtitleNumber - $title")
                }
            } catch (e: Exception) {
                errors.add(ImportError(rowNum, e.message ?: "Unknown error"))
            }
        }

        parser.close()
        return ImportResult(
            totalRows = created.size + skipped.size + errors.size,
            created = created.size, skipped = skipped.size, failed = errors.size,
            errors = errors, messages = created + skipped
        )
    }

    // =====================================================
    // CCC Paragraphs Import
    // =====================================================

    /**
     * Expected CSV columns:
     * paragraph_number, edition, language_code, paragraph_text, licensed
     */
    fun importCCCParagraphs(file: MultipartFile): ImportResult {
        val errors = mutableListOf<ImportError>()
        val created = mutableListOf<String>()
        val skipped = mutableListOf<String>()

        val csvFormat = CSVFormat.DEFAULT.builder()
            .setHeader().setSkipHeaderRecord(true).setTrim(true).setIgnoreEmptyLines(true).build()

        val parser = CSVParser(InputStreamReader(file.inputStream), csvFormat)

        for ((index, record) in parser.withIndex()) {
            val rowNum = index + 2
            try {
                val paragraphNumber = record.get("paragraph_number")?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid paragraph_number")
                val edition = record.get("edition")?.takeIf { it.isNotBlank() } ?: "2nd Edition"
                val lang = record.get("language_code")?.takeIf { it.isNotBlank() } ?: "en"
                val paragraphText = record.get("paragraph_text")?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("paragraph_text is required")
                val licensed = record.get("licensed")?.toBooleanStrictOrNull() ?: false

                var paragraph = cccParagraphRepository.findByParagraphNumber(paragraphNumber)

                if (paragraph == null) {
                    paragraph = cccParagraphRepository.save(
                        CCCParagraph(paragraphNumber = paragraphNumber, edition = edition)
                    )
                    created.add("Row $rowNum: Created CCC §$paragraphNumber")
                } else {
                    val existingTranslation = cccTranslationRepository
                        .findByCccParagraphIdAndLanguageCode(paragraph.id, lang)
                    if (existingTranslation != null) {
                        skipped.add("Row $rowNum: CCC §$paragraphNumber already has $lang translation")
                        continue
                    }
                }

                cccTranslationRepository.save(
                    CCCParagraphTranslation(
                        cccParagraph = paragraph,
                        languageCode = lang,
                        paragraphText = paragraphText,
                        licensed = licensed
                    )
                )
                if (!created.any { it.contains("§$paragraphNumber") }) {
                    created.add("Row $rowNum: Added $lang translation for CCC §$paragraphNumber")
                }
            } catch (e: Exception) {
                errors.add(ImportError(rowNum, e.message ?: "Unknown error"))
            }
        }

        parser.close()
        return ImportResult(
            totalRows = created.size + skipped.size + errors.size,
            created = created.size, skipped = skipped.size, failed = errors.size,
            errors = errors, messages = created + skipped
        )
    }

    // =====================================================
    // Bible References Import
    // =====================================================

    /**
     * Expected CSV columns:
     * book, chapter, verse_start, verse_end, translation, language_code, verse_text
     */
    fun importBibleReferences(file: MultipartFile): ImportResult {
        val errors = mutableListOf<ImportError>()
        val created = mutableListOf<String>()
        val skipped = mutableListOf<String>()

        val csvFormat = CSVFormat.DEFAULT.builder()
            .setHeader().setSkipHeaderRecord(true).setTrim(true).setIgnoreEmptyLines(true).build()

        val parser = CSVParser(InputStreamReader(file.inputStream), csvFormat)

        for ((index, record) in parser.withIndex()) {
            val rowNum = index + 2
            try {
                val book = record.get("book")?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("book is required")
                val chapter = record.get("chapter")?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid chapter")
                val verseStart = record.get("verse_start")?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid verse_start")
                val verseEnd = record.get("verse_end")?.toIntOrNull()
                val translation = record.get("translation")?.takeIf { it.isNotBlank() } ?: "RSV-CE"
                val lang = record.get("language_code")?.takeIf { it.isNotBlank() } ?: "en"
                val verseText = record.get("verse_text")?.takeIf { it.isNotBlank() }
                    ?: throw IllegalArgumentException("verse_text is required")

                val refLabel = "$book $chapter:$verseStart${verseEnd?.let { "-$it" } ?: ""}"

                var reference = bibleReferenceRepository
                    .findByBookAndChapterAndVerseStartAndVerseEndAndTranslation(book, chapter, verseStart, verseEnd, translation)

                if (reference == null) {
                    reference = bibleReferenceRepository.save(
                        BibleReference(book = book, chapter = chapter, verseStart = verseStart, verseEnd = verseEnd, translation = translation)
                    )
                    created.add("Row $rowNum: Created $refLabel ($translation)")
                } else {
                    val existingTranslation = bibleTranslationRepository
                        .findByBibleReferenceIdAndLanguageCode(reference.id, lang)
                    if (existingTranslation != null) {
                        skipped.add("Row $rowNum: $refLabel already has $lang translation")
                        continue
                    }
                }

                bibleTranslationRepository.save(
                    BibleReferenceTranslation(bibleReference = reference, languageCode = lang, verseText = verseText)
                )
                if (!created.any { it.contains(refLabel) }) {
                    created.add("Row $rowNum: Added $lang translation for $refLabel")
                }
            } catch (e: Exception) {
                errors.add(ImportError(rowNum, e.message ?: "Unknown error"))
            }
        }

        parser.close()
        return ImportResult(
            totalRows = created.size + skipped.size + errors.size,
            created = created.size, skipped = skipped.size, failed = errors.size,
            errors = errors, messages = created + skipped
        )
    }

    // =====================================================
    // Question Links Import
    // =====================================================

    /**
     * Expected CSV columns:
     * question_number, ccc_paragraph_number, reference_order
     */
    fun importQuestionCCCLinks(bookletId: UUID, file: MultipartFile): ImportResult {
        val errors = mutableListOf<ImportError>()
        val created = mutableListOf<String>()
        val skipped = mutableListOf<String>()

        val csvFormat = CSVFormat.DEFAULT.builder()
            .setHeader().setSkipHeaderRecord(true).setTrim(true).setIgnoreEmptyLines(true).build()

        val parser = CSVParser(InputStreamReader(file.inputStream), csvFormat)

        for ((index, record) in parser.withIndex()) {
            val rowNum = index + 2
            try {
                val questionNumber = record.get("question_number")?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid question_number")
                val paragraphNumber = record.get("ccc_paragraph_number")?.toIntOrNull()
                    ?: throw IllegalArgumentException("Invalid ccc_paragraph_number")
                val order = record.get("reference_order")?.toIntOrNull() ?: 0

                val question = questionRepository.findByBookletIdAndQuestionNumber(bookletId, questionNumber)
                    ?: throw IllegalArgumentException("Q$questionNumber not found in booklet")
                val paragraph = cccParagraphRepository.findByParagraphNumber(paragraphNumber)
                    ?: throw IllegalArgumentException("CCC §$paragraphNumber not found")

                val existing = questionCCCRepository
                    .findByQuestionIdOrderByReferenceOrder(question.id)
                    .find { it.cccParagraph.paragraphNumber == paragraphNumber }

                if (existing != null) {
                    skipped.add("Row $rowNum: Q$questionNumber already linked to CCC §$paragraphNumber")
                } else {
                    questionCCCRepository.save(
                        QuestionCCCReference(question = question, cccParagraph = paragraph, referenceOrder = order)
                    )
                    created.add("Row $rowNum: Linked Q$questionNumber → CCC §$paragraphNumber")
                }
            } catch (e: Exception) {
                errors.add(ImportError(rowNum, e.message ?: "Unknown error"))
            }
        }

        parser.close()
        return ImportResult(
            totalRows = created.size + skipped.size + errors.size,
            created = created.size, skipped = skipped.size, failed = errors.size,
            errors = errors, messages = created + skipped
        )
    }

    // =====================================================
    // Helpers
    // =====================================================

    private fun resolveSubtitle(record: org.apache.commons.csv.CSVRecord, bookletId: UUID): CatechismActSubtitle? {
        return try {
            val actNumber = record.get("act_number")?.toIntOrNull() ?: return null
            val subtitleNumber = record.get("subtitle_number")?.toIntOrNull() ?: return null
            val act = actRepository.findByBookletIdOrderByDisplayOrder(bookletId)
                .find { it.actNumber == actNumber } ?: return null
            subtitleRepository.findByActIdOrderByDisplayOrder(act.id)
                .find { it.subtitleNumber == subtitleNumber }
        } catch (e: Exception) {
            null
        }
    }
}

// =====================================================
// Result Types
// =====================================================

data class ImportResult(
    val totalRows: Int,
    val created: Int,
    val skipped: Int,
    val failed: Int,
    val errors: List<ImportError>,
    val messages: List<String>
) {
    val success: Boolean get() = failed == 0
    val summary: String get() = "Total: $totalRows | Created: $created | Skipped: $skipped | Failed: $failed"
}

data class ImportError(
    val row: Int,
    val message: String
)