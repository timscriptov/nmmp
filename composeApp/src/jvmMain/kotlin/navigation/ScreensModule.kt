package navigation

import cafe.adriel.voyager.core.registry.screenModule
import ui.MainScreen

val mainScreenModule = screenModule {
    register<Screens.Main> {
        MainScreen()
    }
}
