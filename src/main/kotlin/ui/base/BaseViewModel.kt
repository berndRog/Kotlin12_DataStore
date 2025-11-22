package ui.base

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import ui.error.ErrorEvent

open class BaseViewModel(
   private val _tag: String
) : ViewModelFake(_tag) {

   // One-off UI events: no replay so late collectors don't see old errors
   private val _errorEvents = MutableSharedFlow<ErrorEvent?>(
      replay = 0,
      extraBufferCapacity = 1
   )
   val errorEvents: SharedFlow<ErrorEvent?> = _errorEvents.asSharedFlow()

   // Fire-and-forget helpers
   fun handleErrorEvent(
      throwable: Throwable? = null,
      message: String? = null,
      title: String? = null,
      action: String? = null
   ) {
      val errorMessage = throwable?.localizedMessage
         ?: message
         ?: "Unknown error"
      _errorEvents.tryEmit(ErrorEvent(errorMessage, title, action))
   }

   fun clearError() {
      _errorEvents.tryEmit(null) // optional “ack/clear” event if your handler expects it
   }
}