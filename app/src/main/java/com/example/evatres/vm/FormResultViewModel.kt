package com.example.evatres.vm

import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class FormResultViewModel: ViewModel() {
    val placeName = mutableStateOf("")
    val lat = mutableStateOf(0.0)
    val long = mutableStateOf(0.0)
    val pictureReceptionList = mutableStateListOf<Uri?>()
}