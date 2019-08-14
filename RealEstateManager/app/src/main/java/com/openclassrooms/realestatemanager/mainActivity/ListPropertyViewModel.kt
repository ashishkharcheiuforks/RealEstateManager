package com.openclassrooms.realestatemanager.mainActivity

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.openclassrooms.realestatemanager.data.AgentRepository
import com.openclassrooms.realestatemanager.data.CurrencyRepository
import com.openclassrooms.realestatemanager.data.PropertyForListDisplay
import com.openclassrooms.realestatemanager.data.PropertyRepository
import com.openclassrooms.realestatemanager.data.entity.Property
import com.openclassrooms.realestatemanager.mviBase.BaseViewModel
import com.openclassrooms.realestatemanager.mviBase.Lce
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Created by galou on 2019-08-12
 */
class ListPropertyViewModel(
        private val propertyRepository: PropertyRepository,
        private val currencyRepository: CurrencyRepository)
    : BaseViewModel(){


    private val viewStateLD = MutableLiveData<PropertyListViewState>()
    val viewState: LiveData<PropertyListViewState>
        get() = viewStateLD
    private var currentViewState = PropertyListViewState()
        set(value) {
            field = value
            viewStateLD.value = value
        }

    private var searchPropertiesJob: Job? = null

    fun actionFromIntent(intent: PropertyListIntent){
        when(intent){
            is PropertyListIntent.DisplayPropertiesIntent -> fetchPropertiesFromDB()
        }

    }

    private fun resultToViewState(result: Lce<PropertyListResult>){
        currentViewState = when (result){
            is Lce.Content -> {
                when(result.packet){
                    is PropertyListResult.DisplayPropertiesResult -> {
                        currentViewState.copy(
                                isLoading = false,
                                listProperties = result.packet.properties
                        )
                    }
                }
            }

            is Lce.Loading -> {
                currentViewState.copy(
                        isLoading = true
                )
            }
            is Lce.Error -> {
                when(result.packet){
                    is PropertyListResult.DisplayPropertiesResult -> {
                        currentViewState.copy(
                                isLoading = false,
                                errorSource = ErrorSourceListProperty.NO_PROPERTY_IN_DB
                        )
                    }
                }
            }
        }
    }

    private fun fetchPropertiesFromDB() {
        resultToViewState(Lce.Loading())
        if(searchPropertiesJob?.isActive == true) searchPropertiesJob?.cancel()

        var neighborhood = ""
        var pictureUrl = ""
        val propertiesForDisplay = mutableListOf<PropertyForListDisplay>()

        fun emitResult(){
            val result: Lce<PropertyListResult> = if(propertiesForDisplay.isEmpty()){
                Lce.Error(PropertyListResult.DisplayPropertiesResult(null))
            } else{
                Lce.Content(PropertyListResult.DisplayPropertiesResult(propertiesForDisplay))
            }
            resultToViewState(result)
        }

        fun configurePropertyForDisplay(property: Property){
            val propertyToDisplay = PropertyForListDisplay(
                    property.id!!, property.type.typeName, neighborhood, property.price, pictureUrl)
            Log.e("propertyDisplay", propertyToDisplay.toString())
            propertiesForDisplay.add(propertyToDisplay)

            emitResult()
        }

        fun fetchNeighborhood(idProperty: Int, property: Property){
            launch {
                neighborhood = propertyRepository.getPropertyAddress(idProperty)[0].neighbourhood
                configurePropertyForDisplay(property)
            }
        }

        fun fetchPicture(idProperty: Int, property: Property){
            launch {
                val pictures = propertyRepository.getPropertyPicture(idProperty)
                if(pictures.isNotEmpty()) {
                    pictureUrl = pictures[0].url
                }

                fetchNeighborhood(idProperty, property)
            }
        }

        searchPropertiesJob = launch {
            val properties: List<Property>? = propertyRepository.getAllProperties()
            properties?.forEach {
                fetchPicture(it.id!!, it)
            }

        }
    }

}