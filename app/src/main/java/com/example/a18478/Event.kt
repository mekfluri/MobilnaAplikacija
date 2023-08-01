package com.example.a18478
import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

data class Event(
    val eventType: String,
    val date: String,
    val time: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val userList: MutableList<String> = mutableListOf(),
    var eventId: String = ""
) : ClusterItem, Parcelable {

    // Add a secondary constructor with no arguments (required by Firebase)
    constructor() : this("", "", "", "", 0.0, 0.0, mutableListOf())
    override fun getPosition(): LatLng {
        return LatLng(latitude, longitude)
    }

    override fun getTitle(): String? {
        return eventType
    }

    override fun getSnippet(): String? {
        return "$date, $time - $description"
    }

    // Implement Parcelable methods
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(eventType)
        parcel.writeString(date)
        parcel.writeString(time)
        parcel.writeString(description)
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeStringList(userList)
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

    private constructor(parcel: Parcel) : this(
        eventType = parcel.readString() ?: "",
        date = parcel.readString() ?: "",
        time = parcel.readString() ?: "",
        description = parcel.readString() ?: "",
        latitude = parcel.readDouble(),
        longitude = parcel.readDouble(),
        userList = parcel.createStringArrayList()?.toMutableList() ?: mutableListOf()
    )

}
