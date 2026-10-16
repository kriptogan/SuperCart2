package com.example.supercart2.models

import java.util.UUID
import java.time.LocalDateTime

data class Category(
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val default: Boolean = false,
    val viewOrder: Int = 0,
    val protected: Boolean = false,
    val lastUpdate: LocalDateTime = LocalDateTime.now(),
    val deleted: Boolean = false  // Changed from isDeleted to deleted
)