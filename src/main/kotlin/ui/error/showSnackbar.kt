package ui.error

fun showSnackbar(e: ErrorEvent) {
   println("[SNACKBAR] ${e.title ?: "Error"}: ${e.message}" +
      (e.actionLabel?.let { "  [Action: $it]" } ?: ""))
}