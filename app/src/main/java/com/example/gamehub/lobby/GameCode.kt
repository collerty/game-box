package com.example.gamehub.lobby

interface GameCodec<MOVE, STATE> {
    fun encodeMove(move: MOVE): Map<String, Any>
    fun decodeState(snapshot: Map<String, Any?>): STATE
}
