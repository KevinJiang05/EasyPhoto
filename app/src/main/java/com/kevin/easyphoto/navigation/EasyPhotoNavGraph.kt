package com.kevin.easyphoto.navigation

import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kevin.easyphoto.data.admin.EasyPhotoLogger
import com.kevin.easyphoto.data.admin.EasyPhotoSettings
import com.kevin.easyphoto.data.media.AndroidEasyPhotoMediaStore
import com.kevin.easyphoto.feature.admin.AdminScreen
import com.kevin.easyphoto.feature.camera.CameraScreen
import com.kevin.easyphoto.feature.camera.CaptureResultScreen
import com.kevin.easyphoto.feature.gallery.GalleryScreen
import com.kevin.easyphoto.feature.home.HomeScreen
import com.kevin.easyphoto.feature.viewer.PhotoViewerScreen

@Composable
fun EasyPhotoNavGraph() {
    val navController = rememberNavController()
    val mediaStore = remember { AndroidEasyPhotoMediaStore() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings = remember { EasyPhotoSettings(context) }
    val logger = remember { EasyPhotoLogger(context) }

    NavHost(
        navController = navController,
        startDestination = EasyPhotoRoute.Home,
    ) {
        composable(EasyPhotoRoute.Home) {
            HomeScreen(
                mediaStore = mediaStore,
                settings = settings,
                logger = logger,
                onOpenRearCamera = { navController.navigate(EasyPhotoRoute.RearCamera) },
                onOpenFrontCamera = { navController.navigate(EasyPhotoRoute.FrontCamera) },
                onOpenGallery = { navController.navigate(EasyPhotoRoute.Gallery) },
                onOpenAdmin = {
                    settings.adminModeEnabled = true
                    navController.navigate(EasyPhotoRoute.Gallery)
                },
            )
        }
        composable(EasyPhotoRoute.RearCamera) {
            CameraScreen(
                mediaStore = mediaStore,
                settings = settings,
                logger = logger,
                lensFacing = CameraSelector.LENS_FACING_BACK,
                onBackHome = {
                    navController.popBackStack(EasyPhotoRoute.Home, inclusive = false)
                },
                onCaptured = { uri ->
                    navController.navigate("${EasyPhotoRoute.CaptureResult}/rear/${Uri.encode(uri.toString())}")
                },
                onOpenGallery = { uri ->
                    navController.navigate("${EasyPhotoRoute.Gallery}?selected=${Uri.encode(uri.toString())}")
                },
                onReturnToCamera = {
                    logger.log("拍照后继续后置拍摄")
                },
            )
        }
        composable(EasyPhotoRoute.FrontCamera) {
            CameraScreen(
                mediaStore = mediaStore,
                settings = settings,
                logger = logger,
                lensFacing = CameraSelector.LENS_FACING_FRONT,
                onBackHome = {
                    navController.popBackStack(EasyPhotoRoute.Home, inclusive = false)
                },
                onCaptured = { uri ->
                    navController.navigate("${EasyPhotoRoute.CaptureResult}/front/${Uri.encode(uri.toString())}")
                },
                onOpenGallery = { uri ->
                    navController.navigate("${EasyPhotoRoute.Gallery}?selected=${Uri.encode(uri.toString())}")
                },
                onReturnToCamera = {
                    logger.log("拍照后继续前置拍摄")
                },
            )
        }
        composable(
            route = "${EasyPhotoRoute.CaptureResult}/{lens}/{uri}",
            arguments = listOf(
                navArgument("lens") { type = NavType.StringType },
                navArgument("uri") { type = NavType.StringType },
            ),
        ) { entry ->
            val lens = entry.arguments?.getString("lens").orEmpty()
            val cameraRoute = if (lens == "front") EasyPhotoRoute.FrontCamera else EasyPhotoRoute.RearCamera
            val uri = Uri.parse(entry.arguments?.getString("uri").orEmpty())
            CaptureResultScreen(
                photoUri = uri,
                settings = settings,
                onTakeAnother = {
                    navController.navigate(cameraRoute) {
                        popUpTo(cameraRoute) { inclusive = true }
                    }
                },
                onOpenGallery = {
                    navController.navigate("${EasyPhotoRoute.Gallery}?selected=${Uri.encode(uri.toString())}")
                },
                onBack = {
                    navController.popBackStack()
                },
            )
        }
        composable(
            route = "${EasyPhotoRoute.Gallery}?selected={selected}",
            arguments = listOf(navArgument("selected") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }),
        ) { entry ->
            val selected = entry.arguments?.getString("selected")?.let(Uri::decode)
            GalleryScreen(
                mediaStore = mediaStore,
                settings = settings,
                logger = logger,
                selectedUri = selected,
                onBackHome = {
                    navController.popBackStack(EasyPhotoRoute.Home, inclusive = false)
                },
                onOpenCamera = { navController.navigate(EasyPhotoRoute.RearCamera) },
                onOpenPhoto = { uri ->
                    navController.navigate("${EasyPhotoRoute.Viewer}/${Uri.encode(uri)}")
                },
                onOpenSettings = {
                    navController.navigate(EasyPhotoRoute.Admin)
                },
            )
        }
        composable(EasyPhotoRoute.Admin) {
            AdminScreen(
                mediaStore = mediaStore,
                settings = settings,
                logger = logger,
                onBackHome = {
                    navController.popBackStack()
                },
            )
        }
        composable(
            route = "${EasyPhotoRoute.Viewer}/{uri}",
            arguments = listOf(navArgument("uri") { type = NavType.StringType }),
        ) { entry ->
            val uri = Uri.decode(entry.arguments?.getString("uri").orEmpty())
            PhotoViewerScreen(
                mediaStore = mediaStore,
                settings = settings,
                initialUri = uri,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
