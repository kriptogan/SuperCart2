package com.example.supercart2.models

import java.util.UUID
import java.time.LocalDateTime

data class SubCategory(
    val uuid: String = UUID.randomUUID().toString(),
    val categoryId: String,
    val name: String,
    val protected: Boolean = false,
    val lastUpdate: LocalDateTime = LocalDateTime.now(),
    val isDeleted: Boolean = false
)