package com.openclassrooms.realestatemanager.mainActivity

import android.app.Activity
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import butterknife.BindView
import butterknife.ButterKnife
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.mapbox.mapboxsdk.Mapbox
import com.openclassrooms.realestatemanager.BuildConfig
import com.openclassrooms.realestatemanager.R
import com.openclassrooms.realestatemanager.addAgent.AddAgentActivity
import com.openclassrooms.realestatemanager.addProperty.ActionType
import com.openclassrooms.realestatemanager.addProperty.AddPropertyActivity
import com.openclassrooms.realestatemanager.detailsProperty.DetailActivity
import com.openclassrooms.realestatemanager.detailsProperty.DetailsPropertyView
import com.openclassrooms.realestatemanager.injection.Injection
import com.openclassrooms.realestatemanager.mainActivity.ErrorSourceMainActivity.*
import com.openclassrooms.realestatemanager.mviBase.REMView
import com.openclassrooms.realestatemanager.searchProperty.SearchActivity
import com.openclassrooms.realestatemanager.utils.*
import com.openclassrooms.realestatemanager.utils.TypeConnection.*
import com.wangjie.rapidfloatingactionbutton.RapidFloatingActionButton
import com.wangjie.rapidfloatingactionbutton.RapidFloatingActionHelper
import com.wangjie.rapidfloatingactionbutton.RapidFloatingActionLayout
import com.wangjie.rapidfloatingactionbutton.contentimpl.labellist.RFACLabelItem
import com.wangjie.rapidfloatingactionbutton.contentimpl.labellist.RapidFloatingActionContentLabelList
import com.wangjie.rapidfloatingactionbutton.util.RFABTextUtil
import pub.devrel.easypermissions.EasyPermissions
import java.lang.ref.WeakReference


