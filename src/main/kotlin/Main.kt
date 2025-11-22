import data.IDataStore
import di.defModules
import domain.utilities.logError
import kotlinx.coroutines.*
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import ui.people.PersonViewModel
import ui.people.composablesFake.PeopleScreenFake
import ui.people.composablesFake.PersonScreenFake

suspend fun main() {
   val tag = "<-main"

   val koinApp= startKoin {
      modules(defModules)
   }
   val koin = koinApp.koin
   val dataStore: IDataStore = koin.get<IDataStore>()
   val viewModel: PersonViewModel = koin.get<PersonViewModel>()

   val uiScope = CoroutineScope(
      SupervisorJob() +
         CoroutineExceptionHandler { _, exception ->
            logError(tag,"CoroutineException: $exception")
         } +
         koin.get<CoroutineDispatcher>(named("dispatcherDefault"))
   )

   val personScreen = PersonScreenFake(
      _viewModel = viewModel,
      _uiScope = uiScope
   )

   val peopleScreen = PeopleScreenFake(
      _viewModel = viewModel,
      _uiScope = uiScope
   )


   val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

   val job = appScope.launch {
      println("Using DataStore ${dataStore.filePath}")
      dataStore.initialize()

      // Navigate to PeopleScreen
      peopleScreen.startObserver()
      delay(2000)

      // Navigate to PersonScreen
      peopleScreen.stopObserver()
      personScreen.startObserver()

      viewModel.fetchById(id = "01000000-0000-0000-0000-000000000000")
      delay(2000)
      personScreen.stopObserver()

      // wait for user to terminate the app
      delay(1000)
      println("Press ENTER to terminate main")
      readLine()

      viewModel.onCleared()
   }
   // wait for the launched job to finish (suspend, not blocking thread)
   job.join()

   // Close Koin
   koinApp.close()


}



