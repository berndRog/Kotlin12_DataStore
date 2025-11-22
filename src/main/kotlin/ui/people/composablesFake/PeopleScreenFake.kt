package ui.people.composablesFake

import domain.utilities.logDebug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import ui.error.showSnackbar
import ui.people.PeopleUiState
import ui.people.PersonUiState
import ui.people.PersonViewModel
import kotlin.collections.forEach
import kotlin.collections.plusAssign

class PeopleScreenFake(
   private val _viewModel: PersonViewModel,
   private val _uiScope: CoroutineScope   // z.B. ein „UI“-Scope (Pseudo-Main/Default)
) {
   private val jobs = mutableListOf<Job>()

   // Startet das Sammeln wie eine Composable, die sichtbar wird
   fun startObserver() {
      logDebug(TAG, "startObserver")
      // PeopleUiState sammeln (Liste)
      jobs += _viewModel.peopleUiStateFlow
         .onEach { it: PeopleUiState -> renderPeople(it) }          // „UI-Render“
         .launchIn(_uiScope)
      jobs += _viewModel.errorEvents
         .onEach { evt ->
            if (evt != null) {
               showSnackbar(evt)    // your UI hook
               _viewModel.clearError()      // optional: acknowledge/clear
            }
         }
         .launchIn(_uiScope)
   }

   // Beendet die Sammlung wie bei onStop()/onDispose() 
   fun stopObserver() {
      logDebug(TAG, "stopObserver")
      jobs.forEach { it: Job ->
         logDebug(TAG, "${it.toString()} cancelled")
         it.cancel()
      }
      jobs.clear()
   }

   // --- "Render"-Methoden (hier nur Konsole) ---
   private fun renderPeople(state: PeopleUiState) {
      logDebug(TAG, "People: size=${state.people.size}")
      state.people.forEach { person ->
         logDebug(TAG, "${person.firstName} ${person.lastName}")
      }
   }

   companion object {
      const val TAG = "<-PeopleScreenFake"
   }
}