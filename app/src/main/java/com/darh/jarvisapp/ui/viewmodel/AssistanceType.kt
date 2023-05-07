package com.darh.jarvisapp.ui.viewmodel

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
sealed class AssistanceType : Parcelable {
    data class ResponseSuggestion(val text: String) : AssistanceType()
}