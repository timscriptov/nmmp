package navigation

import cafe.adriel.voyager.core.registry.ScreenProvider

sealed class Screens : ScreenProvider {
    data object Main : Screens()
}
