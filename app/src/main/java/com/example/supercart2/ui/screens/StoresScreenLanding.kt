package com.example.supercart2.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.supercart2.R
import com.example.supercart2.ui.theme.SuperCartColors
import com.example.supercart2.ui.theme.SuperCartSpacing

/** Stores tab entry: "Select a Store" and "View Groceries" option cards. */
@Composable
internal fun StoresLandingView(onViewGroceries: () -> Unit, onSelectStore: () -> Unit) {
    val boxModifier = Modifier.fillMaxSize().background(SuperCartColors.lightGreen)
        .padding(horizontal = SuperCartSpacing.lg, vertical = SuperCartSpacing.xxl)
    Box(modifier = boxModifier, contentAlignment = Alignment.Center) {
        StoresScreenLandingColumn(onViewGroceries, onSelectStore)
    }
}

@Composable
private fun StoresScreenLandingColumn(onViewGroceries: () -> Unit, onSelectStore: () -> Unit) {
    val spacing = Arrangement.spacedBy(SuperCartSpacing.lg)
    Column(Modifier.fillMaxWidth(), verticalArrangement = spacing, horizontalAlignment = Alignment.CenterHorizontally) {
        StoresScreenLandingHeader()
        LandingOptionCard(titleRes = R.string.select_a_store, descRes = R.string.select_a_store_desc, onSelectStore)
        LandingOptionCard(titleRes = R.string.view_groceries, descRes = R.string.view_groceries_desc, onViewGroceries)
    }
}

/** Store icon, "Stores" title and a spacer, emitted as separate children of the landing column. */
@Composable
private fun StoresScreenLandingHeader() {
    val green = SuperCartColors.primaryGreen
    Icon(Icons.Default.Store, contentDescription = null, tint = green, modifier = Modifier.size(72.dp))
    Text(text = stringResource(R.string.nav_stores), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = green)
    Spacer(modifier = Modifier.height(SuperCartSpacing.md))
}

@Composable
private fun LandingOptionCard(titleRes: Int, descRes: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SuperCartColors.white),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) { StoresScreenLandingOptionRow(titleRes, descRes) }
}

@Composable
private fun StoresScreenLandingOptionRow(titleRes: Int, descRes: Int) {
    val rowModifier = Modifier.fillMaxWidth().padding(horizontal = SuperCartSpacing.lg, vertical = SuperCartSpacing.xl)
    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        StoresScreenLandingOptionTexts(titleRes, descRes, Modifier.weight(1f))
        Icon(Icons.Default.ShoppingCart, null, tint = SuperCartColors.primaryGreen, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun StoresScreenLandingOptionTexts(titleRes: Int, descRes: Int, modifier: Modifier) {
    val titleColor = SuperCartColors.black
    Column(modifier = modifier) {
        Text(stringResource(titleRes), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = titleColor)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = stringResource(descRes), fontSize = 13.sp, color = SuperCartColors.gray)
    }
}
