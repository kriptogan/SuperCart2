package com.example.supercart2.models

import java.util.UUID

data class SubCategory(
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val categoryId: String,
    val viewOrder: Int
)
