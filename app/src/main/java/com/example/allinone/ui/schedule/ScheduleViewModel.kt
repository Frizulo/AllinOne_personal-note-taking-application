package com.example.allinone.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allinone.data.local.entities.TaskEntity
import com.example.allinone.data.repo.TasksRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class ScheduleViewModel(
    private val repo: TasksRepository
) : ViewModel() {

    // 用當天做預設選擇
    private val _selectedDayMillis = MutableStateFlow(System.currentTimeMillis())
    val selectedDayMillis: StateFlow<Long> = _selectedDayMillis

    // 月份狀態（用年月來表示）
    private val _monthAnchorMillis = MutableStateFlow(System.currentTimeMillis())
    val monthAnchorMillis: StateFlow<Long> = _monthAnchorMillis

    val tasksOfSelectedDay: StateFlow<List<TaskEntity>> =
        _selectedDayMillis
            .flatMapLatest { day -> repo.observeTasksForDay(day) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectDay(millis: Long) {
        _selectedDayMillis.value = millis
    }

    fun gotoPrevMonth() {
        _monthAnchorMillis.value = shiftMonth(_monthAnchorMillis.value, -1)
    }

    fun gotoNextMonth() {
        _monthAnchorMillis.value = shiftMonth(_monthAnchorMillis.value, +1)
    }

    private fun shiftMonth(anchor: Long, delta: Int): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = anchor
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.add(java.util.Calendar.MONTH, delta)
        return cal.timeInMillis
    }
}
