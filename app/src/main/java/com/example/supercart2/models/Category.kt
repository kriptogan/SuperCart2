package com.example.supercart2.models

import java.util.UUID

data class Category(
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val viewOrder: Int
)
