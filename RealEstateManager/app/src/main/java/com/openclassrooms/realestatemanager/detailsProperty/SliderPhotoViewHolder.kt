package com.openclassrooms.realestatemanager.detailsProperty

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.bumptech.glide.RequestManager
import com.openclassrooms.realestatemanager.R
import com.openclassrooms.realestatemanager.data.entity.Picture
import com.openclassrooms.realestatemanager.utils.extensions.loadImage
import com.smarteist.autoimageslider.SliderViewAdapter

/**
 * Created by galou on 2019-09-07
 */
class SliderPhotoViewHolder(itemView: View) : SliderViewAdapter.ViewHolder(itemView) {

    @BindView(R.id.iv_auto_image_slider) lateinit var imageViewBackground: ImageView
    @BindView(R.id.tv_auto_image_slider) lateinit var textViewDescription: TextView

    init {
        ButterKnife.bind(this, itemView)

    }

    fun updateWithPicture(picture: Picture, glide: RequestManager){
        imageViewBackground.loadImage(picture.url, picture.serverUrl, glide)
        textViewDescription.text = picture.description
    }
}