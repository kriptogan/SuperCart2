package com.example.supercart2.models

import java.util.UUID

data class Category(
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val default: Boolean = false,
    val viewOrder: Int = 0,
    val groupId: String? = null,
    val protected: Boolean = false
)
