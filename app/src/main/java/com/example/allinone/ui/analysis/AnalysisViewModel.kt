package com.example.allinone.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allinone.data.local.ScheduleSlotWithTask
import com.example.allinone.data.repo.ScheduleRepository
import com.example.allinone.data.store.TokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class AnalysisViewModel(
    private val scheduleRepo: ScheduleRepository,
    private val tokenStore: TokenStore
) : ViewModel() {

    // -------------------------
    // Filters
    // -------------------------
    private val _startDayMillis = MutableStateFlow<Long?>(null) // dateMillis 00:00
    val startDayMillis: StateFlow<Long?> = _startDayMillis.asStateFlow()

    private val _endDayMillis = MutableStateFlow<Long?>(null) // dateMillis 00:00
    val endDayMillis: StateFlow<Long?> = _endDayMillis.asStateFlow()

    private val _keyword = MutableStateFlow("")
    val keyword: StateFlow<String> = _keyword.asStateFlow()

    private val _includeTask = MutableStateFlow(true)
    val includeTask: StateFlow<Boolean> = _includeTask.asStateFlow()

    private val _includeFree = MutableStateFlow(true)
    val includeFree: StateFlow<Boolean> = _includeFree.asStateFlow()

    fun setStartDay(millis: Long?) { _startDayMillis.value = millis?.let { normalizeToStartOfDay(it) } }
    fun setEndDay(millis: Long?) { _endDayMillis.value = millis?.let { normalizeToStartOfDay(it) } }
    fun setKeyword(v: String) { _keyword.value = v }
    fun setIncludeTask(v: Boolean) { _includeTask.value = v }
    fun setIncludeFree(v: Boolean) { _includeFree.value = v }

    fun clearFilters() {
        _startDayMillis.value = null
        _endDayMillis.value = null
        _keyword.value = ""
        _includeTask.value = true
        _includeFree.value = true
    }

    // -------------------------
    // Results
    // -------------------------
    data class UiState(
        val loading: Boolean = false,
        val error: String? = null,
        val items: List<ScheduleSlotWithTask> = emptyList(),
        val summary: ScheduleRepository.AnalysisSummary = ScheduleRepository.AnalysisSummary(
            totalMs = 0,
            taskMs = 0,
            freeMs = 0,
            stats4x3 = ScheduleRepository.ScheduleStats4x3()
        )
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * ✅ 只要「全部不填」就會查全部：start/end/keyword 都是 null/blank 時直接全量查詢。
     */
    fun runSearch() {
        viewModelScope.launch {
            val uid = tokenStore.peekUserUid()
            if (uid == null) {
                _uiState.value = _uiState.value.copy(error = "尚未登入，無法查詢")
                return@launch
            }

            var start = _startDayMillis.value
            var end = _endDayMillis.value
            val k = _keyword.value.trim()

            // 使用者若選反了，自動交換，避免查不到
            if (start != null && end != null && start > end) {
                val tmp = start
                start = end
                end = tmp
                _startDayMillis.value = start
                _endDayMillis.value = end
            }

            _uiState.value = _uiState.value.copy(loading = true, error = null)

            runCatching {
                scheduleRepo.querySlotsForAnalysis(
                    uid = uid,
                    startDate = start,
                    endDate = end,
                    keyword = k.ifBlank { null },
                    includeTask = _includeTask.value,
                    includeFree = _includeFree.value
                )
            }.onSuccess { list ->
                val summary = scheduleRepo.summarizeForAnalysis(list)
                _uiState.value = UiState(loading = false, items = list, summary = summary)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "查詢失敗"
                )
            }
        }
    }

    // -------------------------
    // Helpers
    // -------------------------
    private fun normalizeToStartOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