class MainActivity : AppCompatActivity(), REMView<MainActivityViewState>,
        RapidFloatingActionContentLabelList.OnRapidFloatingActionContentLabelListListener<RFACLabelItem<Int>>,
        EasyPermissions.PermissionCallbacks
{

    interface OnListPropertiesChangeListener{
        fun onListPropertiesChange()
    }

    interface OnTabSelectedListener{
        fun onMapSelectedListener()
    }

    var callbackListPropertiesRefresh: WeakReference<OnListPropertiesChangeListener>? = null
    var callbackMapPropertiesRefresh: WeakReference<OnListPropertiesChangeListener>? = null
    var callbackTabListener: WeakReference<OnTabSelectedListener>? = null


            private lateinit var viewModel: MainActivityViewModel

    @BindView(R.id.main_activity_toolbar) lateinit var toolbar: Toolbar
    @BindView(R.id.main_activity_tablayout_viewpager) lateinit var viewPager: MainActivityViewPager
    @BindView(R.id.main_activity_tablayout) lateinit var tabLayout: TabLayout
    @BindView(R.id.activity_main_rfal) lateinit var rfaLayout: RapidFloatingActionLayout
    @BindView(R.id.activity_main_rfab) lateinit var rfaButton: RapidFloatingActionButton
    @BindView(R.id.main_activity_progressbar) lateinit var progressbar: ProgressBar
    private lateinit var rfabHelper: RapidFloatingActionHelper

    private var menuToolbar: Menu? = null

    private var detailsView: DetailsPropertyView? = null

    private val listDrawableIconTab = listOf(R.drawable.list_icon, R.drawable.map_icon)

    var isDoubleScreenMode = false
    private var downloading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(applicationContext, BuildConfig.MapBoxToken)

        setContentView(R.layout.activity_main)
        configureScreenMode()
        ButterKnife.bind(this)

        configureViewModel()
        configureToolbar()
        configureViewPagerAndTablayout()
        configureRapidFloatingActionButton()
        val auth = FirebaseAuth.getInstance()
        auth.signInAnonymously()

    }

    override fun onResume() {
        super.onResume()
        viewModel.actionFromIntent(MainActivityIntent.GetCurrentCurrencyIntent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){
            RC_CODE_ADD_AGENT -> {
                if(resultCode == Activity.RESULT_OK){
                    showSnackBarMessage(getString(R.string.agent_added))
                }
            }

            RC_CODE_ADD_PROPERTY -> {
                when(resultCode){
                    RESULT_SAVED_TO_DB -> {
                        showSnackBarMessage(getString(R.string.property_added))
                        updatePropertiesShown()
                    }
                    RESULT_SAVED_TO_DRAFT -> showSavedAsDraft()
                }
            }

            RC_CODE_MODIFY_PROPERTY -> {
                when(resultCode){
                    RESULT_SAVED_TO_DB -> {
                        showSnackBarMessage(getString(R.string.property_modified))
                        updatePropertiesShown()
                    }
                    RESULT_SAVED_TO_DRAFT -> showSavedAsDraft()
                }
            }
            RC_CODE_DETAIL_PROPERTY -> updatePropertiesShown()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        showSnackBarMessage(getString(R.string.allow_storage))
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        if(requestCode == RC_IMAGE_PERMS) checkNetworkConnection()
    }

    //--------------------
    // CONFIGURE UI
    //--------------------

    private fun configureScreenMode(){
        isDoubleScreenMode = findViewById<FrameLayout>(R.id.main_activity_frame_layout) != null
    }

    //------Toolbar---------

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if(isDoubleScreenMode){
            menuInflater.inflate(R.menu.menu_toolbar_main_double_screen, menu)
        } else{
            menuInflater.inflate(R.menu.menu_toolbar_main_activity, menu)
        }
        menuToolbar = menu
        viewModel.actionFromIntent(MainActivityIntent.GetCurrentCurrencyIntent)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            R.id.menu_toolbar_currency -> changeCurrency()
            R.id.menu_main_activity_search -> openSearchActivity()
            R.id.menu_details_property_modify -> openModifyProperty()
            R.id.menu_main_activity_refresh -> checkNetworkConnection()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun configureToolbar(){
        setSupportActionBar(toolbar)
    }

    private fun openSearchActivity(){
        val intent = Intent(this, SearchActivity::class.java)
        startActivity(intent)
    }

    private fun openModifyProperty(){
        startActivityForResult(modifyPropertyIntent(this), RC_CODE_MODIFY_PROPERTY)
    }

    //------View Pager and tablayout---------

    private fun configureViewPagerAndTablayout(){
        fun setupIconTabLayout(){
            tabLayout.getTabAt(0)?.setIcon(listDrawableIconTab[0])
            tabLayout.getTabAt(1)?.setIcon(listDrawableIconTab[1])
        }

        fun setupTabLayoutListener(){
            tabLayout.addOnTabSelectedListener(object :TabLayout.ViewPagerOnTabSelectedListener(viewPager){
                override fun onTabReselected(tab: TabLayout.Tab?) {

                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {
                    val tabIconColor = ContextCompat.getColor(applicationContext, R.color.colorTextPrimaryAlpha)
                    tab?.icon?.setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN)
                }

                override fun onTabSelected(tab: TabLayout.Tab?) {
                    val tabIconColor = ContextCompat.getColor(applicationContext, R.color.colorTextPrimary)
                    tab?.icon?.setColorFilter(tabIconColor, PorterDuff.Mode.SRC_IN)

                    if(viewPager.currentItem == 0) callbackTabListener?.get()?.onMapSelectedListener()

                }
            })
            viewPager.currentItem = 1
            viewPager.currentItem = 0
        }

        viewPager.adapter = PageAdapterMainActivity(supportFragmentManager)
        tabLayout.setupWithViewPager(viewPager)
        tabLayout.tabMode = TabLayout.MODE_FIXED
        setupIconTabLayout()
        setupTabLayoutListener()
    }

    //------Floating button---------

    private fun configureRapidFloatingActionButton() {
        val rfaContent = RapidFloatingActionContentLabelList(applicationContext)
        rfaContent.setOnRapidFloatingActionContentLabelListListener(this)
        val items = mutableListOf<RFACLabelItem<Int>>()
        items.add(RFACLabelItem<Int>()
                .setLabel(getString(R.string.add_property_menu))
                .setResId(R.drawable.home_icon)
                .setIconNormalColor(ContextCompat.getColor(applicationContext, R.color.colorTextPrimary))
                .setIconPressedColor(ContextCompat.getColor(applicationContext, R.color.colorWhite))
                .setWrapper(0)
        )
        items.add(RFACLabelItem<Int>()

                .setLabel(getString(R.string.add_agent_menu))
                .setResId(R.drawable.person_icon)
                .setIconNormalColor(ContextCompat.getColor(applicationContext, R.color.colorTextPrimary))
                .setIconPressedColor(ContextCompat.getColor(applicationContext, R.color.colorWhite))
                .setWrapper(1)
        )
        rfaContent
                .setItems(items as List<RFACLabelItem<Any>>?)
                .setIconShadowRadius(RFABTextUtil.dip2px(applicationContext, 3F))
                .setIconShadowColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
                .setIconShadowDy(RFABTextUtil.dip2px(applicationContext, 3F))
        rfabHelper = RapidFloatingActionHelper(
                applicationContext,
                rfaLayout,
                rfaButton,
                rfaContent
        ).build()
    }

    //------FAB click listener---------

    override fun onRFACItemIconClick(position: Int, item: RFACLabelItem<RFACLabelItem<Int>>?) {
        when(position){
            0 -> {
                viewModel.actionFromIntent(MainActivityIntent.OpenAddPropertyActivityIntent)}

            1 -> {
                if(isInternetAvailable(this)){
                    showAddAgentActivity()
                } else {
                    showSnackBarMessage(getString(R.string.internet_agent))
                }

            }
        }
        rfabHelper.toggleContent()
    }

    override fun onRFACItemLabelClick(position: Int, item: RFACLabelItem<RFACLabelItem<Int>>?) {
        onRFACItemIconClick(position, item)
    }

    //------2 views mode---------
    private fun showDetailsView(){
        detailsView = DetailsPropertyView()
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.main_activity_frame_layout, detailsView!!)
                .commit()
    }

    fun openDetailsProperty(){
        if(isDoubleScreenMode){
            showDetailsView()
        } else{
            val intent = Intent(this, DetailActivity::class.java)
            startActivityForResult(intent, RC_CODE_DETAIL_PROPERTY)
        }
    }

    //--------------------
    // VIEW MODEL CONNECTION
    //--------------------

    override fun configureViewModel(){
        val viewModelFactory = Injection.providesViewModelFactory(this)
        viewModel = ViewModelProviders.of(
                this,
                viewModelFactory
        ).get(MainActivityViewModel::class.java)

        viewModel.viewState.observe(this, Observer { render(it) })
    }

    private fun downloadNewPropertyFromNetwork(){
        if(requestPermissionStorage(this)) {
            viewModel.actionFromIntent(MainActivityIntent.UpdatePropertyFromNetwork(this.applicationContext))
        }
    }

    private fun changeCurrency(){
        viewModel.actionFromIntent(MainActivityIntent.ChangeCurrencyIntent)
    }

    //--------------------
    // STATE AND INTENT
    //--------------------

    override fun render(state: MainActivityViewState?) {
        if (state == null) return

        if(state.isOpenAddProperty)renderShowAddPropertyActivity()

        state.errorSource?.let { renderErrorOpeningActivity(it) }

        state.propertyAdded?.let { renderNewPropertyAdded(it) }

        renderLoading(state.isLoading)
        renderChangeCurrency(state.currency)

    }

    private fun renderShowAddPropertyActivity(){
        val intent = Intent(this, AddPropertyActivity::class.java)
        intent.putExtra(ACTION_TYPE_ADD_PROPERTY, ActionType.NEW_PROPERTY.actionName)
        startActivityForResult(intent, RC_CODE_ADD_PROPERTY)
    }

    private fun showAddAgentActivity(){
        val intent = Intent(this, AddAgentActivity::class.java)
        startActivityForResult(intent, RC_CODE_ADD_AGENT)
    }

    private fun renderErrorOpeningActivity(errorSource: ErrorSourceMainActivity){
        when(errorSource){
            NO_AGENT_IN_DB -> showSnackBarMessage(getString(R.string.create_agent_first))
            ERROR_FETCHING_NEW_FROM_NETWORK -> showSnackBarMessage(getString(R.string.error_fetching))
            ERROR_DOWNLOADING_IMAGES -> showSnackBarMessage(getString(R.string.pictire_not_available))
        }

    }

    private fun renderNewPropertyAdded(numberPropertyAdded: Int){
        val message = when(numberPropertyAdded){
            0 -> getString(R.string.no_new_property)
            1 -> String.format(getString(R.string.nb_property_added), "$numberPropertyAdded")
            else -> String.format(getString(R.string.nb_properties_added), "$numberPropertyAdded")
        }
        showSnackBarMessage(message)
        updatePropertiesShown()
    }

    private fun renderChangeCurrency(currency: Currency){
        menuToolbar?.let {
            when(currency){
                Currency.EURO -> {
                    val currencyItem = menuToolbar!!.findItem(R.id.menu_toolbar_currency)
                    currencyItem.icon = ContextCompat.getDrawable(applicationContext, R.drawable.euro_icon)
                }
                Currency.DOLLAR -> {
                    val currencyItem = menuToolbar!!.findItem(R.id.menu_toolbar_currency)
                    currencyItem.icon = ContextCompat.getDrawable(applicationContext, R.drawable.dollar_icon)
                }
            }
        }

    }

    private fun renderLoading(loading: Boolean){
        if(loading){
            progressbar.visibility = View.VISIBLE
            downloading = true
        } else {
            progressbar.visibility = View.GONE
            downloading = false
        }
    }

    //--------------------
    // UTILS
    //--------------------

    private fun updatePropertiesShown(){
        callbackListPropertiesRefresh?.get()?.onListPropertiesChange()
        callbackMapPropertiesRefresh?.get()?.onListPropertiesChange()
    }

    private fun showSnackBarMessage(message: String){
        val viewLayout = findViewById<CoordinatorLayout>(R.id.base_activity_main_layout)
        showSnackBar(viewLayout, message)

    }

    private fun checkNetworkConnection(){
        if(downloading) return
        when(typeNetworkConnection(this)){
            WIFI -> downloadNewPropertyFromNetwork()
            DATA -> showNoWifiDialog()
            NONE -> showSnackBarMessage(getString(R.string.connect_data_update))
        }
    }

    private fun showNoWifiDialog(){
        val builder = AlertDialog.Builder(this).apply {
            setMessage(getString(R.string.no_wifi_message))
            setTitle(getString(R.string.no_wifi_title))
            setPositiveButton(getString(R.string.ok_button)) { _, _ ->
                downloadNewPropertyFromNetwork()
            }
            setNegativeButton(getString(R.string.cancel_button)) { dialogInterface, _ ->
                dialogInterface.dismiss()

            }
            create()
        }
        builder.show()


    }

    private fun showSavedAsDraft(){
        when(isInternetAvailable(this)){
            true -> showSnackBarMessage(getString(R.string.modif_draft))
            false -> showSnackBarMessage(getString(R.string.saved_as_draft))
        }
    }
}

