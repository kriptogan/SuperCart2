package com.kriptogan.supercart2.classes

import java.util.UUID

/**
 * Represents a sharing group in the SuperCart app
 * @param uuid Unique identifier for the group
 * @param code 8-digit unique code for group sharing
 */
data class Group(
    val uuid: String = UUID.randomUUID().toString(),
    val code: String = generateUniqueCode()
) {
    companion object {
        /**
         * Generates a unique 8-digit code for group sharing
         * @return 8-digit string code
         */
        private fun generateUniqueCode(): String {
            return (10000000..99999999).random().toString()
        }
    }
}
