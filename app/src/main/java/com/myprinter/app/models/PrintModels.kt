package com.myprinter.app.models

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class PrintItem(
    val uri: Uri,
    val fileName: String,
    val fileType: FileType,
    val pageCount: Int,
    val widthPx: Int,
    val heightPx: Int,
    val orientation: Int,
    val dimensionsMm: Pair<Float, Float>? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(Uri::class.java.classLoader)!!,
        parcel.readString()!!,
        FileType.values()[parcel.readInt()],
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        null
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(uri, flags)
        parcel.writeString(fileName)
        parcel.writeInt(fileType.ordinal)
        parcel.writeInt(pageCount)
        parcel.writeInt(widthPx)
        parcel.writeInt(heightPx)
        parcel.writeInt(orientation)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<PrintItem> {
            override fun createFromParcel(parcel: Parcel): PrintItem = PrintItem(parcel)
            override fun newArray(size: Int): Array<PrintItem?> = arrayOfNulls(size)
        }
    }
}

enum class FileType : Parcelable {
    IMAGE, PDF;

    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(ordinal)
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<FileType> {
            override fun createFromParcel(parcel: Parcel): FileType = values()[parcel.readInt()]
            override fun newArray(size: Int): Array<FileType?> = arrayOfNulls(size)
        }
    }
}

data class PrintSettings(
    val paperSize: String = "A4",
    val quality: String = "Normal",
    val orientation: String = "Auto",
    val scaling: String = "Scale to fit",
    val position: String = "Center",
    val colorMode: String = "Color",
    val copies: Int = 1,
    val pageRange: String = "All"
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(paperSize)
        parcel.writeString(quality)
        parcel.writeString(orientation)
        parcel.writeString(scaling)
        parcel.writeString(position)
        parcel.writeString(colorMode)
        parcel.writeInt(copies)
        parcel.writeString(pageRange)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<PrintSettings> {
            override fun createFromParcel(parcel: Parcel): PrintSettings = PrintSettings(parcel)
            override fun newArray(size: Int): Array<PrintSettings?> = arrayOfNulls(size)
        }
    }
}

sealed class PrinterDestination : Parcelable {
    object Pdf : PrinterDestination() {
        override fun describeContents(): Int = 0
        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(0)
        }
    }

    data class Usb(
        val deviceName: String,
        val manufacturerName: String?,
        val productName: String?,
        val vendorId: Int,
        val productId: Int
    ) : PrinterDestination() {
        override fun describeContents(): Int = 0
        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(1)
            dest.writeString(deviceName)
            dest.writeString(manufacturerName)
            dest.writeString(productName)
            dest.writeInt(vendorId)
            dest.writeInt(productId)
        }
    }

    object WifiPlaceholder : PrinterDestination() {
        override fun describeContents(): Int = 0
        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(2)
        }
    }

    object BluetoothPlaceholder : PrinterDestination() {
        override fun describeContents(): Int = 0
        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(3)
        }
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<PrinterDestination> {
            override fun createFromParcel(parcel: Parcel): PrinterDestination {
                return when (val type = parcel.readInt()) {
                    0 -> Pdf
                    1 -> Usb(
                        parcel.readString()!!,
                        parcel.readString(),
                        parcel.readString(),
                        parcel.readInt(),
                        parcel.readInt()
                    )
                    2 -> WifiPlaceholder
                    3 -> BluetoothPlaceholder
                    else -> throw IllegalArgumentException("Unknown type $type")
                }
            }

            override fun newArray(size: Int): Array<PrinterDestination?> = arrayOfNulls(size)
        }
    }
}
