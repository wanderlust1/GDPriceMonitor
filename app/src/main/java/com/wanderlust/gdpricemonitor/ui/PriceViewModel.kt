package com.wanderlust.gdpricemonitor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wanderlust.gdpricemonitor.data.GoldPriceRepository
import com.wanderlust.gdpricemonitor.data.model.Quote
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(
    val quotes: List<Quote> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastUpdated: Long = 0
)

class PriceViewModel(
    private val repository: GoldPriceRepository = GoldPriceRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var isRefreshing = false

    /** 立即拉取最新行情；自动轮询与手动刷新共用，isRefreshing 防止重入竞态。 */
    fun refresh() {
        if (isRefreshing) return
        isRefreshing = true
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val quotes = repository.getQuotes()
                _uiState.value = _uiState.value.copy(
                    quotes = quotes,
                    isLoading = false,
                    lastUpdated = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "加载失败"
                )
            } finally {
                isRefreshing = false
            }
        }
    }

    private var pollingJob: Job? = null

    /** 前台可见时每 intervalMs 轮询一次；重复调用会取消上一次轮询，避免叠加。 */
    fun startPolling(intervalMs: Long = 10_000) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                refresh()
                delay(intervalMs)
            }
        }
    }
}
