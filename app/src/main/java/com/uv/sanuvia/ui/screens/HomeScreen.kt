package com.uv.sanuvia.ui.screens // O el paquete correcto de tu UI

import android.Manifest
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.uv.sanuvia.R
import com.uv.sanuvia.ui.screens.common.PagerIndicator
import com.uv.sanuvia.ui.screens.common.ProfileAvatar
import com.uv.sanuvia.ui.screens.home.ArticulosSaludPage
import com.uv.sanuvia.ui.screens.home.EscaneoAlimentosPage
import com.uv.sanuvia.ui.screens.home.UbicacionCultivosPage
import kotlinx.coroutines.launch
import com.uv.sanuvia.ui.screens.ForoScreen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    homeViewModel: HomeScreenViewModel = viewModel(),
    onLogout: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onNavigateToArticuloDetail: (articuloId: String) -> Unit,

    onNavigateToCultivoDetail: (cultivoJson: String) -> Unit
) {
    val uiState by homeViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results: Map<String, Boolean> ->
        if (results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            Log.d("HomeScreen", "Permiso ubicación concedido, obteniendo ubicación...")
            homeViewModel.obtenerYGuardarDireccionUsuario()
        } else {
            Log.w("HomeScreen", "Permiso ubicación denegado.")
        }
    }

    // --- Efectos ---
    LaunchedEffect(Unit) {
        // Solicitar permisos al iniciar si no se han concedido
        // Esto también podría hacerse solo cuando el usuario navega a la página de ubicación
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Mostrar Snackbar para errores (manejados centralmente por HomeScreenViewModel)
    LaunchedEffect(uiState.scanError) {
        uiState.scanError?.let { errorMsg ->
            snackbarHostState.showSnackbar(message = errorMsg, duration = SnackbarDuration.Short)
            homeViewModel.limpiarErrorEscaneo()
        }
    }
    LaunchedEffect(uiState.locationError) {
        uiState.locationError?.let { errorMsg ->
            snackbarHostState.showSnackbar(message = errorMsg, duration = SnackbarDuration.Short)
            // Asume que tienes una función para limpiar este error en el ViewModel
            // homeViewModel.limpiarErrorUbicacion()
        }
    }
    // Podrías tener un LaunchedEffect para uiState.cultivosError también

    // --- UI Principal ---
    var menuExpanded by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(initialPage = 0) // Iniciar en Escaneo (índice 0)
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(

                title = {
                    Text(
                        "Sanuvia",
                        style = MaterialTheme.typography.headlineLarge,

                    )
                        }
                ,
                actions = {
                    IconButton(onClick = { menuExpanded = true }) { ProfileAvatar() }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar perfil") },
                            onClick = { menuExpanded = false; onEditProfile() }
                        )
                        DropdownMenuItem(
                            text = { Text("Cerrar sesión") },
                            onClick = {
                                menuExpanded = false
                                homeViewModel.logout()
                                onLogout()
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Indicador de Página primero
            PagerIndicator(
                pagerState = pagerState,
                pageCount = 5,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            ) { pageIndex ->
                scope.launch {
                    pagerState.scrollToPage(pageIndex)
                }
            }

            // HorizontalPager ocupa el espacio restante
            HorizontalPager(
                count = 5,
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> EscaneoAlimentosPage(
                        uiState = uiState,
                        onTomarFoto = { uri -> homeViewModel.procesarImagenAlimento(uri) }
                    )
                    1 -> UbicacionCultivosPage(
                        uiState = uiState,
                        onRetryFetchLocation = { homeViewModel.obtenerYGuardarDireccionUsuario() },
                        onCultivoClick = { cultivo ->
                            Log.d("HomeScreen", "Cultivo clickeado: ${cultivo.nombre}")
                            onNavigateToCultivoDetail(cultivo.nombre)
                        }
                    )
                    4 -> ArticulosSaludPage(
                        uiState = uiState,
                        onArticuloClick = { articuloId ->
                            onNavigateToArticuloDetail(articuloId)
                        }
                    )
                    2 -> ForoScreen()
                    3 ->{
                        StoreScreen()
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        HomeScreen(
            onNavigateToArticuloDetail = { /* No-op para preview */ },
            modifier = TODO(),
            homeViewModel = TODO(),
            onLogout = TODO(),
            onEditProfile = TODO(),
            onNavigateToCultivoDetail = TODO()
        )
    }
}