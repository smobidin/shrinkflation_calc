package com.team.itexperts.shrinkflation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ComparisonSelectionViewModel @Inject constructor() : ViewModel() {
    private val _pending = MutableStateFlow<List<CompInput>>(emptyList())
    val pending: StateFlow<List<CompInput>> = _pending.asStateFlow()

    fun submit(items: List<CompInput>) {
        _pending.value = items
    }

    fun consume() {
        _pending.value = emptyList()
    }
}
