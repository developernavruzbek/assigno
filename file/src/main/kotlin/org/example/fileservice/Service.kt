package uz.zero.fileservice

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.security.SecureRandom
import java.time.LocalDate
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource

@Service
class FileService(
    private val fileRepo: FileRepo,
    @Value("\${file.upload-dir:uploads}") private val uploadDir: String,
    @Value("\${file.max-size:10485760}") private val maxSize: Long // 10MB
) {

    private val log = LoggerFactory.getLogger(javaClass)

    init {
        val path = basePath()
        if (!Files.exists(path)) {
            Files.createDirectories(path)
            log.info("Asosiy yuklash papkasi yaratildi: {}", path.toAbsolutePath())
        }
    }

    companion object {
        private const val CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        private val RANDOM = SecureRandom()
        private const val MAX_RETRIES = 10
    }

    @Transactional
    fun upload(file: MultipartFile, taskId: Long): File {
        log.info("Fayl yuklash boshlandi: nomi={}, hajmi={}, taskId={}", file.originalFilename, file.size, taskId)

        if (file.isEmpty) {
            log.warn("Fayl yuklashda xatolik: fayl bo'sh. taskId={}", taskId)
            throw FileEmptyException()
        }
        if (file.size > maxSize) {
            log.warn("Fayl yuklashda xatolik: hajmi ruxsat etilgandan ko'p ({} > {}). taskId={}", file.size, maxSize, taskId)
            throw FileTooLargeException()
        }

        val originalName = file.originalFilename ?: "unnamed"
        val mediaType = resolveMediaType(file.contentType)
        val keyName = generateUniqueKey() // generateFileKey o'rniga uniquekey ishlatgan ma'qul

        val targetPath = saveToDisk(file, taskId, keyName)

        return try {
            val savedFile = fileRepo.save(
                File(
                    taskId = taskId,
                    type = mediaType,
                    orgName = originalName,
                    keyName = keyName,
                    path = targetPath.toString(),
                    size = file.size
                )
            )
            log.info("Fayl muvaffaqiyatli saqlandi: keyName={}, dbId={}", keyName, savedFile.id)
            savedFile
        } catch (ex: Exception) {
            log.error("Faylni bazaga yozishda xatolik: {}. Diskdan fayl o'chirilmoqda...", ex.message)
            Files.deleteIfExists(targetPath)
            throw FileUploadFailedException(message = ex.message)
        }
    }

    fun download(keyName: String): Resource {
        log.info("Faylni yuklab olish uchun tayyorlanmoqda: keyName={}", keyName)

        val fileMetadata = getByKey(keyName)
        val path = Paths.get(fileMetadata.path)
        val resource = UrlResource(path.toUri())

        if (!resource.exists() || !resource.isReadable) {
            log.error("Download error: file is missing or not readable on disk. keyName={}, path={}", keyName, fileMetadata.path)
            throw FileNotFoundException(message = "File not found or not readable: $keyName")
        }

        log.debug("Fayl muvaffaqiyatli topildi: {}", fileMetadata.orgName)
        return resource
    }

    fun getByKey(keyName: String): File {
        log.debug("Fayl qidirilmoqda: keyName={}", keyName)
        return fileRepo.findByKeyName(keyName)
            ?: throw FileNotFoundException(message = "File not found: $keyName").also {
                log.warn("Fayl topilmadi: keyName={}", keyName)
            }
    }

    fun getAllByTaskId(taskId: Long): List<File> {
        log.debug("Vazifaga tegishli barcha fayllar olinmoqda: taskId={}", taskId)
        return fileRepo.findAllByTaskIdAndDeletedFalse(taskId)
    }

    @Transactional
    fun delete(id: Long) {
        log.info("Faylni o'chirish so'rovi: id={}", id)
        val file = fileRepo.findById(id)
            .orElseThrow {
                log.warn("O'chirish uchun fayl topilmadi: id={}", id)
                FileNotFoundException(message = "File not found: $id")
            }

        file.deleted = true
        fileRepo.save(file)

        try {
            val path = Paths.get(file.path)
            if (Files.deleteIfExists(path)) {
                log.info("Fayl diskdan o'chirildi: path={}", file.path)
            }
        } catch (ex: Exception) {
            log.error("Faylni diskdan o'chirishda xatolik (id={}): {}", id, ex.message)
        }
    }

    @Transactional
    fun deleteByTaskId(taskId: Long) {
        log.info("Vazifaga tegishli barcha fayllarni o'chirish: taskId={}", taskId)
        val files = fileRepo.findAllByTaskIdAndDeletedFalse(taskId)

        files.forEach { file ->
            file.deleted = true
            try {
                Files.deleteIfExists(Paths.get(file.path))
            } catch (ex: Exception) {
                log.error("Faylni o'chirishda xatolik (id={}): {}", file.id, ex.message)
            }
        }

        fileRepo.saveAll(files)
        log.info("{} ta fayl o'chirildi. taskId={}", files.size, taskId)
    }

    private fun saveToDisk(file: MultipartFile, taskId: Long, keyName: String): Path {
        val ownerDir = basePath().resolve(taskId.toString())
        if (!Files.exists(ownerDir)) {
            Files.createDirectories(ownerDir)
        }

        val target = ownerDir.resolve(keyName)
        try {
            Files.copy(file.inputStream, target, StandardCopyOption.REPLACE_EXISTING)
            log.debug("Fayl diskka yozildi: {}", target)
        } catch (ex: Exception) {
            log.error("Diskka yozishda xatolik: taskId={}, keyName={}, error={}", taskId, keyName, ex.message)
            throw FileUploadFailedException(message = "Disk write failed")
        }
        return target
    }

    private fun basePath(): Path =
        Paths.get(System.getProperty("user.home")).resolve(uploadDir)

    fun generateUniqueKey(): String {
        repeat(MAX_RETRIES) { attempt ->
            val key = generateFileKey()
            if (!fileRepo.existsByKeyName(key)) return key
            log.warn("Kalit takrorlandi (duplication), qayta urinish: {}/{}", attempt + 1, MAX_RETRIES)
        }
        log.error("Unikal kalit yaratib bo'lmadi. Maksimal urinishlar soni tugadi.")
        throw FileKeyGenerationException(
            ErrorCodes.FILE_KEY_GENERATION,
            "Maximum number of retries ($MAX_RETRIES) exceeded."
        )
    }

    private fun generateFileKey(): String {
        val randomPart = StringBuilder(6).apply {
            repeat(6) {
                append(CHARS[RANDOM.nextInt(CHARS.length)])
            }
        }.toString()

        val now = LocalDate.now()
        val year = (now.year % 100).toString()
        val month = "%02d".format(now.monthValue)
        val day = "%02d".format(now.dayOfMonth)

        return "$year$randomPart$month$day"
    }

    private fun resolveMediaType(contentType: String?): MediaType {
        return when {
            contentType == null -> {
                log.warn("ContentType bo'sh, xatolik qaytarilmoqda.")
                throw InvalidFileTypeException()
            }
            contentType.startsWith("image") -> MediaType.IMAGE
            contentType.startsWith("video") -> MediaType.VIDEO
            else -> MediaType.DOCUMENT
        }
    }
}