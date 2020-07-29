package com.mospolytech.mospolyhelper.ui.schedule

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mospolytech.mospolyhelper.repository.deadline.DeadlinesRepository
import com.mospolytech.mospolyhelper.repository.schedule.*
import com.mospolytech.mospolyhelper.repository.schedule.models.Lesson
import com.mospolytech.mospolyhelper.repository.schedule.models.Schedule
import com.mospolytech.mospolyhelper.ui.common.Mediator
import com.mospolytech.mospolyhelper.ui.common.ViewModelBase
import com.mospolytech.mospolyhelper.ui.common.ViewModelMessage
import com.mospolytech.mospolyhelper.ui.schedule.calendar.CalendarViewModel
import com.mospolytech.mospolyhelper.ui.schedule.lesson_info.LessonInfoViewModel
import com.mospolytech.mospolyhelper.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.koin.android.ext.android.inject
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.time.LocalDate


class ScheduleViewModel(
    mediator: Mediator<String, ViewModelMessage>,
    private val scheduleRepository: ScheduleRepository,
    private val groupListRepository: GroupListRepository,
    val lessonLabelRepository: LessonLabelRepository,
    val deadlinesRepository: DeadlinesRepository,
    val schedule: MutableStateFlow<Schedule?>,
    val date: MutableStateFlow<LocalDate>,
    val isSession: MutableStateFlow<Boolean>,
    val groupTitle: MutableStateFlow<String>,
    val scheduleFilter: MutableStateFlow<Schedule.Filter>,
    val showEmptyLessons: MutableStateFlow<Boolean>
) : ViewModelBase(mediator, ScheduleViewModel::class.java.simpleName), KoinComponent {
    companion object {
        const val MessageChangeDate = "ChangeDate"
        const val MessageSetAdvancedSearchSchedule = "SetAdvancedSearchSchedule"
    }
    var isAdvancedSearch = false
    var groupList = MutableStateFlow(emptyList<String>())

    var isLoading = MutableStateFlow(true)
    var firstLoading = true

    val onMessage: Event1<String> = Action1()

    init {
        subscribe(::handleMessage)
        getGroupList(true)

        combine(this.isSession, this.groupTitle) { isSession, groupTitle ->
            setUpSchedule(isSession, groupTitle, !firstLoading)
            firstLoading = false
        }.launchIn(viewModelScope)
    }


    private fun handleMessage(message: ViewModelMessage) {
        when (message.key) {
            MessageChangeDate -> {
                date.value = message.content[0] as LocalDate
            }
            MessageSetAdvancedSearchSchedule -> {
                isLoading.value = true
                isAdvancedSearch = true
                schedule.value = message.content[0] as Schedule
                isLoading.value = false
            }
        }
    }

    fun updateSchedule() {
        setUpSchedule(
            isSession.value,
            groupTitle.value,
            true
        )
    }

    private fun setUpSchedule(isSession: Boolean, groupTitle: String, downloadNew: Boolean) {
        isLoading.value = true
        viewModelScope.async {
            val schedule = if (groupTitle.isEmpty()) {
                null
            } else {
                scheduleRepository.getSchedule(
                    groupTitle,
                    isSession,
                    downloadNew,
                    (onMessage as Action1)::invoke
                )
                    ?: scheduleRepository.getSchedule(
                        groupTitle,
                        isSession,
                        !downloadNew,
                        (onMessage as Action1)::invoke
                    )
            }
            withContext(Dispatchers.Main) {
                this@ScheduleViewModel.schedule.value = schedule
                isLoading.value = false
            }
        }
    }

    fun goHome() {
        date.value = LocalDate.now()
    }

    fun openCalendar() {
        send(
            CalendarViewModel::class.java.simpleName,
            CalendarViewModel.CalendarMode,
            schedule.value!!,
            scheduleFilter.value!!,
            date.value!!,
            isAdvancedSearch
        )
    }

    fun openLessonInfo(lesson: Lesson, date: LocalDate) {
        send(
            LessonInfoViewModel::class.java.simpleName,
            LessonInfoViewModel.LessonInfo,
            lesson,
            date
        )
    }

    private fun getGroupList(downloadNew: Boolean) {
        viewModelScope.async {
            val groupList = groupListRepository.getGroupList(downloadNew)
            withContext(Dispatchers.Main) {
                this@ScheduleViewModel.groupList.value = groupList
            }
        }
    }

    class Factory {
        companion object {
            fun create(
                mediator: Mediator<String, ViewModelMessage>,
                scheduleRepository: ScheduleRepository,
                groupListRepository: GroupListRepository,
                lessonLabelRepository: LessonLabelRepository,
                deadlinesRepository: DeadlinesRepository,
                preferences: SharedPreferences
            ): ScheduleViewModel {
                val dateFilter = Schedule.Filter.DateFilter.values()[
                        preferences.getInt(
                            PreferenceKeys.ScheduleDateFilter,
                            Schedule.Filter.default.dateFilter.ordinal
                        )
                ]
                val sessionFilter = preferences.getBoolean(
                    PreferenceKeys.ScheduleSessionFilter,
                    Schedule.Filter.default.sessionFilter
                )

                val scheduleFilter = Schedule.Filter.Builder(Schedule.Filter.default)
                    .dateFilter(dateFilter)
                    .sessionFilter(sessionFilter)
                    .build()

                val groupTitle = preferences.getString(
                    PreferenceKeys.ScheduleGroupTitle,
                    DefaultSettings.ScheduleGroupTitle
                )

                val isSession = try {
                    preferences.getBoolean(
                        PreferenceKeys.ScheduleTypePreference,
                        DefaultSettings.ScheduleTypePreference
                    )
                } catch (e: Exception) {
                    preferences.getInt(PreferenceKeys.ScheduleTypePreference, 0) == 1;
                }

                val showEmptyLessons = preferences.getBoolean(
                    PreferenceKeys.ScheduleShowEmptyLessons,
                    DefaultSettings.ScheduleShowEmptyLessons
                )
                return ScheduleViewModel(
                    mediator,
                    scheduleRepository,
                    groupListRepository,
                    lessonLabelRepository,
                    deadlinesRepository,
                    MutableStateFlow(null),
                    MutableStateFlow(LocalDate.now()),
                    MutableStateFlow(isSession),
                    MutableStateFlow(groupTitle ?: ""),
                    MutableStateFlow(scheduleFilter),
                    MutableStateFlow(showEmptyLessons)
                )
            }
        }
    }
}
