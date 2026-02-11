package uz.zero.fileservice

import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping()
class FileController(
    private val fileService: FileService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/upload/{taskId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadFile(
        @RequestParam("file") file: MultipartFile,
        @PathVariable taskId: Long
    ): File {
        log.info("REST request to upload file for task: {}", taskId)
        return fileService.upload(file, taskId)
    }

    @GetMapping("/{keyName}", produces = [MediaType.APPLICATION_OCTET_STREAM_VALUE])
    fun downloadFile(@PathVariable keyName: String): Resource {
        log.info("REST request to download file: {}", keyName)
        return fileService.download(keyName)
    }

    @GetMapping("/task/{taskId}")
    fun getFilesByTask(@PathVariable taskId: Long): List<File> {
        log.info("REST request to get all files for task: {}", taskId)
        return fileService.getAllByTaskId(taskId)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteFile(@PathVariable id: Long) {
        log.info("REST request to delete file: id={}", id)
        fileService.delete(id)
    }

    @DeleteMapping("/task/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteFilesByTask(@PathVariable taskId: Long) {
        log.info("REST request to delete all files for task: {}", taskId)
        fileService.deleteByTaskId(taskId)
    }
}