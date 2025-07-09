package com.example.gamehub.features.spy.model

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LocationManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "spy_game_prefs"
        private const val KEY_LOCATIONS = "locations"
        private val DEFAULT_LOCATIONS = listOf(
            com.example.gamehub.features.spy.model.Location("Beach", "A sunny beach with waves and sand"),
            com.example.gamehub.features.spy.model.Location("Restaurant", "A fancy restaurant with tables and chairs"),
            com.example.gamehub.features.spy.model.Location("School", "A school with classrooms and a playground"),
            com.example.gamehub.features.spy.model.Location("Hospital", "A hospital with doctors and patients"),
            com.example.gamehub.features.spy.model.Location("Airport", "An airport with planes and passengers"),
            com.example.gamehub.features.spy.model.Location("Supermarket", "A supermarket with shelves and shopping carts"),
            com.example.gamehub.features.spy.model.Location("Library", "A library with books and quiet spaces"),
            com.example.gamehub.features.spy.model.Location("Gym", "A gym with exercise equipment"),
            com.example.gamehub.features.spy.model.Location("Movie Theater", "A movie theater with screens and seats"),
            com.example.gamehub.features.spy.model.Location("Park", "A park with trees and benches")
        )
    }

    fun getLocations(): List<com.example.gamehub.features.spy.model.Location> {
        val json = sharedPreferences.getString(KEY_LOCATIONS, null)
        return if (json != null) {
            val type = object : TypeToken<List<com.example.gamehub.features.spy.model.Location>>() {}.type
            gson.fromJson(json, type)
        } else {
            DEFAULT_LOCATIONS
        }
    }

    fun saveLocations(locations: List<com.example.gamehub.features.spy.model.Location>) {
        val json = gson.toJson(locations)
        sharedPreferences.edit().putString(KEY_LOCATIONS, json).apply()
    }

    fun addLocation(location: com.example.gamehub.features.spy.model.Location) {
        val currentLocations = getLocations().toMutableList()
        currentLocations.add(location)
        saveLocations(currentLocations)
    }

    fun removeLocation(location: com.example.gamehub.features.spy.model.Location) {
        val currentLocations = getLocations().toMutableList()
        currentLocations.remove(location)
        saveLocations(currentLocations)
    }

    fun updateLocation(oldLocation: com.example.gamehub.features.spy.model.Location, newLocation: com.example.gamehub.features.spy.model.Location) {
        val currentLocations = getLocations().toMutableList()
        val index = currentLocations.indexOf(oldLocation)
        if (index != -1) {
            currentLocations[index] = newLocation
            saveLocations(currentLocations)
        }
    }
} 