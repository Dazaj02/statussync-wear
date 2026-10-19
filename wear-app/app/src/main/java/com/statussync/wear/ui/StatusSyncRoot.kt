package com.statussync.wear.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import com.statussync.wear.ui.assistant.AssistantScreen
import com.statussync.wear.ui.assistant.AssistantViewModel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.statussync.wear.R
import com.statussync.wear.domain.AvailabilityState
import com.statussync.wear.domain.CustomState
import com.statussync.wear.ui.common.Choice
import com.statussync.wear.ui.common.ChoiceScreen
import com.statussync.wear.ui.group.GroupScreen
import com.statussync.wear.ui.group.GroupViewModel
import com.statussync.wear.ui.home.DurationPickerScreen
import com.statussync.wear.ui.home.HomeEvent
import com.statussync.wear.ui.home.HomeScreen
import com.statussync.wear.ui.home.StatusViewModel
import com.statussync.wear.ui.theme.color
import com.statussync.wear.ui.theme.onColorFor
import java.time.Duration

/** Pantallas de la app. Sin librería de navegación: el flujo es corto y lineal. */
private sealed interface Route {
    data object Home : Route
    /** `baseLabelRes` para los 4 estados base; `customName` para los propios. */
    data class PickDuration(
        val baseLabelRes: Int?,
        val customName: String?,
        val color: Color,
        val optionsMin: List<Int>,
        val onStart: (Int) -> Unit,
    ) : Route
    data object AddName : Route
    data class AddBase(val name: String) : Route
    data class AddColor(val name: String, val base: AvailabilityState) : Route
    data class AddDuration(val name: String, val base: AvailabilityState, val colorArgb: Long) : Route
    data object Manage : Route
    data object Group : Route
    data object Assistant : Route
    data class ConfirmRemove(val state: CustomState) : Route
}

private fun Route.parent(): Route = when (this) {
    Route.Home -> Route.Home
    is Route.PickDuration, Route.AddName, Route.Manage, Route.Group, Route.Assistant -> Route.Home
    is Route.AddBase -> Route.AddName
    is Route.AddColor -> Route.AddBase(name)
    is Route.AddDuration -> Route.AddColor(name, base)
    is Route.ConfirmRemove -> Route.Manage
}

private const val EVALUATE_EVERY_MILLIS = 30_000L
private const val UNDO_WINDOW_MILLIS = 8_000L

private val PALETTE = listOf(
    0xFF3B82F6 to R.string.color_blue,
    0xFF14B8A6 to R.string.color_teal,
    0xFFEC4899 to R.string.color_pink,
    0xFFF97316 to R.string.color_orange,
    0xFF84CC16 to R.string.color_lime,
    0xFF94A3B8 to R.string.color_gray,
)

private val DURATION_SETS = listOf(listOf(15, 30, 60), listOf(25, 50, 90), listOf(45, 60, 90))

