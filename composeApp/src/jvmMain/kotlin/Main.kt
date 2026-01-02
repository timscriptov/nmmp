import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cafe.adriel.voyager.core.registry.ScreenRegistry
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import di.AppModules
import navigation.mainScreenModule
import org.koin.core.context.startKoin
import ui.MainScreen

fun main() = application {
    startKoin {
        val featureModules = listOf(
            AppModules.modules,
        ).flatten()
        modules(featureModules)
    }
    ScreenRegistry {
        mainScreenModule()
    }
    Window(
        title = "NMMP",
        state = rememberWindowState(width = 800.dp, height = 600.dp),
        onCloseRequest = ::exitApplication,
    ) {
        MaterialTheme {
            val screen = MainScreen()
            Navigator(screen) { nav ->
                SlideTransition(nav)
            }
        }
    }
}
