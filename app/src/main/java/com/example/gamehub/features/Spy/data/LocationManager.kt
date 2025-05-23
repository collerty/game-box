package com.example.gamehub.features.spy.data

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
            Location("Beach", "A sunny beach with waves and sand"),
            Location("Restaurant", "A fancy restaurant with tables and chairs"),
            Location("School", "A school with classrooms and a playground"),
            Location("Hospital", "A hospital with doctors and patients"),
            Location("Airport", "An airport with planes and passengers"),
            Location("Supermarket", "A supermarket with shelves and shopping carts"),
            Location("Library", "A library with books and quiet spaces"),
            Location("Gym", "A gym with exercise equipment"),
            Location("Movie Theater", "A movie theater with screens and seats"),
            Location("Park", "A park with trees and benches")
        )
    }

    fun getLocations(): List<Location> {
        val json = sharedPreferences.getString(KEY_LOCATIONS, null)
        return if (json != null) {
            val type = object : TypeToken<List<Location>>() {}.type
            gson.fromJson(json, type)
        } else {
            DEFAULT_LOCATIONS
        }
    }

    fun saveLocations(locations: List<Location>) {
        val json = gson.toJson(locations)
        sharedPreferences.edit().putString(KEY_LOCATIONS, json).apply()
    }

    fun addLocation(location: Location) {
        val currentLocations = getLocations().toMutableList()
        currentLocations.add(location)
        saveLocations(currentLocations)
    }

    fun removeLocation(location: Location) {
        val currentLocations = getLocations().toMutableList()
        currentLocations.remove(location)
        saveLocations(currentLocations)
    }

    fun updateLocation(oldLocation: Location, newLocation: Location) {
        val currentLocations = getLocations().toMutableList()
        val index = currentLocations.indexOf(oldLocation)
        if (index != -1) {
            currentLocations[index] = newLocation
            saveLocations(currentLocations)
        }
    }
} 