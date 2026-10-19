package com.statussync.wear.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.statussync.wear.R
import com.statussync.wear.ui.common.Choice
import com.statussync.wear.ui.common.ChoiceScreen
import com.statussync.wear.ui.format.formatMinutes
import com.statussync.wear.ui.theme.onColorFor

/** Elegir cuánto dura el bloque. Al elegir, el estado cambia y arranca la cuenta regresiva. */
@Composable
fun DurationPickerScreen(
    stateName: String,
    color: Color,
    optionsMin: List<Int>,
    onPicked: (minutes: Int) -> Unit,
) {
    ChoiceScreen(
        eyebrow = stateName,
        eyebrowColor = color,
        title = stringResource(R.string.picker_title),
        choices = optionsMin.map { Choice(it.toString(), formatMinutes(it), color, onColorFor(color)) },
        onPick = { onPicked(it.key.toInt()) },
        hint = stringResource(R.string.picker_hint_cancel),
    )
}
