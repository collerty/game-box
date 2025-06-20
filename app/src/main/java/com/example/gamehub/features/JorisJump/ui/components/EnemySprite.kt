package com.example.gamehub.features.JorisJump.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.gamehub.R
import com.example.gamehub.features.JorisJump.model.ENEMY_WIDTH_DP
import com.example.gamehub.features.JorisJump.model.ENEMY_HEIGHT_DP
import com.example.gamehub.features.JorisJump.model.EnemyState

@Composable
fun EnemySprite(
    enemy: EnemyState,
    totalScrollOffsetDp: Float,
    showHitbox: Boolean = false,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.saibaman_enemy),
        contentDescription = "Saibaman Enemy",
        modifier = modifier
            .absoluteOffset(
                x = (enemy.x + enemy.visualOffsetX).dp,
                y = ((enemy.y - totalScrollOffsetDp) + enemy.visualOffsetY).dp
            )
            .size(ENEMY_WIDTH_DP.dp, ENEMY_HEIGHT_DP.dp)
    )
    if (showHitbox) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .absoluteOffset(x = enemy.x.dp, y = (enemy.y - totalScrollOffsetDp).dp)
                .size(ENEMY_WIDTH_DP.dp, ENEMY_HEIGHT_DP.dp)
                .background(Color.Yellow.copy(alpha = 0.3f))
        )
    }
} 