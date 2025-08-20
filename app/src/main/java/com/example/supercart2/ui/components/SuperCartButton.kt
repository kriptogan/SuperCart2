package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing

@Composable
fun SuperCartPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SuperCartSpacing.sm),
        colors = ButtonDefaults.buttonColors(
            containerColor = SuperCartColors.primaryGreen,
            contentColor = SuperCartColors.white
        ),
        enabled = enabled
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SuperCartSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SuperCartSpacing.sm),
        colors = ButtonDefaults.buttonColors(
            containerColor = SuperCartColors.primaryBlue,
            contentColor = SuperCartColors.white
        ),
        enabled = enabled
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SuperCartOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SuperCartSpacing.sm),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = SuperCartColors.primaryGreen
        ),
        enabled = enabled
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Medium
        )
    }
}
