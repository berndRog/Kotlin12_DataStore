package ui.error

data class ErrorEvent(
    val message: String,
    val title: String? = null,
    val actionLabel: String? = null
)