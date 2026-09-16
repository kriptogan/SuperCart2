package com.example.supercart2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.example.supercart2.R
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing

/** Fixed light-green header area at the top of the Stores sub-screens. */
@Composable
internal fun StoresScreenHeaderColumn(content: @Composable ColumnScope.() -> Unit) {
    val modifier = Modifier.fillMaxWidth().background(SuperCartColors.lightGreen)
        .padding(start = SuperCartSpacing.md, end = SuperCartSpacing.md, top = SuperCartSpacing.xl)
    Column(modifier = modifier, content = content)
}

/** Back button, title and optional actions of a Stores sub-screen header. */
@Composable
internal fun StoresScreenTitleRow(bottomPadding: Dp, content: @Composable RowScope.() -> Unit) {
    val modifier = Modifier.fillMaxWidth().padding(bottom = bottomPadding)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, content = content)
}

@Composable
internal fun StoresScreenBackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), tint = SuperCartColors.black)
    }
}

@Composable
internal fun StoresScreenTitle(text: String, modifier: Modifier) {
    Text(text, modifier, color = SuperCartColors.primaryGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
}

/** Nearly transparent light-green card background (empty-list card, bought grocery cards). */
@Composable
internal fun storesScreenFaintCardColors(): CardColors =
    CardDefaults.cardColors(containerColor = SuperCartColors.lightGreen.copy(alpha = 0.1f))
