package com.pokemonarena.presentation.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pokemonarena.domain.entity.Card
import com.pokemonarena.domain.entity.Item
import com.pokemonarena.presentation.theme.AppColors
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun HeldItemControls(card: Card, availableItems: List<Pair<Item, Int>>,
                     onEquip: (String) -> Unit, onUnequip: () -> Unit) {
    val held = card.heldItem
    if (held != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KamelImage(asyncPainterResource(held.imageUrl), held.name,
                contentScale = ContentScale.Fit, modifier = Modifier.size(20.dp),
                onLoading = {}, onFailure = {})
            Spacer(Modifier.width(4.dp))
            Text(held.name, style = MaterialTheme.typography.labelSmall,
                 color = AppColors.successColor, fontWeight = FontWeight.Bold)
            IconButton(onClick = onUnequip, modifier = Modifier.size(22.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Quitar item",
                     modifier = Modifier.size(11.dp))
            }
        }
    } else if (availableItems.isNotEmpty()) {
        var menuOpen by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { menuOpen = true },
                       modifier = Modifier.height(28.dp),
                       contentPadding = PaddingValues(horizontal = 6.dp)) {
                Text("+ Equipar item", style = MaterialTheme.typography.labelSmall)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                availableItems.forEach { (item, qty) ->
                    DropdownMenuItem(
                        text = { Text("${item.name} (x$qty)", style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = {
                            KamelImage(asyncPainterResource(item.imageUrl), item.name,
                                contentScale = ContentScale.Fit, modifier = Modifier.size(22.dp),
                                onLoading = {}, onFailure = {})
                        },
                        onClick = { menuOpen = false; onEquip(item.id) }
                    )
                }
            }
        }
    }
}
