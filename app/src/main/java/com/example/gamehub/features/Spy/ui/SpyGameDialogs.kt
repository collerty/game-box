package com.example.gamehub.features.spy.ui

import android.app.AlertDialog
import android.content.Context
import android.widget.NumberPicker
import android.widget.LinearLayout
import android.widget.CheckBox
import android.widget.ScrollView
import android.view.ViewGroup

object SpyGameDialogs {
    fun showNumberPickerDialog(
        context: Context,
        title: String,
        minValue: Int,
        maxValue: Int,
        currentValue: Int,
        onValueSelected: (Int) -> Unit
    ) {
        val numberPicker = NumberPicker(context).apply {
            this.minValue = minValue
            this.maxValue = maxValue
            this.value = currentValue
        }

        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(numberPicker)
            .setPositiveButton("OK") { _, _ ->
                onValueSelected(numberPicker.value)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun showLocationsDialog(
        context: Context,
        currentLocations: List<String>,
        onLocationsSelected: (List<String>) -> Unit
    ) {
        val scrollView = ScrollView(context)
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        SpyGameSettings.defaultLocations.forEach { location ->
            val checkBox = CheckBox(context).apply {
                text = location
                isChecked = currentLocations.contains(location)
            }
            container.addView(checkBox)
        }

        scrollView.addView(container)

        AlertDialog.Builder(context)
            .setTitle("Select Locations")
            .setView(scrollView)
            .setPositiveButton("OK") { _, _ ->
                val selectedLocations = mutableListOf<String>()
                for (i in 0 until container.childCount) {
                    val checkBox = container.getChildAt(i) as? CheckBox
                    if (checkBox?.isChecked == true) {
                        selectedLocations.add(checkBox.text.toString())
                    }
                }
                onLocationsSelected(selectedLocations)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
} 