package com.example.gamehub.model

import com.example.gamehub.R

sealed class Game(
    val id: String,
    val title: String,
    val iconRes: Int,
    val online: Boolean            // ← NEW flag
) {
    object BattleShips  : Game("battleships", "BattleShips",  R.drawable.ic_battleships, online = true)
    object OhPardon     : Game("ohpardon",   "Oh Pardon",    R.drawable.ic_ohpardon,    online = true)
    object Codenames    : Game("codenames",  "Codenames",    R.drawable.ic_codenames,   online = true)

    /* single-player or local-only examples */
    object Spy          : Game("spy",        "Spy",          R.drawable.ic_spy,         online = false)
    object JorisJump    : Game("jorisjump",  "Joris Jump",   R.drawable.ic_jorisjump,   online = false)
    object ScreamoSaur  : Game("screamosaur","Scream-O-Saur",R.drawable.ic_screamosaur, online = false)

    companion object {
        val all = listOf(BattleShips, OhPardon, Codenames, Spy, JorisJump, ScreamoSaur)
    }
}
