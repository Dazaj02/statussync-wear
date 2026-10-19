package com.statussync.wear.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

/** Una opción de [ChoiceScreen]. Sin `color` usa el fondo neutro del tema. */
data class Choice(
    val key: String,
    val label: String,
    val color: Color? = null,
    val onColor: Color = Color.White,
)

/** Pantalla genérica "elige una opción": la usan el selector de duración y el flujo de estados propios. */
@Composable
fun ChoiceScreen(
    title: String,
    choices: List<Choice>,
    onPick: (Choice) -> Unit,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    eyebrowColor: Color = Color.Unspecified,
    hint: String? = null,
) {
    val haptics = LocalHapticFeedback.current

    ScalingLazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (eyebrow != null) {
            item {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.titleMedium,
                    color = eyebrowColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (hint != null) {
            item {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        items(choices.size) { index ->
            val choice = choices[index]
            Button(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPick(choice)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = choice.color ?: MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = if (choice.color != null) choice.onColor else MaterialTheme.colorScheme.onSurface,
                ),
                label = { Text(choice.label, style = MaterialTheme.typography.titleMedium) },
            )
        }
    }
}
