package com.team.itexperts.shrinkflation

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.team.itexperts.shrinkflation.model.ThemeMode
import com.team.itexperts.shrinkflation.storage.Keys
import com.team.itexperts.shrinkflation.storage.dataStore
import com.team.itexperts.shrinkflation.ui.CatalogScreen
import com.team.itexperts.shrinkflation.ui.ComparisonScreen
import com.team.itexperts.shrinkflation.ui.SettingsScreen
import com.team.itexperts.shrinkflation.ui.SettingsViewModel
import com.team.itexperts.shrinkflation.ui.ShrinkflationScreen
import com.team.itexperts.shrinkflation.ui.UnitPriceScreen
import com.team.itexperts.shrinkflation.ui.theme.ShrinkflationTheme
import com.team.itexperts.shrinkflation.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val code = runBlocking { newBase.dataStore.data.first()[Keys.LANGUAGE] ?: "ru" }
        super.attachBaseContext(LocaleHelper.apply(newBase, code))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settingsVm: SettingsViewModel = hiltViewModel()
            val themeMode by settingsVm.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            ShrinkflationTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavHost()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
            launchSingleTop = true
        }
    }
    val items = listOf(
        Triple("unitprice", stringResource(R.string.nav_unitprice), Icons.Default.Calculate),
        Triple("shrinkflation", stringResource(R.string.nav_shrinkflation), Icons.Default.TrendingDown),
        Triple("comparison", stringResource(R.string.nav_comparison), Icons.Default.Compare),
        Triple("settings", stringResource(R.string.nav_settings), Icons.Default.Settings)
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { navController.navigate("catalog") }) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = stringResource(R.string.catalog_action_open)
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val current = backStackEntry?.destination?.route
                items.forEach { (route, label, icon) ->
                    NavigationBarItem(
                        selected = current == route,
                        onClick = { if (current != route) navigateToTab(route) },
                        label = { Text(label) },
                        icon = { Icon(icon, contentDescription = label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController = navController, startDestination = "unitprice", modifier = Modifier.padding(padding)) {
            composable("unitprice") { UnitPriceScreen() }
            composable("shrinkflation") { ShrinkflationScreen() }
            composable("comparison") {
                ComparisonScreen(
                    onNavigateToCatalog = { navController.navigate("catalog") }
                )
            }
            composable("settings") { SettingsScreen() }
            composable("catalog") {
                CatalogScreen(
                    onNavigateToComparison = { navigateToTab("comparison") },
                    onNavigateToCalculate = { navigateToTab("unitprice") }
                )
            }
        }
    }
}
