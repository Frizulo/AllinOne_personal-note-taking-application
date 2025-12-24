package com.example.allinone.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allinone.data.repo.HomeWeather
import com.example.allinone.data.repo.TasksRepository
import com.example.allinone.data.repo.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val repo: TasksRepository,
    private val weatherRepo: WeatherRepository
) : ViewModel() {

    private val _selectedCity = MutableStateFlow(TaiwanCounties.first { it.name == "臺東縣" })
    val todayWeather: StateFlow<HomeWeather?> =
        _selectedCity
            .flatMapLatest { city ->
                flow {
                    emit(null) // 讓 UI 顯示載入中
                    emit(weatherRepo.fetchTodayWeatherByCity(city.lat, city.lon, city.name))
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setCity(city: County) {
        _selectedCity.value = city
    }
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
