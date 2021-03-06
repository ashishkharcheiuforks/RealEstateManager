package com.openclassrooms.realestatemanager.utils.extensions

import com.openclassrooms.realestatemanager.utils.DATE_FORMAT
import com.openclassrooms.realestatemanager.utils.DATE_FORMAT_WITH_TIME
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by galou on 2019-07-27
 */

fun Date.isCorrectOnMarketDate(): Boolean{
    val todayDate = Calendar.getInstance().time
    if(this.after(todayDate)) return false

    return true

}

fun Date.isCorrectSoldDate(onMarketDate: Date): Boolean{
    val todayDate = Calendar.getInstance().time
    return !onMarketDate.after(this) && !this.after(todayDate)
}

fun Date.toStringForDisplay(): String{
    val formatter = SimpleDateFormat(DATE_FORMAT, Locale.CANADA)
    return formatter.format(this.time)
}

fun Date.toCalendar(): Calendar?{
    val calendar = Calendar.getInstance()
    calendar.time = this

    return calendar
}

fun Date.toStringWithTime(): String{
    val formatter = SimpleDateFormat(DATE_FORMAT_WITH_TIME, Locale.CANADA)
    return formatter.format(this.time)
}