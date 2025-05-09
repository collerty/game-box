package com.example.gamehub.model

import com.example.gamehub.R

sealed class Game(val id: String, val title: String, val iconRes: Int) {
    object BattleShips : Game("battleships", "BattleShips", R.drawable.ic_battleships)
    object Spy      : Game("spy",      "Spy",        R.drawable.ic_spy)
    object JorisJump      : Game("jorisjump",      "Joris Jump",        R.drawable.ic_jorisjump)
    object ScreamoSaur : Game("screamosaur", "Scream-O-Saur", R.drawable.ic_screamosaur)
    object OhPardon: Game("ohpardon", "OhPardon", R.drawable.ic_ohpardon)
    // → add more games here (with matching drawables)

    companion object {
        val all = listOf(BattleShips,Spy,JorisJump,ScreamoSaur,OhPardon)
    }
}
