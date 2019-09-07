package com.openclassrooms.realestatemanager.addProperty

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import com.openclassrooms.realestatemanager.data.api.reponse.GeocodingApiResponse
import com.openclassrooms.realestatemanager.data.database.Converters
import com.openclassrooms.realestatemanager.data.entity.*
import com.openclassrooms.realestatemanager.data.repository.AgentRepository
import com.openclassrooms.realestatemanager.data.repository.CurrencyRepository
import com.openclassrooms.realestatemanager.data.repository.PropertyRepository
import com.openclassrooms.realestatemanager.mviBase.BaseViewModel
import com.openclassrooms.realestatemanager.mviBase.Lce
import com.openclassrooms.realestatemanager.mviBase.REMViewModel
import com.openclassrooms.realestatemanager.utils.BitmapDownloader
import com.openclassrooms.realestatemanager.utils.Currency
import com.openclassrooms.realestatemanager.utils.TypeAmenity
import com.openclassrooms.realestatemanager.utils.TypeImage
import com.openclassrooms.realestatemanager.utils.extensions.*
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.URL

/**
 * Created by galou on 2019-07-27
 */
class AddPropertyViewModel (
        private val agentRepository: AgentRepository,
        private val propertyRepository: PropertyRepository,
        private val currencyRepository: CurrencyRepository)
    : BaseViewModel<AddPropertyViewState>(), REMViewModel<AddPropertyIntent, AddPropertyResult>,
