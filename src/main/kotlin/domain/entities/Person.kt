package domain.entities

import domain.utilities.newUuid
import kotlinx.serialization.Serializable

// @Immutable
@Serializable
data class Person (
   val firstName: String = "",
   val lastName: String = "",
   val email:String? = "",
   val phone:String? = "",
   val imagePath: String? = null,
   val id: String = newUuid()
)