package com.example.allinone.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allinone.data.local.ScheduleSlotWithTask
import com.example.allinone.data.local.entities.ScheduleSlotEntity
import com.example.allinone.data.local.entities.TaskEntity
import com.example.allinone.data.repo.SaveSlotResult
import com.example.allinone.data.repo.ScheduleRepository
import com.example.allinone.data.repo.TasksRepository
import com.example.allinone.data.store.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModel(
    private val tasksRepo: TasksRepository,
    private val scheduleRepo: ScheduleRepository,
    private val tokenStore: TokenStore
) : ViewModel() {

    // -------------------------
    // 月曆 + 當日任務
    // -------------------------

    /** 選擇的日期（允許帶時間，但查詢時會 normalize 到 00:00） */
    private val _selectedDayMillis = MutableStateFlow(System.currentTimeMillis())
    val selectedDayMillis: StateFlow<Long> = _selectedDayMillis.asStateFlow()

    /** 月份錨點（用來建月曆） */
    private val _monthAnchorMillis = MutableStateFlow(System.currentTimeMillis())
    val monthAnchorMillis: StateFlow<Long> = _monthAnchorMillis.asStateFlow()

    /** 當天任務（用 dueTimeMillis 落在當天範圍內） */
    val tasksOfSelectedDay: StateFlow<List<TaskEntity>> =
        _selectedDayMillis
            .flatMapLatest { day -> tasksRepo.observeTasksForDay(day) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectDay(millis: Long) {
        _selectedDayMillis.value = millis
    }

    fun setMonthAnchor(millis: Long) {
        _monthAnchorMillis.value = millis
    }

    fun gotoPrevMonth() {
        _monthAnchorMillis.value = shiftMonth(_monthAnchorMillis.value, -1)
    }

    fun gotoNextMonth() {
        _monthAnchorMillis.value = shiftMonth(_monthAnchorMillis.value, +1)
    }

    private fun shiftMonth(anchor: Long, delta: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = anchor
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.add(Calendar.MONTH, delta)
        return cal.timeInMillis
    }

    // -------------------------
    // Timeline / Slot
    // -------------------------

    enum class Mode { Tasks, Timeline }
    private val _mode = MutableStateFlow(Mode.Tasks)
    val mode: StateFlow<Mode> = _mode.asStateFlow()
    fun setMode(m: Mode) { _mode.value = m }

    /** uid（多帳號隔離） */
    private val uidFlow: Flow<Long> =
        tokenStore.uidFlow
            .filterNotNull()
            .distinctUntilChanged()

    /** 當天 slots（Timeline View 使用） */
    val slotsWithTask: StateFlow<List<ScheduleSlotWithTask>> =
        combine(uidFlow, _selectedDayMillis) { uid, day ->
            uid to normalizeToStartOfDay(day)
        }
            .flatMapLatest { (uid, date0) ->
                scheduleRepo.observeSlotsWithTask(uid, date0)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** ✅ 4x3 統計（睡/早/中/晚）×（Total/Task/Free） */
    val stats4x3: StateFlow<ScheduleRepository.ScheduleStats4x3> =
        combine(_selectedDayMillis, slotsWithTask) { day, slots ->
            day to slots
        }
            .mapLatest { (day, slots) ->
                withContext(Dispatchers.Default) {
                    scheduleRepo.calculate4x3Stats(day, slots)
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                ScheduleRepository.ScheduleStats4x3()
            )

    // Snackbar / Toast 訊息
    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message.asSharedFlow()

    // Dialog 狀態（新增/編輯/衝突）
    sealed class SlotDialogState {
        data object Hidden : SlotDialogState()
        data class Editing(val isNew: Boolean, val draft: SlotDraft) : SlotDialogState()
        data class Conflict(val draft: SlotDraft, val conflict: ScheduleSlotEntity) : SlotDialogState()
    }

    data class SlotDraft(
        val slotId: Long = 0,
        val serverSlotId: Long? = null,
        val ownerUid: Long,
        val dateMillis: Long,
        val startTimeMillis: Long,
        val endTimeMillis: Long,
        val localTaskId: String? = null,
        val customTitle: String = "",
        val note: String = ""
    ) {
        fun toEntity(): ScheduleSlotEntity = ScheduleSlotEntity(
            slotId = slotId,
            serverSlotId = serverSlotId,
            ownerUid = ownerUid,
            dateMillis = dateMillis,
            startTimeMillis = startTimeMillis,
            endTimeMillis = endTimeMillis,
            localTaskId = localTaskId,
            customTitle = customTitle.trim().ifBlank { null },
            note = note.trim().ifBlank { null }
        )
    }

    private val _slotDialog = MutableStateFlow<SlotDialogState>(SlotDialogState.Hidden)
    val slotDialog: StateFlow<SlotDialogState> = _slotDialog.asStateFlow()

    fun closeSlotDialog() {
        _slotDialog.value = SlotDialogState.Hidden
    }

    fun updateDraft(transform: (SlotDraft) -> SlotDraft) {
        when (val cur = _slotDialog.value) {
            is SlotDialogState.Editing -> _slotDialog.value = cur.copy(draft = transform(cur.draft))
            is SlotDialogState.Conflict -> _slotDialog.value = cur.copy(draft = transform(cur.draft))
            SlotDialogState.Hidden -> Unit
        }
    }

    fun openCreateFreeSlotDialog() {
        viewModelScope.launch {
            val uid = uidFlow.first()
            val date0 = normalizeToStartOfDay(_selectedDayMillis.value)

            // 預設 08:00-09:00
            val start = date0 + 8L * HOUR_MS
            val end = start + 60L * MIN_MS

            _slotDialog.value = SlotDialogState.Editing(
                isNew = true,
                draft = SlotDraft(
                    ownerUid = uid,
                    dateMillis = date0,
                    startTimeMillis = start,
                    endTimeMillis = end,
                    localTaskId = null,
                    customTitle = "",
                    note = ""
                )
            )
        }
    }

    fun openCreateTaskSlotDialog(taskLocalId: String, taskTitleHint: String? = null) {
        viewModelScope.launch {
            val uid = uidFlow.first()
            val date0 = normalizeToStartOfDay(_selectedDayMillis.value)

            val gap = scheduleRepo.findFirstFreeGapOneHour(uid, date0)
            val start = gap?.first ?: (date0 + 8L * HOUR_MS)
            val end = gap?.second ?: (start + 60L * MIN_MS)

            _slotDialog.value = SlotDialogState.Editing(
                isNew = true,
                draft = SlotDraft(
                    ownerUid = uid,
                    dateMillis = date0,
                    startTimeMillis = start,
                    endTimeMillis = end,
                    localTaskId = taskLocalId,
                    customTitle = taskTitleHint ?: "",
                    note = ""
                )
            )
        }
    }

    fun openEditSlotDialog(slot: ScheduleSlotEntity, taskTitle: String?) {
        _slotDialog.value = SlotDialogState.Editing(
            isNew = false,
            draft = SlotDraft(
                slotId = slot.slotId,
                serverSlotId = slot.serverSlotId,
                ownerUid = slot.ownerUid,
                dateMillis = slot.dateMillis,
                startTimeMillis = slot.startTimeMillis,
                endTimeMillis = slot.endTimeMillis,
                localTaskId = slot.localTaskId,
                customTitle = slot.customTitle ?: (taskTitle ?: ""),
                note = slot.note ?: ""
            )
        )
    }

    fun saveDraft() {
        val cur = _slotDialog.value
        val draft = when (cur) {
            is SlotDialogState.Editing -> cur.draft
            is SlotDialogState.Conflict -> cur.draft
            SlotDialogState.Hidden -> return
        }

        viewModelScope.launch {
            when (val res = scheduleRepo.saveSlot(draft.toEntity())) {
                is SaveSlotResult.Success -> {
                    _message.tryEmit("已儲存排程")
                    _slotDialog.value = SlotDialogState.Hidden
                }
                is SaveSlotResult.Error -> {
                    _message.tryEmit(res.message)
                }
                is SaveSlotResult.Conflict -> {
                    // ✅ 讓你可以回去編輯：保留 draft，顯示衝突資訊
                    _slotDialog.value = SlotDialogState.Conflict(draft, res.conflictSlot)
                }
            }
        }
    }

    fun deleteSlot(slotId: Long) {
        viewModelScope.launch {
            val uid = uidFlow.first()
            scheduleRepo.softDeleteSlot(uid, slotId)
            _message.tryEmit("已刪除時段")
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

    companion object {
        private const val MIN_MS = 60_000L
        private const val HOUR_MS = 3_600_000L
    }
}
