package uz.zero.fileservice

enum class MediaType {
    IMAGE,
    VIDEO,
    DOCUMENT,
}

enum class ErrorCodes(val code: Int, val msg: String) {
    FILE_UPLOAD_FAILED(600, "File upload failed"),
    FILE_NOT_FOUND(601, "File not found"),
    FILE_EMPTY(602, "File cannot be empty"),
    INVALID_FILE_TYPE(603, "Invalid file type"),
    FILE_TOO_LARGE(604, "File size exceeds maximum allowed"),
    FILE_KEY_GENERATION(655, "File key generation failed"),

    // Validation errors (111)
    VALIDATION_EXCEPTION(111, "Validation error occurred"),

    // System errors (555)
    INTERNAL_SERVER_ERROR(555, "Internal server error occurred")
}