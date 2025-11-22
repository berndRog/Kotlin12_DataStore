package ui.people

import de.rogallab.mobile.ui.base.updateState
import domain.IPersonRepository
import domain.entities.Person
import domain.utilities.logDebug
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ui.base.BaseViewModel

class PersonViewModel(
   private val _repository: IPersonRepository
) : BaseViewModel(tag) {

   private val _personUiStateFlow = MutableStateFlow<PersonUiState>(PersonUiState())
   val personUiStateFlow = _personUiStateFlow.asStateFlow()

   private val _peopleUiStateFlow = MutableStateFlow<PeopleUiState>(PeopleUiState())
   val peopleUiStateFlow = _peopleUiStateFlow.asStateFlow()

   // Intents: no atomic updates of personUiStateFlow
   fun change(person: Person) {
      updateState(_personUiStateFlow) {
         this.copy(person = person)
      }
   }

   init {
      logDebug(tag, "PersonViewModel init")
      fetchSorted()
   }

   // Actions

   // Fetch all people from the repository and expose them via PeopleUiState.
   // This method works with any backend that provides Flow<Result<List<Person>>>,
   // e.g. Room, DataStore or a Retrofit wrapper.
   fun fetchSorted() {
      viewModelScope.launch {
         _repository.getAllSorted()
            // 1) Emit a loading state before the first result arrives
            .onStart {
               updateState(_peopleUiStateFlow) { copy(isLoading = true) }
            }
            // 2) Handle upstream exceptions (e.g. thrown inside the flow builder)
            .catch { t ->
               updateState(_peopleUiStateFlow) { copy(isLoading = false) }
               handleErrorEvent(t)
            }
            // 3) Consume each emitted Result<List<Person>>
            //    Map Result<List<Person>> -> PeopleUiState
            .collectLatest { result ->
               result
                  .onSuccess { people ->
                     val snapshot = people.toList() // defensive copy
                     updateState(_peopleUiStateFlow) {
                        copy(
                           isLoading = false,
                           people = snapshot
                        )
                     }
                  }
                  .onFailure { t ->
                     // Keep current people, clear loading and report error
                     updateState(_peopleUiStateFlow) { copy(isLoading = false) }
                     handleErrorEvent(t)
                  }
            }
      }
   }

   fun fetchSortedWithFold() {
      viewModelScope.launch {
         _repository.getAllSorted()
            // 1) Emit a loading state before the first result arrives
            .onStart {
               updateState(_peopleUiStateFlow) { copy(isLoading = true) }
            }
            // 2) Handle upstream exceptions (e.g. thrown inside the flow builder)
            .catch { t ->
               updateState(_peopleUiStateFlow) { copy(isLoading = false) }
               handleErrorEvent(t)
            }
            // 3) Consume each emitted Result<List<Person>>
            //    Map Result<List<Person>> -> PeopleUiState
            .collectLatest { result ->
               result.fold(
                  onSuccess = { people ->
                     val snapshot = people.toList()
                     updateState(_peopleUiStateFlow) {
                        copy(isLoading = false, people = snapshot)
                     }
                  },
                  onFailure = { t ->
                     // Keep current people, clear loading and report error
                     updateState(_peopleUiStateFlow) { copy(isLoading = false) }
                     handleErrorEvent(t)
                  }
               )
            }
      }
   }

   fun fetchById(id: String) {
      viewModelScope.launch {
         _repository.findById(id)
            .onSuccess { person: Person? ->
               logDebug(tag, "person: $person")
               person?.let {
                  updateState(_personUiStateFlow) { copy(person = it) }
               }
            }
            .onFailure { it: Throwable ->
               handleErrorEvent(it)
            }
      }
   }

   fun fetchByIdWithFold(id: String) {
      viewModelScope.launch {
         _repository.findById(id).fold(
            onSuccess = { person: Person? ->
               logDebug(tag, "person: $person")
               // transform Person? -> Person -> update personUiStateFlow
               person?.let {
                  updateState(_personUiStateFlow) { copy(person = it) }
               }
            },
            onFailure = { it: Throwable ->
               handleErrorEvent(it)
            }
         )
      }
   }

   fun insert() {
      viewModelScope.launch {
         _repository.create(_personUiStateFlow.value.person)
            .onSuccess { logDebug(tag, "insert") }
            .onFailure { handleErrorEvent(it) }
      }
   }

   fun update() {
      viewModelScope.launch {
         _repository.update(_personUiStateFlow.value.person)
            .onSuccess { logDebug(tag, "update") }
            .onFailure { handleErrorEvent(it) }
      }
   }

   fun remove() {
      viewModelScope.launch {
         _repository.remove(_personUiStateFlow.value.person)
            .onSuccess { logDebug(tag, "remove") }
            .onFailure { handleErrorEvent(it) }
      }
   }

   override fun onCleared() {
      logDebug(tag, "onCleared")
      super.onCleared()
   }

   companion object {
      private const val tag = "<-PersonViewModel"
   }

}