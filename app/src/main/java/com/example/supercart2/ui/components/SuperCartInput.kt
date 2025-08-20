package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartShapes
import com.example.supercart2.ui.theme.SuperCartSpacing

@Composable
fun SuperCartOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    height: Int = 56
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .padding(bottom = SuperCartSpacing.md),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SuperCartColors.primaryGreen,
            unfocusedBorderColor = SuperCartColors.gray,
            focusedLabelColor = SuperCartColors.primaryGreen,
            cursorColor = SuperCartColors.primaryGreen
        ),
        shape = SuperCartShapes.small,
        maxLines = maxLines
    )
}

@Composable
fun SuperCartMultilineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 5
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(bottom = SuperCartSpacing.md),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SuperCartColors.primaryGreen,
            unfocusedBorderColor = SuperCartColors.gray,
            focusedLabelColor = SuperCartColors.primaryGreen,
            cursorColor = SuperCartColors.primaryGreen
        ),
        shape = SuperCartShapes.small,
        maxLines = maxLines
    )
}
