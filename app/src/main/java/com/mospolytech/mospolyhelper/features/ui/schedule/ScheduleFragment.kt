package com.mospolytech.mospolyhelper.features.ui.schedule

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager.widget.ViewPager
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import by.kirich1409.viewbindingdelegate.viewBinding
import com.mospolytech.mospolyhelper.R
import com.mospolytech.mospolyhelper.databinding.FragmentScheduleBinding
import com.mospolytech.mospolyhelper.domain.schedule.model.*
import com.mospolytech.mospolyhelper.domain.schedule.model.lesson.Lesson
import com.mospolytech.mospolyhelper.domain.schedule.model.lesson.LessonTime
import com.mospolytech.mospolyhelper.features.appwidget.schedule.ScheduleAppWidgetProvider
import com.mospolytech.mospolyhelper.features.ui.schedule.advanced_search.AdvancedSearchFragment
import com.mospolytech.mospolyhelper.features.ui.schedule.model.ScheduleDatesUiData
import com.mospolytech.mospolyhelper.features.ui.schedule.model.ScheduleUiData
import com.mospolytech.mospolyhelper.features.utils.getAttributeRes
import com.mospolytech.mospolyhelper.features.utils.setSmartCurrentItem
import com.mospolytech.mospolyhelper.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


class ScheduleFragment : Fragment(R.layout.fragment_schedule) {
    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("d MMMM")
    }

    private val viewModel  by sharedViewModel<ScheduleViewModel>()
    private val viewBinding by viewBinding(FragmentScheduleBinding::bind)

    private var appBarExpanded = true
    private var viewPagerIdle = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setAppBar()
        setScheduleViews()
        bindViewModel()
    }

    private fun setAppBar() {
        viewBinding.textviewUser.setOnClickListener {
            findNavController().safe {
                navigate(ScheduleFragmentDirections.actionScheduleFragmentToScheduleUsersFragment())
            }
        }

        viewBinding.btnMenu.setOnClickListener {
            openMenu(context)
        }
    }

    @SuppressLint("RestrictedApi")
    private fun openMenu(context: Context?) {
        val menuBuilder = MenuBuilder(context)
        val inflater = MenuInflater(context)
        inflater.inflate(R.menu.menu_schedule, menuBuilder)
        menuBuilder.forEach {
            val drawable = DrawableCompat.wrap(it.icon)
            DrawableCompat.setTint(drawable, ContextCompat.getColor(requireContext(), R.color.text_color_primary))
            it.icon = drawable
        }
        val optionsMenu = MenuPopupHelper(requireContext(), menuBuilder, viewBinding.btnMenu)
        optionsMenu.setForceShowIcon(true)

        menuBuilder.setCallback(object : MenuBuilder.Callback {
            override fun onMenuItemSelected(menu: MenuBuilder, item: MenuItem): Boolean {
                return onOptionsItemSelected(item)
            }

            override fun onMenuModeChange(menu: MenuBuilder) {}
        })
        optionsMenu.show()
    }

    private fun setScheduleViews() {
        with(viewBinding) {
            refreshSchedule.setProgressBackgroundColorSchemeResource(getAttributeRes(R.attr.colorSurfaceSecondary)!!)
            refreshSchedule.setColorSchemeResources(R.color.color_primary)
            refreshSchedule.setOnRefreshListener { viewModel.setRefreshing() }

            includeViewpager.viewpagerSchedule.apply {
                offscreenPageLimit = 2
                registerOnPageChangeCallback(TabLayoutOnPageChangeCallback())
                adapter = ScheduleAdapter().apply {
                    onLessonClick = { lessonTime: LessonTime, lesson: Lesson, date: LocalDate ->
                        findNavController().safe {
                            navigate(
                                ScheduleFragmentDirections
                                    .actionScheduleFragmentToLessonInfoFragment(
                                        lessonTime = lessonTime,
                                        lesson = lesson,
                                        date = date.toEpochDay()
                                    )
                            )
                        }
                    }
                }
            }

            buttonHome.setOnClickListener { viewModel.setTodayDate() }
            includeAddUser.buttonChooseSchedule.setOnClickListener {
                findNavController().safe {
                    navigate(ScheduleFragmentDirections.actionScheduleFragmentToScheduleUsersFragment())
                }
            }

            viewpagerWeeks.adapter = WeekAdapter()
            viewpagerWeeks.isUserInputEnabled = false
            viewpagerWeeks.offscreenPageLimit = 1
            viewpagerWeeks.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    viewModel.setWeek(position)
                }
            })
        }
    }

    private fun setLoading(isLoading: Boolean) {
        if (isLoading) {
            viewBinding.progressbarSchedule.show()
        } else {
            viewBinding.progressbarSchedule.gone()
        }
    }

    private fun setUserTitle(user: UserSchedule?) {
        viewBinding.textviewUser.text = when (user) {
            null -> getString(R.string.schedule_choose_user)
            is StudentSchedule ->
                getString(R.string.schedule_user_group, user.title)
            else -> user.title
        }
    }


    private fun setWeekViewPager(scheduleDatesUiData: ScheduleDatesUiData?) {
        (viewBinding.viewpagerWeeks.adapter as? WeekAdapter)
            ?.update(scheduleDatesUiData ?: emptyList(), viewModel.date.value)
    }

    private fun setSchedule(scheduleUiData: ScheduleUiData) {
        viewBinding.includeViewpager.root.show()
        viewBinding.includeAddUser.root.hide()
        viewBinding.includeNull.root.hide()
        (viewBinding.includeViewpager.viewpagerSchedule.adapter as ScheduleAdapter)
            .submitData(
                scheduleUiData,
                viewModel.currentLessonTimes.value.first
            )
    }

    private fun setNullSchedule() {
        viewBinding.includeViewpager.root.hide()
        viewBinding.includeAddUser.root.hide()
        viewBinding.includeNull.root.show()
    }

    private fun setAddUser() {
        viewBinding.includeViewpager.root.hide()
        viewBinding.includeAddUser.root.show()
        viewBinding.includeNull.root.hide()
    }

    private fun setCurrentLessonTimes(lessonTimes: List<LessonTime>) {
        (viewBinding.includeViewpager.viewpagerSchedule.adapter as? ScheduleAdapter)
            ?.setCurrentLessonTimes(lessonTimes)
    }

    private fun updateAppWidget() {
        val intent = Intent(context, ScheduleAppWidgetProvider::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        // Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
        // since it seems the onUpdate() is only fired on that:
        val ids = AppWidgetManager.getInstance(context)
            .getAppWidgetIds(
                ComponentName(
                    requireContext(),
                    ScheduleAppWidgetProvider::class.java
                )
            )
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context?.sendBroadcast(intent)
    }

    private fun bindViewModel() {
        lifecycleScope.launchWhenResumed {
            viewModel.scheduleUiData.collect {
                it.onSuccess {
                    setSchedule(it)
                }.onFailure {
                    if (viewModel.user.value == null) {
                        setAddUser()
                    } else {
                        setNullSchedule()
                    }
                }
            }
        }
        lifecycleScope.launchWhenResumed {
            viewModel.scheduleDatesUiData.collect {
                it.onSuccess {
                    setWeekViewPager(it)
                }.onFailure {
                    setWeekViewPager(null)
                }
            }
        }
        lifecycleScope.launchWhenResumed {
            viewModel.scheduleWeekPosition.collect {
                if (it != viewBinding.viewpagerWeeks.currentItem) {
                    viewBinding.viewpagerWeeks.setSmartCurrentItem(it)
                }
            }
        }
        lifecycleScope.launchWhenResumed {
            combine(viewModel.date, viewModel.scheduleWeekPosition) { date, week ->
                if (viewModel.scheduleDatesUiData.value.getOrNull()?.isNotEmpty() == true) {
                    viewBinding.textviewDateAndWeek.text =
                        getString(R.string.schedule_date, date.format(dateFormatter), week + 1)
                    viewBinding.textviewDateAndWeek.show()
                } else {
                    viewBinding.textviewDateAndWeek.hide()
                }

            }.collect()
        }
        lifecycleScope.launchWhenResumed {
            viewModel.schedulePosition.collect {
                if (it != viewBinding.includeViewpager.viewpagerSchedule.currentItem) {
                    viewBinding.includeViewpager.viewpagerSchedule.setSmartCurrentItem(it)
                }
            }
        }
        lifecycleScope.launchWhenResumed {
            viewModel.date.collect {
                if (LocalDate.now() == it) {
                    viewBinding.buttonHome.hide()
                } else {
                    viewBinding.buttonHome.show()
                }

                (viewBinding.viewpagerWeeks.adapter as? WeekAdapter)?.updateSelectedDay(it)
            }
        }
        lifecycleScope.launchWhenResumed {
            viewModel.currentLessonTimes.collect {
                setCurrentLessonTimes(it.first)
            }
        }
        lifecycleScope.launchWhenResumed {
            viewModel.isRefreshing.collect {
                viewBinding.refreshSchedule.isRefreshing = it
            }
        }
        lifecycleScope.launchWhenResumed {
            viewModel.user.collect {
                setUserTitle(it)
            }
        }
        lifecycleScope.launchWhenResumed {
            viewModel.isLoading.collect {
                if (!viewModel.isRefreshing.value) {
                    setLoading(it)
                }
            }
        }
        lifecycleScope.launch {
            viewModel.user.collect {
                if (it !is AdvancedSearchSchedule) {
                    updateAppWidget()
                }
            }
        }

        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<ScheduleFilters>(AdvancedSearchFragment.ADVANCED_SEARCH)
            ?.observe(viewLifecycleOwner) {
                viewModel.setAdvancedSearch(it)
            }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.schedule_advanced_search -> findNavController().safe {
                navigate(ScheduleFragmentDirections.actionScheduleFragmentToAdvancedSearchFragment())
            }
            R.id.menu_schedule_filter -> findNavController().safe {
                navigate(ScheduleFragmentDirections.actionScheduleFragmentToScheduleFiltersFragment())
            }
            R.id.menu_schedule_calendar -> findNavController().safe {
                navigate(ScheduleFragmentDirections.actionScheduleFragmentToCalendarFragment())
            }
            R.id.menu_schedule_user_choice -> findNavController().safe {
                navigate(ScheduleFragmentDirections.actionScheduleFragmentToScheduleUsersFragment())
            }
        }

        return true
    }

    private inner class TabLayoutOnPageChangeCallback : OnPageChangeCallback() {
        private var previousScrollState = 0
        private var scrollState = 0

        init {
            reset()
        }

        override fun onPageScrollStateChanged(state: Int) {
            previousScrollState = scrollState
            scrollState = state

            viewPagerIdle = state == ViewPager.SCROLL_STATE_IDLE
            viewBinding.refreshSchedule.isEnabled = appBarExpanded && viewPagerIdle
        }

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            // Only update the text selection if we're not settling, or we are settling after
            // being dragged
            val updateText = scrollState != ViewPager2.SCROLL_STATE_SETTLING
                    || previousScrollState == ViewPager2.SCROLL_STATE_DRAGGING

            val dayOffset = if (positionOffset < 0.5) 0L else 1L
            val date = viewModel.getDateByDay(position + dayOffset)
            if (updateText) {
                if (LocalDate.now() == date) {
                    viewBinding.buttonHome.hide()
                } else {
                    viewBinding.buttonHome.show()
                }
            }
        }

        override fun onPageSelected(position: Int) {
            viewModel.setDay(position)
        }

        fun reset() {
            scrollState = ViewPager2.SCROLL_STATE_IDLE
            previousScrollState = scrollState
        }
    }

}