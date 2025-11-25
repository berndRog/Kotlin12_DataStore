package di


import data.IDataStore
import data.local.Seed
import data.local.datastore.DataStore
import data.repositories.PersonRepository
import domain.IPersonRepository
import domain.utilities.logInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ui.people.PersonViewModel

val defModules: Module = module {
    val tag = "<-defModules"

   logInfo(tag, "single    -> dispatcherIO: CoroutineDispatcher")
   single<CoroutineDispatcher>(named("dispatcherIo")) {
      Dispatchers.IO
   }
   logInfo(tag, "single    -> dispatcherDefault: CoroutineDispatcher")
   single<CoroutineDispatcher>(named("dispatcherDefault")) {
      Dispatchers.Default
   }

    // data modules
   logInfo(tag, "single    -> Seed")
   single<Seed> {
      Seed()
   }

   logInfo(tag, "single    -> DataStore: IDataStore")
   single<IDataStore> {
      DataStore(
         appHomeName = null,
         directoryName = null,
         fileName = null,
         _seed = get<Seed>(),
         _dispatcherDefault = get(named("dispatcherDefault")),
         _dispatcherIO = get(named("dispatcherIo")),
      )
   }

    logInfo(tag, "single    -> PersonRepository: IPersonRepository")
    single<IPersonRepository> {
       PersonRepository(
          _dataStore = get<IDataStore>()
       )
    }

   logInfo(tag, "single    -> PersonViewModel")
   single<PersonViewModel> {
      PersonViewModel(
         _repository = get<IPersonRepository>()
      )
   }

}