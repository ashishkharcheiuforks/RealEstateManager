package com.openclassrooms.realestatemanager.detailsProperty

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.openclassrooms.realestatemanager.R
import com.openclassrooms.realestatemanager.baseCurrency.BaseCurrencyActivity
import com.openclassrooms.realestatemanager.baseCurrency.BaseCurrencyIntent

class DetailActivity : BaseCurrencyActivity<DetailsPropertyView>(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAndBindLayout()
        configureViewModel()
        configureToolbar(null)
        configureAndShowView()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar_detail_property, menu)
        menuToolbar = menu
        viewModel.actionFromIntent(BaseCurrencyIntent.GetCurrentCurrencyIntent)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.menu_toolbar_currency -> {
                viewModel.actionFromIntent(BaseCurrencyIntent.ChangeCurrencyIntent)
                return true
            }
            R.id.menu_details_property_modify -> {
                view!!.toolBarModifyClickListener()
                return true
            }
            android.R.id.home -> finish()

        }
        return super.onOptionsItemSelected(item)
    }

    override fun createNewView(): DetailsPropertyView {
        return DetailsPropertyView()
    }
}
