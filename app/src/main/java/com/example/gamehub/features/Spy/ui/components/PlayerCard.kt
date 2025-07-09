package com.example.gamehub.features.spy.ui.components

import com.example.gamehub.ui.SpriteMenuButton
import com.example.gamehub.R
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.gamehub.features.spy.service.SpyGameService.PlayerCardInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.runtime.*

@Composable
fun PlayerCard(
    info: PlayerCardInfo,
    onRevealRole: () -> Unit,
    onAdvancePlayer: () -> Unit
) {
    var showRoleDialog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Player ${info.playerNumber}")
            Spacer(modifier = Modifier.height(24.dp))
            if (!info.isRoleRevealed) {
                SpriteMenuButton(text = "Tap to reveal role", onClick = {
                    onRevealRole()
                    showRoleDialog = true
                })
            } else {
                SpriteMenuButton(text = "Next Player", onClick = onAdvancePlayer)
            }
        }
    }
    if (info.isRoleRevealed && showRoleDialog) {
        AlertDialog(
            onDismissRequest = { showRoleDialog = false },
            title = { Text("Your Role") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                        if (info.role?.contains("Spy") == true) {
                            Image(painter = painterResource(id = R.drawable.spy_reveal), contentDescription = "Spy", modifier = Modifier.size(120.dp))
                            Text("You are the Spy!", modifier = Modifier.padding(top = 8.dp))
                        } else {
                            Image(painter = painterResource(id = R.drawable.civilian_reveal), contentDescription = "Civilian", modifier = Modifier.size(120.dp))
                            Text(info.role ?: "You are a Civilian", modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                SpriteMenuButton(text = "OK", onClick = { showRoleDialog = false }, minWidth = 80.dp)
            }
        )
    }
} 