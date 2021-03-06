package com.openclassrooms.realestatemanager.addAgent


import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.textfield.TextInputLayout
import com.openclassrooms.realestatemanager.R
import com.openclassrooms.realestatemanager.addAgent.ErrorSourceAddAgent.*
import com.openclassrooms.realestatemanager.injection.Injection
import com.openclassrooms.realestatemanager.mviBase.REMView
import com.openclassrooms.realestatemanager.utils.*
import com.openclassrooms.realestatemanager.utils.extensions.saveToInternalStorage
import pub.devrel.easypermissions.EasyPermissions


/**
 * A simple [Fragment] subclass.
 *
 */
class AddAgentView : Fragment(), REMView<AddAgentViewState>, EasyPermissions.PermissionCallbacks {

    @BindView(R.id.add_agent_view_firstname) lateinit var firstName: EditText
    @BindView(R.id.add_agent_view_lastname) lateinit var lastName: EditText
    @BindView(R.id.add_agent_view_email) lateinit var email: EditText
    @BindView(R.id.add_agent_view_phonenb) lateinit var phoneNumber: EditText
    @BindView(R.id.add_agent_view_firstname_layout) lateinit var firstNameLayout: TextInputLayout
    @BindView(R.id.add_agent_view_lastname_layout) lateinit var lastNameLayout: TextInputLayout
    @BindView(R.id.add_agent_view_phonenb_layout) lateinit var phoneNumberLayout: TextInputLayout
    @BindView(R.id.add_agent_view_email_layout) lateinit var emailLayout: TextInputLayout
    @BindView(R.id.add_agent_view_picture_agent) lateinit var profilePicture: ImageView

    private lateinit var viewModel: AddAgentViewModel
    private var uriProfileImage: String? = null
    private var urlInDevice: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_add_agent_view, container, false)
        ButterKnife.bind(this, view)

        configureViewModel()
        return view
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == RC_CHOOSE_PHOTO){
            if(resultCode == RESULT_OK){
                data?.let{
                    savePicturePicked(it)
                }
            }
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        showSnackBarMessage(getString(R.string.allow_storage))
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        if(requestCode == RC_IMAGE_PERMS) chooseProfilePictureFromPhone()
    }

    //--------------------
    // CLICK LISTENER
    //--------------------

    fun clickListenerToolbar(){
        viewModel.actionFromIntent(AddAgentIntent.AddAgentToDBIntent(
                uriProfileImage,
                urlInDevice,
                firstName.text.toString(),
                lastName.text.toString(),
                email.text.toString(),
                phoneNumber.text.toString()
        ))
    }

    @OnClick(R.id.add_agent_view_picture_agent)
    fun clickProfilePictureListener(){
        chooseProfilePictureFromPhone()
    }

    //--------------------
    // VIEW MODEL CONNECTION
    //--------------------

    override fun configureViewModel(){
        val viewModelFactory = Injection.providesViewModelFactory(activity!!.applicationContext)
        viewModel = ViewModelProviders.of(
                this,
                viewModelFactory
        ).get(AddAgentViewModel::class.java)

        viewModel.viewState.observe(this, Observer { render(it) })
    }

    //--------------------
    // STATE AND INTENT
    //--------------------

    override fun render(state: AddAgentViewState?){
        if (state == null) return
        if(state.isSaved) {
            renderPropertyAddedToDB()
        }
        if(state.errors != null){
            renderErrors(state.errors)
        }
    }

    private fun renderPropertyAddedToDB(){
        activity!!.setResult(RESULT_OK)
        activity!!.finish()
    }

    private fun renderErrors(errors: List<ErrorSourceAddAgent>){
        disableAllErrors()
        errors.forEach{
            when(it){
                FIRST_NAME_INCORRECT -> firstNameLayout.error = getString(R.string.error_message_first_name)
                LAST_NAME_INCORRECT -> lastNameLayout.error = getString(R.string.error_message_last_name)
                EMAIL_INCORRECT -> emailLayout.error = getString(R.string.error_message_email)
                PHONE_INCORRECT -> phoneNumberLayout.error = getString(R.string.error_message_phone)
                UNKNOW_ERROR -> showSnackBarMessage(getString(R.string.unknow_error))
                UPDATING_PICTURE -> showSnackBarMessage(getString(R.string.error_update_server))
            }
        }
    }

    private fun disableAllErrors(){
        firstNameLayout.isErrorEnabled = false
        lastNameLayout.isErrorEnabled = false
        emailLayout.isErrorEnabled = false
        phoneNumberLayout.isErrorEnabled = false
    }

    private fun chooseProfilePictureFromPhone(){
        if(requestPermissionStorage(this)) {
            startActivityForResult(intentSinglePicture(), RC_CHOOSE_PHOTO)
        } else {
            return
        }

    }

    private fun savePicturePicked(data: Intent){
        urlInDevice = data.data?.toString()
        val bitmap = MediaStore.Images.Media.getBitmap(activity!!.contentResolver, data.data)
        val uriInternal = bitmap.saveToInternalStorage(
                activity!!.applicationContext, generateName(), TypeImage.AGENT
        )
        uriProfileImage = uriInternal.toString()
        Glide.with(context!!)
                .load(uriProfileImage)
                .apply(RequestOptions.circleCropTransform())
                .into(profilePicture)

    }

    private fun showSnackBarMessage(message: String){
        val viewLayout = activity!!.findViewById<CoordinatorLayout>(R.id.base_activity_main_layout)
        showSnackBar(viewLayout, message)

    }
}
