package ui.base

import domain.utilities.logDebug
import domain.utilities.logError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.qualifier.named
import kotlin.coroutines.CoroutineContext

open class ViewModelFake(
   private val _tag: String
): KoinComponent {

   // SupervisorJob to manage coroutines,
   // allowing child coroutines to fail independently
   private val _job = SupervisorJob()

   // Dispatcher for main thread operations, as in ViewModel in Android
   private val _dispatcher: CoroutineDispatcher = get(named("dispatcherDefault"))

   // Exception handler to log uncaught exceptions in coroutines
   protected val _exceptionHandler = CoroutineExceptionHandler { _, e ->
      logError(_tag, "Coroutine error: ${e.message}")
   }

   // CoroutineContext
   private val _coroutineContext: CoroutineContext = _job + _dispatcher + _exceptionHandler

   // CoroutineScope for launching coroutines (launch/async) tied to this ViewModel
   val viewModelScope: CoroutineScope = CoroutineScope(_coroutineContext )
   // public access of coroutineContext via viewModelScope.coroutineContext

   // Called when ViewModel is no longer used and will be destroyed
   open fun onCleared() {
      // Log all child coroutines
      _job.children.forEach { child ->
         logDebug(_tag, "${child.toString()} cancelled")
         //child.cancel()
      }
      // cancel the SupervisorJob to clean up all coroutines
      _job.cancel() // Cancel all coroutines when ViewModel is cleared
   }
}