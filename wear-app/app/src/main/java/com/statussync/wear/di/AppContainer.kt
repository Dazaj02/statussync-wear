package com.statussync.wear.di

import android.content.Context
import com.statussync.wear.data.InMemoryStatusRepository
import com.statussync.wear.data.CalendarScheduleSource
import com.statussync.wear.data.CompositeMotionSource
import com.statussync.wear.data.CompositeScheduleSource
import com.statussync.wear.data.MockGroupRepository
import com.statussync.wear.data.SharedPrefsAssistantSettingsRepository
import com.statussync.wear.data.SimulatedInputs
import com.statussync.wear.data.StepCounterMotionSource
import com.statussync.wear.domain.AssistantSettingsRepository
import com.statussync.wear.domain.MotionSource
import com.statussync.wear.domain.ScheduleSource
import com.statussync.wear.data.SharedPrefsCustomStateRepository
import com.statussync.wear.domain.CustomStateRepository
import com.statussync.wear.domain.GroupRepository
import com.statussync.wear.domain.StatusRepository
import java.time.Clock

/** Inyección manual. En W6 se cambia el adaptador mock por el real sin tocar la UI. */
class AppContainer(context: Context) {
    val clock: Clock = Clock.systemUTC()
    val statusRepository: StatusRepository = InMemoryStatusRepository(clock)
    val groupRepository: GroupRepository = MockGroupRepository(clock)
    val simulatedInputs = SimulatedInputs(clock)
    val scheduleSource: ScheduleSource = CompositeScheduleSource(listOf(CalendarScheduleSource(context), simulatedInputs))
    val motionSource: MotionSource = CompositeMotionSource(listOf(StepCounterMotionSource(context), simulatedInputs))
    val assistantSettingsRepository: AssistantSettingsRepository =
        SharedPrefsAssistantSettingsRepository(context.getSharedPreferences("statussync", Context.MODE_PRIVATE))
    val customStateRepository: CustomStateRepository =
        SharedPrefsCustomStateRepository(context.getSharedPreferences("statussync", Context.MODE_PRIVATE))
}
