package com.example.supercart2.models

import java.util.UUID
import java.time.LocalDateTime

data class Store(
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val address: String = "",
    val viewOrder: Int = 0,
    val lastUpdate: LocalDateTime = LocalDateTime.now(),
    val deleted: Boolean = false
)
