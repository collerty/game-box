package com.example.gamehub.features.spy.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gamehub.R
import com.example.gamehub.features.spy.model.Location

class LocationsAdapter(
    private var locations: List<Location>,
    private val onEditClick: (Location) -> Unit,
    private val onDeleteClick: (Location) -> Unit
) : RecyclerView.Adapter<LocationsAdapter.LocationViewHolder>() {

    class LocationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.locationNameText)
        val descriptionText: TextView = view.findViewById(R.id.locationDescriptionText)
        val editButton: ImageButton = view.findViewById(R.id.editButton)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_location, parent, false)
        return LocationViewHolder(view)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        val location = locations[position]
        holder.nameText.text = location.name
        holder.descriptionText.text = location.description
        
        holder.editButton.setOnClickListener { onEditClick(location) }
        holder.deleteButton.setOnClickListener { onDeleteClick(location) }
    }

    override fun getItemCount() = locations.size

    fun updateLocations(newLocations: List<Location>) {
        locations = newLocations
        notifyDataSetChanged()
    }
} 