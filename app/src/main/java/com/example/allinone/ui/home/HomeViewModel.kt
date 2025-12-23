package com.example.allinone.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allinone.data.repo.TasksRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val repo: TasksRepository
) : ViewModel() {

    val activeTaskCount: StateFlow<Int> =
        flow { emitAll(repo.observeActiveTaskCount()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val todayTodoCount: StateFlow<Int> =
        flow { emitAll(repo.observeTodayTodoCount()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val todayTodoTotalCount: StateFlow<Int> =
        flow { emitAll(repo.observeTodayTodoTotalCount()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
