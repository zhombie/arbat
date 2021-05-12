package kz.zhombie.cinema.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class Movie constructor(
    val uri: Uri,
    val info: Info? = null
) : Parcelable {

    companion object CREATOR : Parcelable.Creator<Movie> {
        override fun createFromParcel(parcel: Parcel): Movie {
            return Movie(parcel)
        }

        override fun newArray(size: Int): Array<Movie?> {
            return arrayOfNulls(size)
        }
    }

    constructor(parcel: Parcel) : this(
        uri = parcel.readParcelable(Uri::class.java.classLoader) ?: Uri.EMPTY,
        info = parcel.readParcelable(Info::class.java.classLoader)
    )

    data class Info constructor(
        val title: String?,
        val subtitle: String? = null
    ) : Parcelable {

        companion object CREATOR : Parcelable.Creator<Info> {
            override fun createFromParcel(parcel: Parcel): Info {
                return Info(parcel)
            }

            override fun newArray(size: Int): Array<Info?> {
                return arrayOfNulls(size)
            }
        }

        constructor(parcel: Parcel) : this(
            title = parcel.readString(),
            subtitle = parcel.readString()
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(title)
            parcel.writeString(subtitle)
        }

        override fun describeContents(): Int {
            return 0
        }

    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(uri, flags)
        parcel.writeParcelable(info, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

}