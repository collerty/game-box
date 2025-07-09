package com.example.gamehub.features.spy.model

const val DEFAULT_NUMBER_OF_PLAYERS = 4
const val DEFAULT_NUMBER_OF_SPIES = 1
const val DEFAULT_TIMER_MINUTES = 5

data class SpyGameSettings(
    var numberOfPlayers: Int = DEFAULT_NUMBER_OF_PLAYERS,
    var numberOfSpies: Int = DEFAULT_NUMBER_OF_SPIES,
    var timerMinutes: Int = DEFAULT_TIMER_MINUTES,
    var selectedLocations: List<String> = defaultLocations
) {
    companion object {
        val defaultLocations = listOf(
            "Beach",
            "Airport",
            "Restaurant",
            "Hospital",
            "School",
            "Movie Theater",
            "Shopping Mall",
            "Park",
            "Office",
            "Hotel",
            "Library",
            "Gym",
            "Zoo",
            "Museum",
            "Train Station",
            "Bank",
            "Supermarket",
            "Stadium",
            "Theater",
            "Cruise Ship"
        )
    }
} 