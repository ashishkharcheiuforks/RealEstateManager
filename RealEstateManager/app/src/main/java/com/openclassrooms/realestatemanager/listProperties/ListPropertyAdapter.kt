package com.openclassrooms.realestatemanager.listProperties

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.openclassrooms.realestatemanager.R
import com.openclassrooms.realestatemanager.data.entity.PropertyWithAllData
import com.openclassrooms.realestatemanager.utils.Currency

/**
 * Created by galou on 2019-08-12
 */

class ListPropertyAdapter(var properties: List<PropertyWithAllData>,
                          var currency: Currency,
                          val glide: RequestManager)
    : RecyclerView.Adapter<ListPropertyViewHolder>(){

    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListPropertyViewHolder {
        context = parent.context
        val layoutInflater = LayoutInflater.from(context)
        val view = layoutInflater.inflate(R.layout.list_view_item, parent, false)

        return ListPropertyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return properties.size
    }

    override fun onBindViewHolder(holder: ListPropertyViewHolder, position: Int) {
        holder.updateWithProperty(properties[position], glide, currency, context)
    }

    fun getProperty(position: Int): PropertyWithAllData{
        return properties[position]
    }

    fun updateCurrency(newCurrency: Currency){
        currency = newCurrency
        notifyDataSetChanged()
    }
}