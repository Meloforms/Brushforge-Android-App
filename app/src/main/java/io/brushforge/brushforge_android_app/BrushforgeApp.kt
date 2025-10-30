package io.brushforge.brushforge_android_app

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.brushforge.brushforge.feature.converter.R as ConverterR
import io.brushforge.brushforge.feature.mypaints.R as MyPaintsR
import io.brushforge.brushforge.feature.palettes.R as PalettesR
import io.brushforge.brushforge.feature.primed.R as PrimedR
import io.brushforge.brushforge.feature.profile.R as ProfileR
import io.brushforge.brushforge.feature.converter.ConverterScreen
import io.brushforge.brushforge.feature.mypaints.MyPaintsScreen
import io.brushforge.brushforge.feature.palettes.PalettesScreen
import io.brushforge.brushforge.feature.primed.PrimedScreen
import io.brushforge.brushforge.feature.profile.ProfileScreen

@Composable
fun BrushforgeApp(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val destinations = BrushforgeDestination.all
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = modifier,
        bottomBar = {
            BrushforgeBottomBar(
                destinations = destinations,
                currentDestination = currentDestination,
                onDestinationSelected = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BrushforgeDestination.Converter.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BrushforgeDestination.Converter.route) {
                ConverterScreen()
            }
            composable(BrushforgeDestination.MyPaints.route) {
                MyPaintsScreen()
            }
            composable(BrushforgeDestination.Palettes.route) {
                PalettesScreen()
            }
            composable(BrushforgeDestination.Primed.route) {
                PrimedScreen()
            }
            composable(BrushforgeDestination.Profile.route) {
                ProfileScreen()
            }
        }
    }
}

@Composable
private fun BrushforgeBottomBar(
    destinations: Array<BrushforgeDestination>,
    currentDestination: NavDestination?,
    onDestinationSelected: (BrushforgeDestination) -> Unit
) {
    NavigationBar {
        destinations.forEach { destination ->
            val selected = currentDestination
                ?.hierarchy
                ?.any { it.route == destination.route } == true

            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationSelected(destination) },
                icon = { Icon(imageVector = destination.icon, contentDescription = null) },
                label = { androidx.compose.material3.Text(text = stringResource(destination.labelRes)) }
            )
        }
    }
}

private enum class BrushforgeDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    Converter(
        route = "converter",
        labelRes = ConverterR.string.converter_title,
        icon = Icons.Outlined.SwapHoriz
    ),
    MyPaints(
        route = "mypaints",
        labelRes = MyPaintsR.string.my_paints_title,
        icon = Icons.Outlined.FormatPaint
    ),
    Palettes(
        route = "palettes",
        labelRes = PalettesR.string.palettes_title,
        icon = Icons.Outlined.Palette
    ),
    Primed(
        route = "primed",
        labelRes = PrimedR.string.primed_title,
        icon = Icons.Outlined.CameraAlt
    ),
    Profile(
        route = "profile",
        labelRes = ProfileR.string.profile_title,
        icon = Icons.Outlined.Person
    );

    companion object {
        val all: Array<BrushforgeDestination> = values()
    }
}