BitmapDownloader.Listeners{

    private var currentViewState = AddPropertyViewState()
        set(value) {
            field = value
            viewStateLD.value = value
        }

    val currency: LiveData<Currency>
        get() = currencyRepository.currency

    private lateinit var disposable: Disposable

    private lateinit var actionType: ActionType
    private lateinit var context: Context
    private var propertyId: Int? = null
    private lateinit var idFromApi: String

    // data
    private val listErrorInputs = mutableListOf<ErrorSourceAddProperty>()
    private lateinit var type: String
    private var price: Double? = null
    private var surface: Double? = null
    private var rooms: Int? = null
    private var bedrooms: Int? = null
    private var bathrooms: Int? = null
    private lateinit var description: String
    private lateinit var address: String
    private lateinit var street: String
    private lateinit var city: String
    private lateinit var postalCode: String
    private lateinit var state: String
    private lateinit var country: String
    private lateinit var neighborhood: String
    private lateinit var onMarketSince: String
    private var isSold: Boolean = false
    private var sellOn: String? = null
    private var agent: Int? = null
    private lateinit var amenities: List<TypeAmenity>
    private lateinit var pictures: List<Picture>
    private var map = ""
    private var latitude: Double? = null
    private var longitude: Double? = null
    private var addressForDisplay: String = ""


    //Coroutine job
    private var addPropertyJob: Job? = null
    private var addPropertyDataJob: Job? = null
    private var searchAgentsJob: Job? = null
    private var deletePreviousDataJob: Job? = null


    override fun actionFromIntent(intent: AddPropertyIntent) {
        when(intent) {
            is AddPropertyIntent.AddPropertyToDBIntent -> {
                receivePropertyData(
                        intent.type, intent.price,
                        intent.surface, intent.rooms,
                        intent.bedrooms, intent.bathrooms,
                        intent.description, intent.address,
                        intent.neighborhood, intent.onMarketSince,
                        intent.isSold, intent.sellDate,
                        intent.agent, intent.amenities,
                        intent.pictures, intent.context)

            }

            is AddPropertyIntent.OpenListAgentsIntent -> fetchAgentsFromDB()

            is AddPropertyIntent.SetActionTypeIntent -> setActionType(intent.actionType)
        }
    }

    override fun resultToViewState(result: Lce<AddPropertyResult>) {
        currentViewState = when (result){
            is Lce.Content ->{
                when(result.packet){
                    is AddPropertyResult.ListAgentsResult -> {
                        currentViewState.copy(
                                listAgents = result.packet.listAgents,
                                isLoading = false)
                    }

                    is AddPropertyResult.AddPropertyToDBResult -> {
                        currentViewState.copy(
                                isSaved = true,
                                listAgents = null,
                                errors = null,
                                isLoading = false
                        )
                    }
                    is AddPropertyResult.FetchedPropertyResult -> {
                        currentViewState.copy(
                                errors = null,
                                isLoading = false,
                                isModifyProperty = true,
                                price = result.packet.property!!.price,
                                surface = result.packet.property.surface,
                                rooms = result.packet.property.rooms,
                                bedrooms = result.packet.property.bedrooms,
                                bathrooms = result.packet.property.bathrooms,
                                description = result.packet.property.description,
                                type = result.packet.property.type.typeName,
                                onMarketSince = result.packet.property.onMarketSince.toStringForDisplay(),
                                isSold = result.packet.property.sold,
                                sellDate = result.packet.property.sellDate?.toStringForDisplay(),
                                address = result.packet.address!!.addressForDisplay,
                                neighborhood = result.packet.address.neighbourhood,
                                amenities = result.packet.amenities,
                                agentId = result.packet.agent!!.id,
                                agentFirstName = result.packet.agent.firstName,
                                agentLastName = result.packet.agent.lastName,
                                pictures = result.packet.pictures
                        )
                    }
                }
            }

            is Lce.Loading ->{
                currentViewState.copy(
                        listAgents = null,
                        isLoading = true)

            }

            is Lce.Error ->{
                when(result.packet){
                    is AddPropertyResult.AddPropertyToDBResult -> {
                        currentViewState.copy(
                                listAgents = null,
                                errors = result.packet.errorSource,
                                isLoading = false)
                    }

                    is AddPropertyResult.ListAgentsResult -> {
                        currentViewState.copy(
                                listAgents = null,
                                isLoading = false,
                                errors = result.packet.errorSource
                        )
                    }
                    is AddPropertyResult.FetchedPropertyResult -> {
                        currentViewState.copy(
                                listAgents = null,
                                isLoading = false,
                                errors = result.packet.errorSource
                        )
                    }
                }

            }
        }
    }

    private fun setActionType(actionType: ActionType){
        this.actionType = actionType
        if(actionType == ActionType.MODIFY_PROPERTY){
            fetchExistingPropertyFromDB()
        }
    }

    //--------------------
    // ADD PROPERTY TO DB
    //--------------------

    private fun receivePropertyData(type: String, price: Double?,
                                    surface: Double?, rooms: Int?,
                                    bedrooms: Int?, bathrooms: Int?,
                                    description: String, address: String,
                                    neighborhood: String, onMarketSince: String,
                                    isSold: Boolean, sellOn: String?,
                                    agent: Int?, amenities: List<TypeAmenity>,
                                    pictures: List<Picture>,
                                    contextApp: Context){
        fun setGlobalProperties() {
            context = contextApp
            this.type = type
            this.price = price
            this.surface = surface
            this.rooms = rooms
            this.bedrooms = bedrooms
            this.bathrooms = bathrooms
            this.description = description
            this.address = address
            this.neighborhood = neighborhood
            this.onMarketSince = onMarketSince
            this.isSold = isSold
            this.sellOn = sellOn
            this.agent = agent
            this.amenities = amenities
            this.pictures = pictures
        }

        listErrorInputs.clear()
        //resultToViewState(Lce.Loading())
        setGlobalProperties()
        checkErrorsFromUserInput().forEach { listErrorInputs.add(it) }
        fetchAddressLocation(address)
    }

    private fun checkLocationAndMap(geocodingApi: GeocodingApiResponse){
        val results = geocodingApi.results
        if(results.isNotEmpty()){
            configureAddressComponent(geocodingApi)
            val mapUrl = fetchMapFromApi(latitude!!, longitude!!)
            if( mapUrl != null){
                fetchBitmapMap(mapUrl)
            } else {
                emitResultAddPropertyToView()
            }
        } else {
            listErrorInputs.add(ErrorSourceAddProperty.TOO_MANY_ADDRESS)
            emitResultAddPropertyToView()
        }
    }

    override fun onBitmapDownloaded(bitmap: Bitmap) {
        map = bitmap.saveToInternalStorage(context, idFromApi, TypeImage.ICON_MAP).toString()
        emitResultAddPropertyToView()
    }

    private fun checkErrorsFromUserInput(): List<ErrorSourceAddProperty>{
        val listErrors = mutableListOf<ErrorSourceAddProperty>()
        val onMarketDate = onMarketSince.toDate()
        val sellDate = sellOn?.toDate()

        if(onMarketDate == null ||
                !onMarketDate.isCorrectOnMarketDate()) listErrors.add(ErrorSourceAddProperty.INCORRECT_ON_MARKET_DATE)
        if(isSold) {
            if (sellDate == null ||
                    onMarketDate != null
                    && !sellDate.isCorrectSoldDate(onMarketDate)) listErrors.add(ErrorSourceAddProperty.INCORRECT_SOLD_DATE)
            if (sellOn == null) listErrors.add(ErrorSourceAddProperty.NO_SOLD_DATE)
        }
        if(!type.isExistingPropertyType()) listErrors.add(ErrorSourceAddProperty.NO_TYPE_SELECTED)
        if(price == null) listErrors.add(ErrorSourceAddProperty.NO_PRICE)
        if(surface == null) listErrors.add(ErrorSourceAddProperty.NO_SURFACE)
        if(rooms == null) listErrors.add(ErrorSourceAddProperty.NO_ROOMS)
        if(address.isEmpty()) listErrors.add(ErrorSourceAddProperty.NO_ADDRESS)
        if(agent == null) listErrors.add(ErrorSourceAddProperty.NO_AGENT)
        pictures?.forEach {
            if(it.description.isNullOrBlank()){
                listErrors.add(ErrorSourceAddProperty.MISSING_DESCRIPTION)
            }
        }

        return listErrors
    }

    private fun fetchAddressLocation(address: String){
        disposable = propertyRepository.getLocationFromAddress(address.convertForApi())
                .subscribeWith(getObserverGeocodingApi())
    }

    private fun getObserverGeocodingApi(): DisposableObserver<GeocodingApiResponse>{
        return object : DisposableObserver<GeocodingApiResponse>() {
            override fun onNext(geocodingApi: GeocodingApiResponse) {
                checkLocationAndMap(geocodingApi)
            }

            override fun onError(e: Throwable) {
                listErrorInputs.add(ErrorSourceAddProperty.INCORECT_ADDRESS)
                emitResultAddPropertyToView()
            }

            override fun onComplete() {}
        }
    }

    private fun configureAddressComponent(geocodingApi: GeocodingApiResponse){
        val results = geocodingApi.results
        val result = results[0]
        latitude = result.geometry.location.lat
        longitude = result.geometry.location.lng
        addressForDisplay = result.formattedAddress
        var streetNumber = ""
        var streetName = ""
        result.addressComponents.forEach { component ->
            component.types.forEach {
                when(it){
                    "street_number" -> streetNumber = component.longName
                    "route" -> streetName = component.longName
                    "locality" -> city = component.longName
                    "administrative_area_level_1" -> state = component.shortName
                    "country" -> country = component.longName
                    "postal_code" -> postalCode = component.longName
                    "neighborhood" -> neighborhood = if(neighborhood.isEmpty()) component.longName else neighborhood
                }
            }
        }
        neighborhood = if(neighborhood.isEmpty()) city else neighborhood
        idFromApi = result.placeId
        street = "$streetNumber $streetName"
    }

    private fun fetchMapFromApi(lat: Double, lng: Double): URL? {
        return propertyRepository.getMapLocation(lat.toString(), lng.toString()).toUrl()
    }

    private fun fetchBitmapMap(mapUrl: URL){
        BitmapDownloader(this).execute(mapUrl)
    }

    private fun emitResultAddPropertyToView(){
        if(addPropertyJob?.isActive == true) addPropertyJob?.cancel()
        if(addPropertyDataJob?.isActive == true) addPropertyDataJob?.cancel()

        val amenitiesForDB = mutableListOf<Amenity>()

        var address: Address? = null

        fun emitResult(){
            val result: Lce<AddPropertyResult> = Lce.Content(AddPropertyResult.AddPropertyToDBResult(null))
            resultToViewState(result)
        }

        fun createObjectForDB(){
            if(pictures.isNotEmpty()){
                pictures.forEachIndexed { index, picture ->
                    picture.orderNumber = index
                    picture.property = propertyId

                }
            }

            amenities.forEach {
                amenitiesForDB.add(Amenity(null, propertyId!!, it))
            }

            address = Address(
                    propertyId!!, street, city, postalCode, country, state,
                    longitude!!, latitude!!, neighborhood, map, addressForDisplay
            )

            emitResult()
        }

        fun createDataPropertyInDB(){
            addPropertyDataJob = launch {
                propertyRepository.createDataProperty(amenitiesForDB, pictures, address!!, actionType)
            }

        }

        fun createNewPropertyInDB(){
            addPropertyJob = launch {
                val currency = currencyRepository.currency.value!!
                val hasPicture = pictures.isNotEmpty()
                val propertyForDB = Property(
                        propertyId, Converters.toTypeProperty(type), price!!.toEuro(currency),
                        surface!!.toSqMeter(currency), rooms!!,
                        bedrooms, bathrooms,
                        description, onMarketSince.toDate()!!,
                        isSold, sellOn?.toDate(), agent!!, hasPicture)
                when(actionType){
                    ActionType.NEW_PROPERTY -> propertyId = propertyRepository.createProperty(propertyForDB).toInt()
                    ActionType.MODIFY_PROPERTY -> propertyRepository.updateProperty(propertyForDB)
                }

                createObjectForDB()
                createDataPropertyInDB()
            }

        }

        fun deletePreviousAmenities(){
            if(deletePreviousDataJob?.isActive == true) deletePreviousDataJob?.cancel()
            deletePreviousDataJob = launch {
                propertyRepository.deletePreviousData(propertyId!!)
                createNewPropertyInDB()
            }

        }

        if(listErrorInputs.isEmpty()){
            if(actionType == ActionType.MODIFY_PROPERTY){
                deletePreviousAmenities()
            } else{
                createNewPropertyInDB()
            }
        } else {
            val result: Lce<AddPropertyResult> = Lce.Error(AddPropertyResult.AddPropertyToDBResult(listErrorInputs))
            resultToViewState(result)
        }

    }

    //--------------------
    // FETCH AGENTS
    //--------------------

    private fun fetchAgentsFromDB(){
        resultToViewState(Lce.Loading())

        if(searchAgentsJob?.isActive == true) searchAgentsJob?.cancel()

        searchAgentsJob = launch {
            val agents: List<Agent>? = agentRepository.getAllAgents()
            val result: Lce<AddPropertyResult> = if(agents == null || agents.isEmpty()){
                val listErrors = listOf(ErrorSourceAddProperty.ERROR_FETCHING_AGENTS)
                Lce.Error(AddPropertyResult.ListAgentsResult(null, listErrors))
            } else{
                Lce.Content(AddPropertyResult.ListAgentsResult(agents, null))
            }
            resultToViewState(result)
        }

    }

    //--------------------
    // FETCH EXISING PROPERTY
    //--------------------

    private fun fetchExistingPropertyFromDB(){
        if(searchAgentsJob?.isActive == true) searchAgentsJob?.cancel()

        var property: PropertyWithAllData? = null
        var agent: Agent? = null

        fun emitResult(){
            val result: Lce<AddPropertyResult>
            result = if(property == null){
                val errors = listOf(ErrorSourceAddProperty.ERROR_FETCHING_PROPERTY)
                Lce.Error(AddPropertyResult.FetchedPropertyResult(
                        null, null, null, null, null, errors
                ))
            } else {
                Lce.Content(AddPropertyResult.FetchedPropertyResult(
                        property!!.property, property!!.amenities.map { it.type },
                        property!!.pictures.sortedBy { it.orderNumber }, agent,
                        property!!.address[0], null
                ))
            }
            resultToViewState(result)
        }

        fun fetchAgent(){
            searchAgentsJob = launch {
                agent = agentRepository.getAgent(property!!.property.agent)[0]
                emitResult()
            }
        }

        fun fetchProperty(){
            property = propertyRepository.propertyPicked!!
            propertyId = property!!.property.id
        }

        fetchProperty()
        fetchAgent()

    }


}