package com.example.gamehub.features.battleships.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.gamehub.features.battleships.model.Cell

/**
 * Sealed definitions of your power-ups.
 * `.expand(origin)` returns all target cells.
 */
sealed class PowerUp(val cost: Int) {
    abstract fun expand(origin: Cell): List<Cell>
    object Mine    : PowerUp(2) { override fun expand(o: Cell) = listOf(o) }
    object Bomb2x2 : PowerUp(3) {
        override fun expand(o: Cell) = listOf(
            o,
            Cell(o.row,     o.col + 1),
            Cell(o.row + 1, o.col),
            Cell(o.row + 1, o.col + 1)
        )
    }
    object Laser   : PowerUp(4) { // <<== set to 4!
        override fun expand(o: Cell) = (0 until 10).map { c -> Cell(o.row, c) }
    }
}

/**
 * Displays your energy pool and buttons for each power-up.
 */
@Composable
fun PowerUpPanel(
    energy: Int,
    hasPlacedMineThisTurn: Boolean,
    onSelect: (PowerUp) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(8.dp)) {
        Text("Energy(ℇ): $energy", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        listOf(PowerUp.Mine, PowerUp.Bomb2x2, PowerUp.Laser).forEach { pu ->
            val isMine = pu == PowerUp.Mine
            val enabled = if (isMine) {
                energy >= pu.cost && !hasPlacedMineThisTurn
            } else {
                energy >= pu.cost
            }
            Button(
                onClick = { if (enabled) onSelect(pu) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor      = if (enabled) Color(0xFF000000) else Color(0xBB000000), // enabled: dark grey, disabled: black + more opacity
                    contentColor        = Color.White, // always white text
                    disabledContainerColor = Color(0xBB141313), // black, more transparent (override default!)
                    disabledContentColor   = Color.White,       // force text to stay white when disabled
                )
            ) {
                Text(
                    when {
                        isMine && hasPlacedMineThisTurn -> "You placed a mine!"
                        isMine -> "Mine (2ℇ)"
                        pu == PowerUp.Bomb2x2 -> "Bomb 2×2 (3ℇ)"
                        pu == PowerUp.Laser   -> "Laser (4ℇ)"
                        else -> ""
                    }
                )
            }
        }
    }
}
