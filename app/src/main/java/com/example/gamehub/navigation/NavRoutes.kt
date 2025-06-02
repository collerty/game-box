package com.example.gamehub.navigation

object NavRoutes {
    const val MAIN_MENU   = "mainMenu"
    const val GAMES_LIST  = "gamesList"
    const val SETTINGS    = "settings"
    const val TEST_SENSORS= "testSensors"

    const val ACCEL_TEST     = "accelerometerTest"
    const val GYRO_TEST      = "gyroscopeTest"
    const val PROXIMITY_TEST = "proximitytest"
    const val VIBRATION_TEST = "vibrationtest"
    const val MIC_TEST       = "mictest"
    const val CAMERA_TEST    = "cameratest"

    // lobby flow
    const val LOBBY_MENU      = "lobby/{gameId}"
    const val HOST_LOBBY      = "hostLobby/{gameId}/{code}"
    const val GUEST_GAME      = "guestGame/{gameId}/{code}/{userName}"

    // after “Start” pressed: per-game multiplayer join-in
    const val BATTLESHIPS_GAME = "battleships/{code}/{userName}"
    const val OHPARDON_GAME    = "ohpardon/{code}/{userName}"

    // single-player / local
    const val SPY_GAME         = "spy"
    const val JORISJUMP_GAME   = "jorisjump"
    const val SCREAMOSAUR_GAME = "screamosaur"

    //Battleships
    const val BATTLE_VOTE    = "battleships/{code}/{userName}/vote"
    const val BATTLE_PLACE = "battleships/{code}/{userName}/place/{mapId}"

    //TriviaToe
    const val TRIVIATOE_GAME = "triviatoe/{code}/{userName}"
    const val TRIVIATOE_XO_ASSIGN = "triviatoe/{code}/{userName}/xo"


}
