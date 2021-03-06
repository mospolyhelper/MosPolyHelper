package com.mospolytech.mospolyhelper.features.ui.schedule.advanced_search

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.ObservableList
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mospolytech.mospolyhelper.R
import com.mospolytech.mospolyhelper.databinding.BottomSheetScheduleAdvancedSearchBinding
import com.mospolytech.mospolyhelper.domain.schedule.model.teacher.Teacher
import com.mospolytech.mospolyhelper.features.utils.RoundedBackgroundSpan
import com.mospolytech.mospolyhelper.utils.gone
import com.mospolytech.mospolyhelper.utils.hide
import com.mospolytech.mospolyhelper.utils.safe
import com.mospolytech.mospolyhelper.utils.show
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.coroutines.CoroutineContext

class AdvancedSearchFragment : BottomSheetDialogFragment(), CoroutineScope {

    companion object {
        const val ADVANCED_SEARCH = "ADVANCED_SEARCH"
    }

    private val job = SupervisorJob()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    private var downloadSchedulesJob = SupervisorJob()

    private val viewModel by sharedViewModel<AdvancedSearchViewModel>()
    private val viewBinding by viewBinding(BottomSheetScheduleAdvancedSearchBinding::bind)

    override fun getTheme(): Int  = R.style.CustomBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_schedule_advanced_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setBottomSheet()
        bindViewModel()
    }

    private fun setBottomSheet() {

        viewBinding.buttonCancel.setOnClickListener {
            downloadSchedulesJob.cancel()
            viewBinding.buttonCancel.visibility = View.GONE
        }


        viewBinding.buttonDownload.setOnClickListener {
            downloadSchedulesJob = SupervisorJob()
            launch(Dispatchers.Main + downloadSchedulesJob) {
                viewBinding.buttonDownload.isEnabled = false
                setFiltersVisibility(View.GONE)
                setProgressVisibility(View.VISIBLE)
                try {
                    viewModel.getAdvancedSearchData {
                        lifecycleScope.launchWhenResumed {
                            synchronized(viewBinding.textviewProgress) {
                                viewBinding.progressBar.progress = (it * 10000).toInt()
                                viewBinding.textviewProgress.text = "${(it * 100).toInt()} %"
                            }
                        }
                    }
                    Toast.makeText(context, "Расписания загружены", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Загрузка отменена", Toast.LENGTH_SHORT).show()
                    setFiltersVisibility(View.GONE)
                } finally {
                    viewBinding.buttonDownload.isEnabled = true
                    setProgressVisibility(View.GONE)
                    viewBinding.progressBar.progress = 0
                    viewBinding.textviewProgress.text = "0 %"
                }
            }
        }

        viewBinding.textviewLessonTypes.setOnClickListener {
            val dialog = AdvancedSearchSelectFragment.newInstance()
            dialog.show(requireActivity().supportFragmentManager, "qq")
            dialog.setAdapter(
                AdvancedSearchAdapter(
                    SimpleFilter(viewModel.lessonTypes.value, viewModel.checkedLessonTypes)
                )
            )
        }
        viewBinding.textviewLessonTitles.setOnClickListener {
            val dialog = AdvancedSearchSelectFragment.newInstance()
            dialog.show(requireActivity().supportFragmentManager, "qq")
            dialog.setAdapter(
                AdvancedSearchAdapter(
                    AdvancedFilter(viewModel.lessonTitles.value, viewModel.checkedLessonTitles)
                )
            )
        }
        viewBinding.textviewTeachers.setOnClickListener {
            val dialog = AdvancedSearchSelectFragment.newInstance()
            dialog.show(requireActivity().supportFragmentManager, "qq")
            dialog.setAdapter(
                AdvancedSearchAdapter(
                    AdvancedFilter(viewModel.lessonTeachers.value, viewModel.checkedTeachers)
                )
            )
        }
        viewBinding.textviewGroups.setOnClickListener {
            val dialog = AdvancedSearchSelectFragment.newInstance()
            dialog.show(requireActivity().supportFragmentManager, "qq")
            dialog.setAdapter(
                AdvancedSearchAdapter(
                    SimpleFilter(viewModel.lessonGroups.value, viewModel.checkedGroups)
                )
            )
        }
        viewBinding.textviewAuditoriums.setOnClickListener {
            val dialog = AdvancedSearchSelectFragment.newInstance()
            dialog.show(requireActivity().supportFragmentManager, "qq")
            dialog.setAdapter(
                AdvancedSearchAdapter(
                    AdvancedFilter(viewModel.lessonAuditoriums.value, viewModel.checkedAuditoriums)
                )
            )
        }

        viewBinding.buttonApply.setOnClickListener {
            findNavController().safe {
                previousBackStackEntry?.savedStateHandle?.set(ADVANCED_SEARCH, viewModel.getScheduleFilters())
                navigateUp()
            }
        }

        setLessonTitles()
        setLessonTypes()
        setTeachers()
        setGroups()
        setAuditoriums()
        if (
            viewModel.checkedLessonTitles.isNotEmpty() ||
            viewModel.checkedLessonTypes.isNotEmpty() ||
            viewModel.checkedTeachers.isNotEmpty() ||
            viewModel.checkedGroups.isNotEmpty() ||
            viewModel.checkedAuditoriums.isNotEmpty()
        ) {
            setFiltersVisibility(View.VISIBLE)
        } else {
            setFiltersVisibility(View.GONE)
        }
    }

    private fun bindViewModel() {
        viewModel.checkedLessonTitles.addOnListChangedCallback(ListChangedObserver {
            lifecycleScope.launchWhenResumed {
                setLessonTitles()
            }
        })

        viewModel.checkedLessonTypes.addOnListChangedCallback(ListChangedObserver {
            lifecycleScope.launchWhenResumed {
                setLessonTypes()
            }
        })

        viewModel.checkedTeachers.addOnListChangedCallback(ListChangedObserver {
            lifecycleScope.launchWhenResumed {
                setTeachers()
            }
        })

        viewModel.checkedGroups.addOnListChangedCallback(ListChangedObserver {
            lifecycleScope.launchWhenResumed {
                setGroups()
            }
        })

        viewModel.checkedAuditoriums.addOnListChangedCallback(ListChangedObserver {
            lifecycleScope.launchWhenResumed {
                setAuditoriums()
            }
        })


        lifecycleScope.launchWhenResumed {
            viewModel.scheduleVersion.collect {
                val days = it?.downloadingDateTime?.until(ZonedDateTime.now(), ChronoUnit.DAYS)

                viewBinding.textviewScheduleStatus.text = when {
                    days == null -> getString(R.string.schedule_advanced_search_not_downloaded)
                    days == 0L -> getString(R.string.schedule_advanced_search_updated_recently)
                    days < 31L -> resources.getQuantityString(
                        R.plurals.schedule_advanced_search_updated, days.toInt(), days.toInt()
                    )
                    else -> getString(R.string.schedule_advanced_search_update_long_ago)
                }

                if (days != null) {
                    viewBinding.buttonDownload.isEnabled = false
                    viewBinding.progressbarSchedule.show()
                    viewBinding.buttonDownload.text = getString(R.string.update_schedules)
                    viewModel.getAdvancedSearchDataLocal()
                } else {
                    viewBinding.buttonDownload.isEnabled = true
                    viewBinding.progressbarSchedule.hide()
                    viewBinding.buttonDownload.text = getString(R.string.download_schedules)
                }
            }
        }

        lifecycleScope.launchWhenResumed {
            viewModel.schedulePackList.collect {
                if (it != null) {
                    viewBinding.buttonDownload.isEnabled = true
                    viewBinding.progressbarSchedule.gone()
                    setFiltersVisibility(View.VISIBLE)
                }
            }
        }
    }

    private fun setLessonTitles() {
        if (viewModel.checkedLessonTitles.isEmpty()) {
            viewBinding.textviewLessonTitles.text = getFeaturesString(listOf(getString(R.string.all_subjects)))
        } else {
            viewBinding.textviewLessonTitles.text =
                getFeaturesString(viewModel.checkedLessonTitles.map { viewModel.lessonTitles.value[it] })
        }
    }

    private fun setLessonTypes() {
        if (viewModel.checkedLessonTypes.isEmpty()) {
            viewBinding.textviewLessonTypes.text = getFeaturesString(listOf(getString(R.string.all_lesson_types)))
        } else {
            viewBinding.textviewLessonTypes.text =
                getFeaturesString(viewModel.checkedLessonTypes.map { viewModel.lessonTypes.value[it] })
        }
    }

    private fun setTeachers() {
        if (viewModel.checkedTeachers.isEmpty()) {
            viewBinding.textviewTeachers.text = getFeaturesString(listOf(getString(R.string.all_teachers)))
        } else {
            viewBinding.textviewTeachers.text =
                getFeaturesString(viewModel.checkedTeachers.map { Teacher(viewModel.lessonTeachers.value[it]).getShortName() })
        }
    }

    private fun setGroups() {
        if (viewModel.checkedGroups.isEmpty()) {
            viewBinding.textviewGroups.text = getFeaturesString(listOf(getString(R.string.all_groups)))
        } else {
            viewBinding.textviewGroups.text =
                getFeaturesString(viewModel.checkedGroups.map { viewModel.lessonGroups.value[it] })
        }
    }

    private fun setAuditoriums() {
        if (viewModel.checkedAuditoriums.isEmpty()) {
            viewBinding.textviewAuditoriums.text = getFeaturesString(listOf(getString(R.string.all_auditoriums)))
        } else {
            viewBinding.textviewAuditoriums.text =
                getFeaturesString(viewModel.checkedAuditoriums.map { viewModel.lessonAuditoriums.value[it] })
        }
    }

    private fun getFeaturesString(features: Iterable<String>): SpannableStringBuilder {
        val iterator = features.iterator()
        val builder = SpannableStringBuilder()
        while (iterator.hasNext()) {
            val feature = iterator.next()
            builder.append(
                "\u00A0",
                RoundedBackgroundSpan(
                    backgroundColor = requireContext().getColor(R.color.featureBackground),
                    textColor = requireContext().getColor(R.color.featureText),
                    text = feature
                ),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            if (iterator.hasNext()) {
                builder.append(" ")
            } else {
                builder.append(" ")
            }
        }
        return builder
    }

    private fun setFiltersVisibility(visibility: Int) {
        viewBinding.textviewLessonTitles.visibility = visibility
        viewBinding.textviewLessonTitlesLabel.visibility = visibility
        viewBinding.textviewLessonTypes.visibility = visibility
        viewBinding.textviewLessonTypesLabel.visibility = visibility
        viewBinding.textviewTeachers.visibility = visibility
        viewBinding.textviewTeachersLabel.visibility = visibility
        viewBinding.textviewGroups.visibility = visibility
        viewBinding.textviewGroupsLabel.visibility = visibility
        viewBinding.textviewAuditoriums.visibility = visibility
        viewBinding.textviewAuditoriumsLabel.visibility = visibility
        viewBinding.buttonApply.visibility = visibility
    }

    private fun setProgressVisibility(visibility: Int) {
        viewBinding.progressBar.visibility = visibility
        viewBinding.textviewProgress.visibility = visibility
        viewBinding.buttonCancel.visibility = visibility
    }

    class ListChangedObserver(private val block: (ObservableList<*>?) -> Unit) : ObservableList.OnListChangedCallback<ObservableList<*>>() {
        override fun onChanged(sender: ObservableList<*>?) = block(sender)

        override fun onItemRangeRemoved(
            sender: ObservableList<*>?,
            positionStart: Int,
            itemCount: Int
        ) = block(sender)

        override fun onItemRangeMoved(
            sender: ObservableList<*>?,
            fromPosition: Int,
            toPosition: Int,
            itemCount: Int
        ) = block(sender)

        override fun onItemRangeInserted(
            sender: ObservableList<*>?,
            positionStart: Int,
            itemCount: Int
        ) = block(sender)

        override fun onItemRangeChanged(
            sender: ObservableList<*>?,
            positionStart: Int,
            itemCount: Int
        ) = block(sender)
    }
}