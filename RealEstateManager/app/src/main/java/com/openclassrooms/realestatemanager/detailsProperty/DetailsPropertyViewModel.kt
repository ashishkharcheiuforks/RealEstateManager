package com.openclassrooms.realestatemanager.detailsProperty

import androidx.lifecycle.LiveData
import com.openclassrooms.realestatemanager.data.entity.PropertyWithAllData
import com.openclassrooms.realestatemanager.data.repository.CurrencyRepository
import com.openclassrooms.realestatemanager.data.repository.PropertyRepository
import com.openclassrooms.realestatemanager.mviBase.BaseViewModel
import com.openclassrooms.realestatemanager.mviBase.Lce
import com.openclassrooms.realestatemanager.mviBase.REMViewModel
import com.openclassrooms.realestatemanager.utils.Currency
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Created by galou on 2019-08-25
 */

class DetailsPropertyViewModel(
        private val propertyRepository: PropertyRepository,
        private val currencyRepository: CurrencyRepository
) : BaseViewModel<DetailsPropertyViewState>(),
        REMViewModel<DetailsPropertyIntent, DetailsPropertyResult>{

    private var currentViewState = DetailsPropertyViewState()
        set(value) {
            field = value
            viewStateLD.value = value
        }

    val currency: LiveData<Currency>
        get() = currencyRepository.currency

    private var searchPropertyJob: Job? = null

    override fun actionFromIntent(intent: DetailsPropertyIntent) {
        when(intent){
            is DetailsPropertyIntent.FetchDetailsIntent -> fetchDetailsProperty()
            is DetailsPropertyIntent.DisplayDetailsIntent -> displayPropertyDetails()
        }
    }

    override fun resultToViewState(result: Lce<DetailsPropertyResult>) {
        currentViewState = when (result){
            is Lce.Content -> {
                when(result.packet){
                    is DetailsPropertyResult.FetchDetailsResult -> {
                        currentViewState.copy(
                                isLoading = false,
                                property = result.packet.property,
                                address = result.packet.address,
                                amenities = result.packet.amenities,
                                pictures = result.packet.pictures
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
                currentViewState.copy(
                        isLoading = false
                )

            }

        }
    }

    private fun displayPropertyDetails(){
        propertyRepository.propertyPicked?.let {
            emitResultDisplayProperty(it)
        }
    }

    private fun fetchDetailsProperty(){
        resultToViewState(Lce.Loading())
        if(searchPropertyJob?.isActive == true) searchPropertyJob?.cancel()

        val propertyId = propertyRepository.propertyPicked!!.property.id

        fun fetchProperty(){
            searchPropertyJob = launch {
                val property = propertyRepository.getProperty(propertyId)[0]
                propertyRepository.propertyPicked = property
                emitResultDisplayProperty(property)
            }
        }

        fetchProperty()

    }

    private fun emitResultDisplayProperty(property: PropertyWithAllData){
        val result: Lce<DetailsPropertyResult> = Lce.Content(DetailsPropertyResult.FetchDetailsResult(
                property.property, property.address[0], property.amenities, property.pictures
        ))
        resultToViewState(result)
    }
}