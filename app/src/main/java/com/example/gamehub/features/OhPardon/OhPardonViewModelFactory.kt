package com.example.gamehub.features.ohpardon

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class OhPardonViewModelFactory(
    private val application: Application,
    private val roomCode: String,
    private val currentUserName: String
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OhPardonViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OhPardonViewModel(application, roomCode, currentUserName) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
