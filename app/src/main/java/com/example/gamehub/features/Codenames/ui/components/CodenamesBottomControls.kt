package com.example.gamehub.features.codenames.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.gamehub.features.codenames.model.Clue

@Composable
fun CodenamesBottomControls(
    isMaster: Boolean,
    isMasterPhase: Boolean,
    currentTeam: String,
    masterTeam: String?,
    clueText: String,
    onClueTextChange: (String) -> Unit,
    onSubmitClue: () -> Unit,
    phaseStatus: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isMaster && isMasterPhase && currentTeam.equals(masterTeam, ignoreCase = true)) {
            ClueInput(
                clueText = clueText,
                onClueTextChange = onClueTextChange,
                onSubmit = onSubmitClue,
                enabled = true,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            phaseStatus()
        }
    }
} 