package com.example.a18478

import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem
import java.text.SimpleDateFormat
import java.util.*

data class Event(
    val eventType: String = "",
    val date: String = "",
    val time: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    var creatorUserId: String = "",
    var eventId: String = "",
    var dateOfMaking: String = ""
) : ClusterItem, Parcelable {

    init {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy.", Locale.getDefault())
        dateOfMaking = dateFormat.format(Date())
    }


    constructor() : this("", "", "", "", 0.0, 0.0, "", "", "")

    override fun getPosition(): LatLng {
        return LatLng(latitude, longitude)
    }

    override fun getTitle(): String? {
        return eventType
    }

    override fun getSnippet(): String? {
        return "$date, $time - $description"
    }


    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(eventType)
        parcel.writeString(date)
        parcel.writeString(time)
        parcel.writeString(description)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeString(creatorUserId)
        parcel.writeString(eventId)
        parcel.writeString(dateOfMaking)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Event> {
        override fun createFromParcel(parcel: Parcel): Event {
            return Event(parcel)
        }

        override fun newArray(size: Int): Array<Event?> {
            return arrayOfNulls(size)
        }
    }
 //za prenos podataka kroz activitije
    private constructor(parcel: Parcel) : this(
        eventType = parcel.readString() ?: "",
        date = parcel.readString() ?: "",
        time = parcel.readString() ?: "",
        description = parcel.readString() ?: "",
        latitude = parcel.readDouble(),
        longitude = parcel.readDouble(),
        creatorUserId = parcel.readString() ?: "",
        eventId = parcel.readString() ?: "",
        dateOfMaking = parcel.readString() ?: ""
    )
}
