package com.example.evatres.vm

import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.evatres.Screen

class CameraAppViewModel: ViewModel() {
    val screen = mutableStateOf(Screen.Form)

    var onCameraPermissionsGranted: () -> Unit = {}
    var onLocationPermissionsGranted: () -> Unit = {}

    var permissionsLauncher: ActivityResultLauncher<Array<String>>? = null

    fun changeToPictureScreen() { screen.value = Screen.Picture }
    fun changeToFormScreen() { screen.value = Screen.Form }
}