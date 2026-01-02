package di

import data.repository.MainRepository
import data.repository.MainRepositoryImpl
import org.koin.core.module.Module
import org.koin.dsl.module
import ui.MainViewModel

object AppModules : FeatureModule {
    override val modules: List<Module>
        get() = listOf(
            viewModelsModule,
            repositoriesModule,
        )
}

private val viewModelsModule = module {
    factory {
        MainViewModel(
            mainRepository = get(),
        )
    }
}

private val repositoriesModule = module {
    factory<MainRepository> {
        MainRepositoryImpl()
    }
}