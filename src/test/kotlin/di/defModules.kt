package di


import data.IDataStore
import data.local.Seed
import data.local.datastore.DataStore
import data.repositories.PersonRepository
import domain.IPersonRepository
import domain.utilities.logInfo
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun defModulesTest(
   appHomeName: String,
   directoryName: String,
   fileName: String,
   ioDispatcher: CoroutineDispatcher
): Module = module {

    val tag = "<-defModulesTest"

   single<CoroutineDispatcher>(named("dispatcherIo")) {
      ioDispatcher
   }

    // data modules
   logInfo(tag, "single    -> Seed")
   single<Seed> {
      Seed()
   }


   logInfo(tag, "single    -> DataStore: IDataStore")
   single<IDataStore> {
      DataStore(
         appHomeName = appHomeName,
         directoryName = directoryName,
         fileName = fileName,
         _seed = get<Seed>(),
         _dispatcherIO = get(named("dispatcherIo")),
      )
   }

    logInfo(tag, "single    -> PersonRepository: IPersonRepository")
    single<IPersonRepository> {
       PersonRepository(
          _dataStore = get<IDataStore>()
       )
    }
}