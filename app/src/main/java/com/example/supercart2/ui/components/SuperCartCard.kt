package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartShapes
import com.example.supercart2.ui.theme.SuperCartSpacing

@Composable
fun SuperCartElevatedCard(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = SuperCartSpacing.md),
        colors = CardDefaults.cardColors(
            containerColor = SuperCartColors.white
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        shape = SuperCartShapes.medium
    ) {
        Box(
            modifier = Modifier.padding(SuperCartSpacing.md)
        ) {
            content()
        }
    }
}

@Composable
fun SuperCartLightGreenCard(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = SuperCartSpacing.md),
        colors = CardDefaults.cardColors(
            containerColor = SuperCartColors.lightGreen
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        shape = SuperCartShapes.medium
    ) {
        Box(
            modifier = Modifier.padding(SuperCartSpacing.md)
        ) {
            content()
        }
    }
}
