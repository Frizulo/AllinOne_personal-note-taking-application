package com.example.allinone.ui.tasks


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allinone.data.local.entities.TaskEntity
import com.example.allinone.data.repo.TasksRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class TasksViewModel(private val repo: TasksRepository) : ViewModel() {


    fun addTask(title: String, detail: String, tag: String, quadrant: Int, progress: Int, dueTimeMillis: Long?) {
        viewModelScope.launch {
            repo.createLocalTask(
                title = title,
                detail = detail,
                tag = tag,
                quadrant = quadrant,
                progress = progress,
                dueTimeMillis = dueTimeMillis
            )
        }
    }

    fun updateTask(localId: String, title: String, detail: String, tag: String, quadrant: Int, progress: Int, dueTimeMillis: Long?) {
        viewModelScope.launch {
            repo.updateLocalTask(localId, title, detail, tag, quadrant, progress, dueTimeMillis)
        }
    }

    fun deleteTask(localId: String) {
        viewModelScope.launch { repo.markDelete(localId) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun syncOnce() {
        viewModelScope.launch {
            runCatching { repo.syncOnce() }
                .onFailure { e ->
                    e.printStackTrace() // 或 Log.e("SYNC", "syncOnce failed", e)
                }
        }
    }


    private val _query = MutableStateFlow("")

    // 搜尋
    val tasks: StateFlow<List<TaskEntity>> =
        _query
            .debounce(200)
            .distinctUntilChanged()
            .flatMapLatest { q -> repo.observeTasks(q) }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )
    fun setQuery(q: String) { _query.value = q; }



}

