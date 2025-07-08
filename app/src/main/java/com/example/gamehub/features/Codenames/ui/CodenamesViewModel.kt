package com.example.gamehub.features.codenames.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.gamehub.features.codenames.model.CodenamesGameState
import com.example.gamehub.repository.interfaces.ICodenamesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CodenamesViewModel(
    private val repository: ICodenamesRepository
) : ViewModel() {
    private val _gameState = MutableStateFlow<CodenamesGameState?>(null)
    val gameState: StateFlow<CodenamesGameState?> = _gameState

    fun startListening(roomId: String) {
        repository.listenToGameState(
            roomId,
            onDataChange = { state -> _gameState.value = state },
            onError = { /* handle error, e.g. log or show message */ }
        )
    }

    fun submitClue(roomId: String, newState: CodenamesGameState) {
        viewModelScope.launch {
            repository.updateGameState(
                roomId,
                newState,
                onSuccess = { /* handle success, e.g. show toast */ },
                onError = { /* handle error */ }
            )
        }
    }
}

class CodenamesViewModelFactory(
    private val repository: ICodenamesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CodenamesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CodenamesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 