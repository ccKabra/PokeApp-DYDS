package com.pokemonarena.presentation.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pokemonarena.domain.entity.PlayerGender
import com.pokemonarena.presentation.theme.AppColors
import com.pokemonarena.presentation.utils.spriteUrl
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

private const val MAX_NAME_LENGTH = 20

@Composable
fun ProfileSetupScreen(onConfirm: (String, PlayerGender) -> Unit) {
    var name   by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf<PlayerGender?>(null) }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(elevation = CardDefaults.cardElevation(6.dp), shape = RoundedCornerShape(20.dp)) {
            Column(
                Modifier.width(420.dp).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Filled.CatchingPokemon, contentDescription = null,
                     tint = AppColors.goldColor, modifier = Modifier.size(48.dp))
                Text("¡Bienvenido a PokeApp!", style = MaterialTheme.typography.headlineSmall,
                     fontWeight = FontWeight.ExtraBold)
                Text("Contanos quién sos para empezar tu aventura.",
                     style = MaterialTheme.typography.bodySmall,
                     color = AppColors.textSecondary, textAlign = TextAlign.Center)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(MAX_NAME_LENGTH) },
                    label = { Text("Tu nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    PlayerGender.entries.forEach { option ->
                        GenderOption(option, selected = gender == option) { gender = option }
                    }
                }

                Button(
                    onClick = { gender?.let { onConfirm(name.trim(), it) } },
                    enabled = name.isNotBlank() && gender != null,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("¡Comenzar aventura!", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun GenderOption(gender: PlayerGender, selected: Boolean, onSelect: () -> Unit) {
    Card(
        border = if (selected) BorderStroke(2.dp, AppColors.goldColor) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.clickable(onClick = onSelect)
    ) {
        Column(
            Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KamelImage(asyncPainterResource(gender.spriteUrl), gender.displayName,
                contentScale = ContentScale.Fit, modifier = Modifier.size(80.dp),
                onLoading = { CircularProgressIndicator(Modifier.size(22.dp)) },
                onFailure = {
                    Icon(Icons.Filled.Person, contentDescription = gender.displayName,
                         tint = AppColors.textSecondary, modifier = Modifier.size(48.dp))
                })
            Text(gender.displayName,
                 fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal)
        }
    }
}
