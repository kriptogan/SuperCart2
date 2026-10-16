package com.example.supercart2.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.supercart2.R

/**
 * Returns the localized display name for a category.
 * Default categories (Other, Vegetables, Fruits) are translated; other names are shown as-is.
 */
@Composable
fun localizedCategoryDisplayName(name: String): String = when (name) {
    "Other" -> stringResource(R.string.default_category_other)
    "Vegetables" -> stringResource(R.string.default_category_vegetables)
    "Fruits" -> stringResource(R.string.default_category_fruits)
    else -> name
}

/**
 * Returns the localized display name for a sub-category.
 * The default sub-category "General" is translated; other names are shown as-is.
 */
@Composable
fun localizedSubCategoryDisplayName(name: String): String = when (name) {
    "General" -> stringResource(R.string.general)
    else -> name
}
