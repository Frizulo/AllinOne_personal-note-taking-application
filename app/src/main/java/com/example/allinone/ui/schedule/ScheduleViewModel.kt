package com.example.allinone.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allinone.data.local.ScheduleSlotWithTask
import com.example.allinone.data.local.entities.ScheduleSlotEntity
import com.example.allinone.data.local.entities.TaskEntity
import com.example.allinone.data.repo.SaveSlotResult
import com.example.allinone.data.repo.ScheduleRepository
import com.example.allinone.data.repo.ScheduleStats3x3
import com.example.allinone.data.repo.TasksRepository
import com.example.allinone.data.store.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ScheduleViewModel(
    private val tasksRepo: TasksRepository,
    private val scheduleRepo: ScheduleRepository,
    tokenStore: TokenStore
) : ViewModel() {

    // -------------------------
    // Calendar state
    // -------------------------

    // ✅ 一開始就用「今天 00:00」避免狀態亂跳
    private val _selectedDayMillis = MutableStateFlow(startOfDay(System.currentTimeMillis()))
    val selectedDayMillis: StateFlow<Long> = _selectedDayMillis.asStateFlow()

    private val _monthAnchorMillis = MutableStateFlow(startOfMonth(System.currentTimeMillis()))
    val monthAnchorMillis: StateFlow<Long> = _monthAnchorMillis.asStateFlow()

    fun selectDay(millis: Long) {
        _selectedDayMillis.value = startOfDay(millis)
    }

    fun setMonthAnchor(millis: Long) {
        _monthAnchorMillis.value = startOfMonth(millis)
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
    // uid state（避免每次 first()）
    // -------------------------

    // ✅ uid 變成 StateFlow，隨時可取，不要 first()
    private val uidState: StateFlow<Long?> =
        tokenStore.uidFlow
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun requireUidOrNull(): Long? = uidState.value

    // -------------------------
    // Tasks of day
    // -------------------------

    val tasksOfSelectedDay: StateFlow<List<TaskEntity>> =
        combine(uidState, _selectedDayMillis) { uid, day0 ->
            uid to day0
        }
            .flatMapLatest { (uid, day0) ->
                if (uid == null) flowOf(emptyList())
                else tasksRepo.observeTasksForDay(day0) // repo 內部已 peekUid/或 DAO 帶 uid（你目前版本OK）
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // -------------------------
    // Mode
    // -------------------------

    enum class Mode { Tasks, Timeline }
    private val _mode = MutableStateFlow(Mode.Tasks)
    val mode: StateFlow<Mode> = _mode.asStateFlow()
    fun setMode(m: Mode) { _mode.value = m }

    // -------------------------
    // Slots + Stats
    // -------------------------

    val slotsWithTask: StateFlow<List<ScheduleSlotWithTask>> =
        combine(uidState, _selectedDayMillis) { uid, day0 ->
            uid to day0 // day0 已是 00:00
        }
            .flatMapLatest { (uid, date0) ->
                if (uid == null) flowOf(emptyList())
                else scheduleRepo.observeSlotsWithTask(uid, date0)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ✅ 重點：統計計算丟到 Default，避免卡 Main 導致 ANR
    val stats3x3: StateFlow<ScheduleStats3x3> =
        slotsWithTask
            .mapLatest { slots ->
                withContext(Dispatchers.Default) {
                    scheduleRepo.calculate3x3Stats(_selectedDayMillis.value, slots)
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleStats3x3())

    // -------------------------
    // UI message
    // -------------------------

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message.asSharedFlow()

    // -------------------------
    // Dialog state
    // -------------------------

    sealed class SlotDialogState {
        data object Hidden : SlotDialogState()
        data class Editing(val isNew: Boolean, val draft: SlotDraft) : SlotDialogState()
        data class Conflict(val draft: SlotDraft, val conflict: ScheduleSlotEntity) : SlotDialogState()
    }

    data class SlotDraft(
        val slotId: Long = 0,
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
        val cur = _slotDialog.value
        _slotDialog.value = when (cur) {
            is SlotDialogState.Editing -> cur.copy(draft = transform(cur.draft))
            is SlotDialogState.Conflict -> cur.copy(draft = transform(cur.draft))
            SlotDialogState.Hidden -> cur
        }
    }

    fun openCreateFreeSlotDialog() {
        val uid = requireUidOrNull() ?: run {
            _message.tryEmit("尚未登入")
            return
        }
        val date0 = _selectedDayMillis.value
        val start = date0 + 8L * HOUR_MS
        val end = start + 60L * MIN_MS

        _slotDialog.value = SlotDialogState.Editing(
            isNew = true,
            draft = SlotDraft(
                ownerUid = uid,
                dateMillis = date0,
                startTimeMillis = start,
                endTimeMillis = end
            )
        )
    }

    fun openCreateTaskSlotDialog(taskLocalId: String, taskTitleHint: String? = null) {
        val uid = requireUidOrNull() ?: run {
            _message.tryEmit("尚未登入")
            return
        }
        val date0 = _selectedDayMillis.value

        viewModelScope.launch {
            // ✅ 可能會掃當天 slots，丟到 IO/Default 比較安全
            val gap = withContext(Dispatchers.Default) {
                scheduleRepo.findFirstFreeGapOneHour(uid, date0)
            }
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
                    customTitle = taskTitleHint ?: ""
                )
            )
        }
    }

    fun openEditSlotDialog(slot: ScheduleSlotEntity, taskTitle: String?) {
        _slotDialog.value = SlotDialogState.Editing(
            isNew = false,
            draft = SlotDraft(
                slotId = slot.slotId,
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
            val res = withContext(Dispatchers.IO) {
                scheduleRepo.saveSlot(draft.toEntity())
            }
            when (res) {
                is SaveSlotResult.Success -> {
                    _message.tryEmit("已儲存排程")
                    _slotDialog.value = SlotDialogState.Hidden
                }
                is SaveSlotResult.Error -> _message.tryEmit(res.message)
                is SaveSlotResult.Conflict -> _slotDialog.value =
                    SlotDialogState.Conflict(draft, res.conflictSlot)
            }
        }
    }

    fun deleteSlot(slotId: Long) {
        val uid = requireUidOrNull() ?: run {
            _message.tryEmit("尚未登入")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            scheduleRepo.softDeleteSlot(uid, slotId)
            _message.tryEmit("已刪除時段")
        }
    }

    // -------------------------
    // Helpers
    // -------------------------

    companion object {
        private const val MIN_MS = 60_000L
        private const val HOUR_MS = 3_600_000L
    }
}

private fun startOfDay(millis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

private fun startOfMonth(millis: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}
