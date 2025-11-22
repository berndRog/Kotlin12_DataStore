package domain.utilities

import java.util.Locale
import java.util.UUID


fun newUuid(): String = UUID.randomUUID().toString()

fun emptyUuid(): String = "00000000-0000-0000-0000-000000000000"

fun createUuid(number:Int, value:Int): String =
   String.format(Locale.ROOT, "%08d-%04d-0000-0000-000000000000", number, value)
