package com.openclassrooms.realestatemanager.listProperties

import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.openclassrooms.realestatemanager.R
import com.openclassrooms.realestatemanager.data.entity.PropertyWithAllData
import com.openclassrooms.realestatemanager.injection.Injection
import com.openclassrooms.realestatemanager.mainActivity.MainActivity
import com.openclassrooms.realestatemanager.mviBase.BaseFragmentREM
import com.openclassrooms.realestatemanager.mviBase.REMView
import com.openclassrooms.realestatemanager.searchResult.SearchResultActivity
import com.openclassrooms.realestatemanager.utils.ACTION_TYPE_LIST_PROPERTY
import com.openclassrooms.realestatemanager.utils.Currency
import com.openclassrooms.realestatemanager.utils.showSnackBar

/**
 * Created by galou on 2019-08-21
 */
abstract class BaseViewListProperties : BaseFragmentREM(), REMView<PropertyListViewState> {

    protected lateinit var viewModel: ListPropertyViewModel
    protected var currentCurrency: Currency? = null

    protected abstract fun renderListProperties(properties: List<PropertyWithAllData>, itemSelected: Int?)
    protected abstract fun renderIsLoading(loading: Boolean)
    protected abstract fun renderChangeCurrency(currency: Currency)

    //--------------------
    // VIEW MODEL CONNECTION
    //--------------------

    override fun configureViewModel(){
        val viewModelFactory = Injection.providesViewModelFactory(activity!!.applicationContext)
        viewModel = ViewModelProviders.of(
                this,
                viewModelFactory
        ).get(ListPropertyViewModel::class.java)

        viewModel.viewState.observe(this, Observer { render(it) })

    }

    protected fun configureActionType(){
        val argument = arguments?.getString(ACTION_TYPE_LIST_PROPERTY, ActionTypeList.ALL_PROPERTIES.actionName)
        argument?.let{
            val actionType = ActionTypeList.valueOf(it)
            viewModel.actionFromIntent(PropertyListIntent.SetActionTypeIntent(actionType))
            updatePropertiesDisplay()
        }

    }

    fun updatePropertiesDisplay(){
        viewModel.actionFromIntent(PropertyListIntent.DisplayPropertiesIntent)
    }


    override fun render(state: PropertyListViewState?) {
        if (state == null) return

        if (state.openDetails) {
            renderOpenPropertyDetails(state.itemSelected)
            return
        }

        if(state.listProperties != null  && !state.isLoading){
            renderListProperties(state.listProperties, state.itemSelected)
        }

        state.errorSource?.let { renderErrorFetchingProperty(it) }

        renderIsLoading(state.isLoading)

    }

    private fun renderOpenPropertyDetails(itemPosition: Int?){
        when(activity) {
            is MainActivity -> (activity as MainActivity).openDetailsProperty()
            is SearchResultActivity -> (activity as SearchResultActivity).openDetailsProperty()
        }
    }

    private fun renderErrorFetchingProperty(errorSource: ErrorSourceListProperty){
        when(errorSource){
            ErrorSourceListProperty.NO_PROPERTY_IN_DB -> showSnackBarMessage(getString(R.string.no_properties))
            ErrorSourceListProperty.CAN_T_ACCESS_DB -> showSnackBarMessage(getString(R.string.unknow_error))
        }
    }

    protected fun currencyObserver(){
        viewModel.currency.observe(this, Observer {currency ->
            currentCurrency = currency
            renderChangeCurrency(currentCurrency!!)
        })
    }

    protected fun setPropertyPicked(property: PropertyWithAllData, itemPosition: Int?){
        viewModel.actionFromIntent(PropertyListIntent.OpenPropertyDetailIntent(property, itemPosition))

    }

    protected fun showSnackBarMessage(message: String){
        val viewLayout = activity!!.findViewById<CoordinatorLayout>(R.id.base_activity_main_layout)
        showSnackBar(viewLayout, message)

    }
}