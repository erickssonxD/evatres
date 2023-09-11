package com.example.evatres.vm

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.evatres.Screen

class CameraAppViewModel: ViewModel() {
    val screen = mutableStateOf(Screen.Form)

    val currentFullscreenPicture = mutableStateOf<Uri>(Uri.EMPTY)

    var onCameraPermissionsGranted: () -> Unit = {}
    var onLocationPermissionsGranted: () -> Unit = {}

    var permissionsLauncher: ActivityResultLauncher<Array<String>>? = null

    fun changeToPictureScreen() { screen.value = Screen.PictureCapture }
    fun changeToFormScreen() { screen.value = Screen.Form }
    fun changeToPictureFullScreen() { screen.value = Screen.PictureFullscreen }
}