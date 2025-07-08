package com.example.gamehub.features.codenames.ui.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun DebugLogger(vararg values: Any?) {
    LaunchedEffect(values) {
        Log.d("CodenamesDebug", values.joinToString { it.toString() })
    }
} 