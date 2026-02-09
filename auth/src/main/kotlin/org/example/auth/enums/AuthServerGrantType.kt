package org.example.auth.enums

enum class AuthServerGrantType(val key: String, val isActive: Boolean) {
    PASSWORD("password", true);

    companion object {
        fun findByKey(key: String): AuthServerGrantType? {
            return entries.find { it.key == key && it.isActive }
        }
    }
}