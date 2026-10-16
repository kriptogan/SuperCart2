package com.example.supercart2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.supercart2.R
import com.example.supercart2.models.Grocery
import com.example.supercart2.ui.components.common.primaryButtonColors
import com.example.supercart2.ui.theme.SuperCartColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Lists the dates [grocery] was bought, newest first. */
@Composable
internal fun HierarchicalCategoryDisplayBuyHistoryDialog(grocery: Grocery, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { BuyHistoryTitle(grocery.name) },
        text = { if (grocery.buyEvents.isEmpty()) NoBuyHistory() else BuyDates(grocery.buyEvents) },
        confirmButton = { CloseButton(onClose) },
    )
}

@Composable
private fun BuyHistoryTitle(groceryName: String) {
    Text(
        text = stringResource(R.string.buy_history_title, groceryName), style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NoBuyHistory() {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Info, null, tint = SuperCartColors.gray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        NoBuyHistoryText()
    }
}

@Composable
private fun NoBuyHistoryText() {
    Text(
        text = stringResource(R.string.no_purchase_history), style = MaterialTheme.typography.bodyMedium,
        color = SuperCartColors.gray, textAlign = TextAlign.Center,
    )
}

@Composable
private fun BuyDates(buyEvents: List<LocalDate>) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        buyEvents.sortedDescending().forEach { date ->
            Text(text = formatBuyDate(date), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun formatBuyDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))

@Composable
private fun CloseButton(onClose: () -> Unit) {
    Button(onClick = onClose, colors = primaryButtonColors(), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.close))
    }
}