@Composable
fun StatusSyncRoot(
    viewModel: StatusViewModel,
    groupViewModel: GroupViewModel,
    assistantViewModel: AssistantViewModel,
    onTimerFinished: () -> Unit,
) {
    val ui = viewModel.uiState.collectAsStateWithLifecycle().value
    val assistant = assistantViewModel.uiState.collectAsStateWithLifecycle().value
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Permisos que el asistente pide solo al encender cada función.
    var afterGrant by remember { mutableStateOf<(() -> Unit)?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) afterGrant?.invoke()
        afterGrant = null
    }
    fun enableWithPermission(permission: String, enable: () -> Unit) {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            enable()
        } else {
            afterGrant = enable
            permissionLauncher.launch(permission)
        }
    }

    // El asistente solo revisa mientras la app está visible; al salir suelta los sensores.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            try {
                while (true) {
                    assistantViewModel.evaluate()
                    delay(EVALUATE_EVERY_MILLIS)
                }
            } finally {
                assistantViewModel.pause()
            }
        }
    }
    LaunchedEffect(assistant.undo) {
        if (assistant.undo != null) {
            delay(UNDO_WINDOW_MILLIS)
            assistantViewModel.clearUndo()
        }
    }
    val customStates = viewModel.customStates.collectAsStateWithLifecycle().value
    var route by remember { mutableStateOf<Route>(Route.Home) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { if (it == HomeEvent.TimerFinished) onTimerFinished() }
    }
    BackHandler(enabled = route != Route.Home) { route = route.parent() }

    fun goHome() { route = Route.Home }

    when (val current = route) {
        Route.Home -> HomeScreen(
            ui = ui,
            customStates = customStates,
            onFree = { viewModel.select(AvailabilityState.Free) },
            onPickTimer = { state ->
                route = Route.PickDuration(
                    baseLabelRes = state.labelRes,
                    customName = null,
                    color = state.color(),
                    optionsMin = state.durationOptionsMin,
                    onStart = { min -> viewModel.select(state, Duration.ofMinutes(min.toLong())); goHome() },
                )
            },
            onPickCustom = { custom ->
                if (custom.hasTimer) {
                    route = Route.PickDuration(
                        baseLabelRes = null,
                        customName = custom.name,
                        color = Color(custom.colorArgb),
                        optionsMin = custom.durationsMin,
                        onStart = { min -> viewModel.selectCustom(custom, Duration.ofMinutes(min.toLong())); goHome() },
                    )
                } else {
                    viewModel.selectCustom(custom)
                }
            },
            onAdd = { route = Route.AddName },
            onRemove = { route = Route.Manage },
            onGroup = { route = Route.Group },
            assistant = assistant,
            onAccept = assistantViewModel::accept,
            onDismissSuggestion = assistantViewModel::dismiss,
            onUndo = assistantViewModel::undoAuto,
            onAssistant = { route = Route.Assistant },
        )

        Route.Assistant -> AssistantScreen(
            ui = assistant,
            onCalendar = { on ->
                if (on) enableWithPermission(Manifest.permission.READ_CALENDAR) { assistantViewModel.setCalendar(true) }
                else assistantViewModel.setCalendar(false)
            },
            onMotion = { on ->
                if (on) enableWithPermission(Manifest.permission.ACTIVITY_RECOGNITION) { assistantViewModel.setMotion(true) }
                else assistantViewModel.setMotion(false)
            },
            onAuto = assistantViewModel::setAutoApply,
            onSimulateClass = assistantViewModel::simulateClass,
            onSimulateStillness = assistantViewModel::simulateStillness,
        )

        Route.Group -> GroupScreen(groupViewModel.uiState.collectAsStateWithLifecycle().value)

        is Route.PickDuration -> DurationPickerScreen(
            stateName = current.customName ?: current.baseLabelRes?.let { stringResource(it) }.orEmpty(),
            color = current.color,
            optionsMin = current.optionsMin,
            onPicked = current.onStart,
        )

        Route.AddName -> ChoiceScreen(
            title = stringResource(R.string.add_name_title),
            choices = stringArrayResource(R.array.suggested_names).map { Choice(it, it) },
            onPick = { route = Route.AddBase(it.key) },
        )

        is Route.AddBase -> ChoiceScreen(
            eyebrow = current.name,
            title = stringResource(R.string.add_base_title),
            hint = stringResource(R.string.add_base_hint),
            choices = AvailabilityState.entries.map {
                Choice(it.wireName, stringResource(it.labelRes), it.color(), onColorFor(it.color()))
            },
            onPick = { route = Route.AddColor(current.name, AvailabilityState.fromWire(it.key) ?: AvailabilityState.Free) },
        )

        is Route.AddColor -> ChoiceScreen(
            eyebrow = current.name,
            title = stringResource(R.string.add_color_title),
            choices = PALETTE.map { (argb, nameRes) ->
                Choice(argb.toString(), stringResource(nameRes), Color(argb), onColorFor(Color(argb)))
            },
            onPick = { picked ->
                val argb = picked.key.toLong()
                if (current.base.hasTimer) {
                    route = Route.AddDuration(current.name, current.base, argb)
                } else {
                    viewModel.addCustomState(current.name, current.base, argb, emptyList())
                    goHome()
                }
            },
        )

        is Route.AddDuration -> ChoiceScreen(
            eyebrow = current.name,
            eyebrowColor = Color(current.colorArgb),
            title = stringResource(R.string.add_duration_title),
            choices = DURATION_SETS.mapIndexed { index, set ->
                Choice(index.toString(), set.joinToString(" · ") + " min")
            },
            onPick = { picked ->
                viewModel.addCustomState(current.name, current.base, current.colorArgb, DURATION_SETS[picked.key.toInt()])
                goHome()
            },
        )

        Route.Manage -> ChoiceScreen(
            title = stringResource(R.string.remove_title),
            choices = customStates.map { Choice(it.id, it.name, Color(it.colorArgb), onColorFor(Color(it.colorArgb))) },
            onPick = { picked -> customStates.firstOrNull { it.id == picked.key }?.let { route = Route.ConfirmRemove(it) } },
        )

        is Route.ConfirmRemove -> ChoiceScreen(
            title = stringResource(R.string.remove_confirm_title, current.state.name),
            choices = listOf(
                Choice("yes", stringResource(R.string.remove_confirm_yes), Color(0xFFD64545), Color.White),
                Choice("no", stringResource(R.string.remove_confirm_no)),
            ),
            onPick = { picked ->
                if (picked.key == "yes") viewModel.removeCustomState(current.state.id)
                goHome()
            },
        )
    }
}
