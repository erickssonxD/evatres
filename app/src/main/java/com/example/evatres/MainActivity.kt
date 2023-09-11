package com.example.evatres

import android.Manifest.*
import android.content.Context
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.evatres.ui.theme.EvatresTheme
import com.example.evatres.vm.CameraAppViewModel
import com.example.evatres.vm.FormResultViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.time.LocalDateTime

enum class Screen {
    Form,
    Picture
}

class MainActivity : ComponentActivity() {
    val cameraAppVm: CameraAppViewModel by viewModels()
    lateinit var cameraController: LifecycleCameraController

    val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            when {
                (it[permission.ACCESS_FINE_LOCATION]
                    ?: false) or (it[permission.ACCESS_COARSE_LOCATION]
                    ?: false) -> {
                    Log.v("callback RequestMultiplePermissions", "location permissions granted")
                    cameraAppVm.onLocationPermissionsGranted()
                }

                (it[permission.CAMERA] ?: false) -> {
                    Log.v("callback RequestMultiplePermissions", "camera permissions granted")
                    cameraAppVm.onCameraPermissionsGranted()
                }

                else -> {
                    Log.v("RequestMultiplePermissions", "One or permissions were not granted")
                }
            }
        }

    private fun setupCamera() {
        cameraController = LifecycleCameraController(this)
        cameraController.bindToLifecycle(this)
        cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraAppVm.permissionsLauncher = permissionsLauncher
        setupCamera()

        setContent {
            EvatresTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainActivityUI(cameraController)
                }
            }
        }
    }
}

fun generarNombreSegunFechaHastaSegundo(): String =
    LocalDateTime.now().toString().replace(Regex("[T:.-]"), "").substring(0, 14)

fun crearArchivoImagenPrivado(contexto: Context): File = File(
    contexto.getExternalFilesDir(
        Environment.DIRECTORY_PICTURES
    ), "${generarNombreSegunFechaHastaSegundo()}.jpg"
)

fun uri2imageBitmap(uri: Uri, contexto: Context) =
    BitmapFactory.decodeStream(contexto.contentResolver.openInputStream(uri)).asImageBitmap()

fun tomarFotografia(
    cameraController: CameraController,
    archivo: File,
    contexto: Context,
    imagenGuardadaOk: (uri: Uri) -> Unit
) {
    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(archivo).build()
    cameraController.takePicture(
        outputFileOptions,
        ContextCompat.getMainExecutor(contexto),
        object :
            ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                outputFileResults.savedUri?.also {
                    Log.v("tomarFotografia()::onImageSaved", "Foto guardada en ${it.toString()}")
                    imagenGuardadaOk(it)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("tomarFotografia()", "Error: ${exception.message}")
            }
        })
}

class SinPermisoException(mensaje: String) : Exception(mensaje)

fun getUbicacion(contexto: Context, onUbicacionOk: (location: Location) -> Unit): Unit {
    try {
        val servicio = LocationServices.getFusedLocationProviderClient(contexto)
        val tarea = servicio.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        )
        tarea.addOnSuccessListener { onUbicacionOk(it) }
    } catch (e: SecurityException) {
        throw SinPermisoException(e.message ?: "No tiene permisos para conseguir la ubicación")
    }
}

@Composable
fun MainActivityUI(cameraController: CameraController, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val formRecepcionVm: FormResultViewModel = viewModel()
    val cameraAppViewModel: CameraAppViewModel = viewModel()
    when (cameraAppViewModel.screen.value) {
        Screen.Form -> {
            FormUI(
                formRecepcionVm,
                tomarFotoOnClick = {
                    cameraAppViewModel.changeToPictureScreen()
                    cameraAppViewModel.permissionsLauncher?.launch(
                        arrayOf(permission.CAMERA)
                    )
                },
                actualizarUbicacionOnClick = {
                    cameraAppViewModel.onLocationPermissionsGranted = {
                        getUbicacion(contexto) {
                            formRecepcionVm.lat.value = it.latitude
                            formRecepcionVm.long.value = it.longitude
                        }
                    }
                    cameraAppViewModel.permissionsLauncher?.launch(
                        arrayOf(
                            permission.ACCESS_FINE_LOCATION,
                            permission.ACCESS_COARSE_LOCATION
                        )
                    )
                })
        }

        Screen.Picture -> {
            PictureUI(formRecepcionVm, cameraAppViewModel, cameraController)
        }

        else -> {
            Log.v("AppUI()", "when else, no debería entrar aquí")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormUI(
    formReceptionVm: FormResultViewModel,
    tomarFotoOnClick: () -> Unit = {},
    actualizarUbicacionOnClick: () -> Unit = {}
) {
    val contexto = LocalContext.current
    Column(
        modifier =
        Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            label = { Text(stringResource(R.string.text_edit_place)) },
            value = formReceptionVm.placeName.value,
            onValueChange = { formReceptionVm.placeName.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
        )
        Text("Fotografía de la recepción de la encomienda:")
        Button(onClick =
        { tomarFotoOnClick() }) { Text("Tomar Fotografía") }


        LazyRow {
            items(items = formReceptionVm.pictureReceptionList, itemContent = { item ->
                Box(Modifier.size(200.dp, 100.dp)) {
                    Image(
                        painter = BitmapPainter(uri2imageBitmap(item!!, contexto)),
                        contentDescription = "im")
                }
            })
        }
        Text("La ubicación es: lat: ${formReceptionVm.lat.value} y long: ${formReceptionVm.long.value}")
        Button(onClick =
        { actualizarUbicacionOnClick() }) { Text("Actualizar Ubicación") }
        Spacer(
            Modifier.height(
                100.dp
            )
        )
        MapOsmUI(formReceptionVm.lat.value, formReceptionVm.long.value)
    }
}

@Composable
fun PictureUI(
    formReceptionVm: FormResultViewModel,
    appViewModel: CameraAppViewModel,
    cameraController: CameraController
) {
    val ctx = LocalContext.current
    AndroidView(
        factory =
        { PreviewView(it).apply { controller = cameraController } },
        modifier = Modifier.fillMaxSize()
    )
    Button(
        onClick = {
            tomarFotografia(
                cameraController,
                crearArchivoImagenPrivado(ctx),
                ctx
            ) {
                formReceptionVm.pictureReceptionList.add(it)
                appViewModel.changeToFormScreen()
            }
        },
        modifier = Modifier
    ) { Text("Tomar foto") }
}

@Preview
@Composable
fun TakePictureUIPreview() {
    val cameraController = LifecycleCameraController(LocalContext.current)
    cameraController.bindToLifecycle(LocalLifecycleOwner.current)
    cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    PictureUI(
        formReceptionVm = FormResultViewModel(),
        appViewModel = CameraAppViewModel(),
        cameraController = cameraController
    )
}

@Composable
fun MapOsmUI(latitud: Double, longitud: Double) {
    val ctx = LocalContext.current
    AndroidView(factory = {
        MapView(it).also {
            it
            it.setTileSource(TileSourceFactory.MAPNIK)
            Configuration.getInstance().userAgentValue =
                ctx.packageName
        }
    }, update = {
        it.overlays.removeIf { true }
        it.invalidate()
        it.controller.setZoom(18.0)
        val geoPoint = GeoPoint(latitud, longitud)
        it.controller.animateTo(geoPoint)
        val marcador = Marker(it)
        marcador.position = geoPoint
        marcador.setAnchor(
            Marker.ANCHOR_CENTER,
            Marker.ANCHOR_CENTER
        )
        it.overlays.add(marcador)
    })
}