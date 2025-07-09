package com.example.gamehub.features.spy.model

data class SpyGameSettings(
    var numberOfPlayers: Int = 4,
    var numberOfSpies: Int = 1,
    var timerMinutes: Int = 5,
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