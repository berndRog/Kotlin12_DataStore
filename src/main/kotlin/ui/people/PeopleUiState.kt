package ui.people

import domain.entities.Person

data class PeopleUiState(
   val isLoading: Boolean = false,
   val people: List<Person> = emptyList()
)