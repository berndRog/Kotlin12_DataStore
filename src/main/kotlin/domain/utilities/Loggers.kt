package domain.utilities

import Globals.isComp
import Globals.isDebug
import Globals.isInfo
import Globals.isVerbose

// Logger as function type
typealias LogFunction = (tag: String, message: String) -> Unit

internal fun formatMessage(message: String) =
   String.format("%-110s %s", message, Thread.currentThread().toString())

// internal logger as Android logger
internal var errorLogger: LogFunction = { tag, msg -> println("E/$tag, ${formatMessage(msg)}" ) }
internal var warningLogger: LogFunction = { tag, msg -> println("W/$tag: ${formatMessage(msg)}") }
internal var infoLogger: LogFunction = { tag, msg -> if(isInfo) println("I/$tag: ${formatMessage(msg)}")  }
internal var debugLogger: LogFunction = { tag, msg -> if(isDebug) println("D/$tag: ${formatMessage(msg)}") }
internal var verboseLogger: LogFunction = { tag, msg -> if(isVerbose) println("V/$tag: $msg")  }
internal var compLogger: LogFunction = { tag, msg -> if(isComp) println("C$tag: $msg") }


// public functions
fun logError(tag: String, message: String) = errorLogger(tag, message)
fun logWarning(tag: String, message: String) = warningLogger(tag, message)
fun logInfo(tag: String, message: String) = infoLogger(tag, message)
fun logDebug(tag: String, message: String) = debugLogger(tag, message)
fun logVerbose(tag: String, message: String) = verboseLogger(tag, message)
fun logComp(tag: String, message: String) = compLogger(tag, message)

