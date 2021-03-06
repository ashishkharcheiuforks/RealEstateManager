package com.openclassrooms.realestatemanager.detailsProperty

import com.openclassrooms.realestatemanager.data.entity.Address
import com.openclassrooms.realestatemanager.data.entity.Amenity
import com.openclassrooms.realestatemanager.data.entity.Picture
import com.openclassrooms.realestatemanager.data.entity.Property
import com.openclassrooms.realestatemanager.mviBase.REMIntent
import com.openclassrooms.realestatemanager.mviBase.REMResult
import com.openclassrooms.realestatemanager.mviBase.REMViewState

/**
 * Created by galou on 2019-08-25
 */

data class DetailsPropertyViewState(
        val isLoading: Boolean = false,
        val property: Property? = null,
        val address: Address? = null,
        val pictures: List<Picture>? = null,
        val amenities: List<Amenity>? = null
) : REMViewState

sealed class DetailsPropertyIntent : REMIntent{
    object FetchDetailsIntent : DetailsPropertyIntent()
    object DisplayDetailsIntent : DetailsPropertyIntent()
}

sealed class DetailsPropertyResult : REMResult {
    data class FetchDetailsResult(
            val property: Property?, val address: Address?,
            val amenities: List<Amenity>?, val pictures: List<Picture>?
    ) : DetailsPropertyResult()
}