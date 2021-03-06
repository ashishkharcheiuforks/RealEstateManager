package com.openclassrooms.realestatemanager.injection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.openclassrooms.realestatemanager.addAgent.AddAgentViewModel
import com.openclassrooms.realestatemanager.addProperty.AddPropertyViewModel
import com.openclassrooms.realestatemanager.baseCurrency.BaseCurrencyViewModel
import com.openclassrooms.realestatemanager.data.repository.AgentRepository
import com.openclassrooms.realestatemanager.data.repository.CurrencyRepository
import com.openclassrooms.realestatemanager.data.repository.PropertyRepository
import com.openclassrooms.realestatemanager.data.repository.SaveDataRepository
import com.openclassrooms.realestatemanager.detailsProperty.DetailsPropertyViewModel
import com.openclassrooms.realestatemanager.listProperties.ListPropertyViewModel
import com.openclassrooms.realestatemanager.mainActivity.MainActivityViewModel
import com.openclassrooms.realestatemanager.searchProperty.SearchPropertyViewModel

/**
 * Created by galou on 2019-07-09
 */
class ViewModelFactory(
        private val agentRepository: AgentRepository,
        private val propertyRepository: PropertyRepository,
        private val currencyRepository: CurrencyRepository,
        private val saveDataRepository: SaveDataRepository
) : ViewModelProvider.Factory{
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when{
            modelClass.isAssignableFrom(MainActivityViewModel::class.java) -> MainActivityViewModel(agentRepository, currencyRepository, propertyRepository, saveDataRepository) as T
            modelClass.isAssignableFrom(AddAgentViewModel::class.java) -> AddAgentViewModel(agentRepository) as T
            modelClass.isAssignableFrom(AddPropertyViewModel::class.java) -> AddPropertyViewModel(agentRepository, propertyRepository, currencyRepository, saveDataRepository) as T
            modelClass.isAssignableFrom(ListPropertyViewModel::class.java) -> ListPropertyViewModel(propertyRepository, currencyRepository) as T
            modelClass.isAssignableFrom(DetailsPropertyViewModel::class.java) -> DetailsPropertyViewModel(propertyRepository, currencyRepository) as T
            modelClass.isAssignableFrom(SearchPropertyViewModel::class.java) -> SearchPropertyViewModel(agentRepository, propertyRepository, currencyRepository) as T
            modelClass.isAssignableFrom(BaseCurrencyViewModel::class.java) -> BaseCurrencyViewModel(currencyRepository) as T

            else -> throw Exception("Unknown ViewModel class")

        }

    }
}
