package data.local.datastore

import Globals
import data.IDataStore
import data.local.Seed
import domain.entities.Person
import domain.utilities.logDebug
import domain.utilities.logError
import domain.utilities.logVerbose
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class DataStore(
   appHomeName: String?,
   directoryName: String?,
   fileName: String?,
   private val _seed: Seed? = null,
   private val _dispatcherDefault: CoroutineDispatcher = Dispatchers.Default,
   private val _dispatcherIO: CoroutineDispatcher = Dispatchers.IO
) : IDataStore {

   private val _appHome = appHomeName ?: System.getProperty("user.home") // context.filesDir on Android
   private var _directoryName = directoryName ?: Globals.directory_name
   private val _fileName = fileName ?: Globals.file_name

   override val filePath: Path = getOrCreateFilePath(
      appHome = _appHome,
      directoryName = _directoryName,
      fileName = _fileName
   )

   // Coroutine-safe locking
   private val _mutex = Mutex()

   // Reactive in-memory state
   private val _peopleFlow = MutableStateFlow<List<Person>>(emptyList())
   val peopleFlow: StateFlow<List<Person>> = _peopleFlow.asStateFlow()

   // JSON serializer
   private val _jsonSettings = Json {
      prettyPrint = true
      ignoreUnknownKeys = true
   }

   // Reactive selects
   override fun selectAllSorted(): Flow<List<Person>> =
      peopleFlow
         .map { list:List<Person> -> list.sortedBy { it: Person -> it.lastName.lowercase() } }
         .distinctUntilChanged()

   // one shot read
   override suspend fun findById(id: String): Person? =
      peopleFlow.value
         .firstOrNull {
            //throw Exception("Testing error handling in findById()")
            it.id == id
         }

   // one show write
   override suspend fun insert(person: Person) {
      _mutex.withLock {
         // snapshot current value
         val current: List<Person> = _peopleFlow.value
         if (current.any { it.id == person.id }) return
         logVerbose(TAG, "insert: $person")
//         val updated: List<Person> = current.toMutableList()
//            .apply { add(person) }
//            .toList()
         val updated = current + person // copy current and add person
         writeInternal(updated)
         _peopleFlow.value = updated
      }
   }

   override suspend fun update(person: Person) {
      _mutex.withLock {
         val current = _peopleFlow.value
         require(current.any { it.id == person.id }) { "Person with id ${person.id} does not exist" }
         logVerbose(TAG, "update: $person")
         val updated = current.map { if (it.id == person.id) person else it }
         writeInternal(updated)
         _peopleFlow.value = updated
      }
   }

   override suspend fun delete(person: Person) {
      _mutex.withLock {
         val current = _peopleFlow.value
         require(current.any { it.id == person.id }) { "Person with id ${person.id} does not exist" }
         logVerbose(TAG, "delete: $person")
         val updated = current.filterNot { it.id == person.id }
         writeInternal(updated)
         _peopleFlow.value = updated
      }
   }

   // Initialize: load from file or seed
   override suspend fun initialize() {
      logDebug(TAG, "init: read datastore")
      _mutex.withLock {
         val exists = Files.exists(filePath)
         val size = if (exists) runCatching {
            Files.size(filePath)
         }.getOrElse { 0L } else 0L

         if (!exists || size == 0L) {
            _seed?.let { seed ->
               logVerbose(TAG, "create(): seedData ${seed.people.size} people")
               writeInternal(seed.people)
            } ?: run {
               // no seed provided → start with empty file
               writeInternal(emptyList())
            }
         }
         _peopleFlow.value = readInternal()
      }
   }

   // ---------- Internal I/O (always on IO dispatcher) ----------
   // coroutine are propagating exceptions automatically: no try-catch needed here
   // exceptions are caught in the repository layer
   private suspend fun readInternal(): List<Person> = withContext(_dispatcherIO) {
      // Check file existence and size
      if (!Files.exists(filePath)) return@withContext emptyList()
      val file = File(filePath.toString())
      if (file.length() == 0L) return@withContext emptyList()

      // Read text from file
      val jsonString =  File(filePath.toString()).readText()

      // Deserialize JSON to people list
      val people: List<Person> = if (jsonString.isBlank()) {
         emptyList()
      } else {
         _jsonSettings.decodeFromString(jsonString)
      }

      logDebug(TAG, "read(): decode JSON ${people.size} people")
      return@withContext people
   }

   private suspend fun writeInternal(people: List<Person>) = withContext(_dispatcherIO) {

      // Serialize people list to JSON
      val jsonString = _jsonSettings.encodeToString(people)
      logDebug(TAG, "write(): encode JSON ${people.size} people")

      // Ensure directory exists
      val dir = File(filePath.parent.toString())
      if (!dir.exists()) dir.mkdirs()

      // Atomic write via temp file
      val targetFile = File(filePath.toString())
      val tmpFile = File(targetFile.parent, "${targetFile.name}.tmp")
      tmpFile.writeText(jsonString)
      if (!tmpFile.renameTo(targetFile)) {
         tmpFile.copyTo(targetFile, overwrite = true)
         tmpFile.delete()
      }
      logVerbose(TAG, jsonString)
   }

   // Just to illustrate using of coroutineScope and async compression/decompression
   // and different dispatchers
   private suspend fun readInternal1(): List<Person> = coroutineScope {
      // Check file existence and size
      if (!Files.exists(filePath)) return@coroutineScope emptyList()
      val file = File(filePath.toString())
      if (file.length() == 0L) return@coroutineScope emptyList()

      // Read  bytes from file
      val bytes = withContext(_dispatcherIO) {
         logDebug(TAG, "readBytes()")
         file.readBytes()
      }

      // Async Decompress with GZip if needed
      val deferredJsonBytes: Deferred<ByteArray> = async(_dispatcherDefault) {
         logDebug(TAG, "gunzip()")
         if (isGzip(bytes)) gunzip(bytes)
         else bytes
      }
      // Async Decode JSON
      val deferredJsonString: Deferred<String> = async(_dispatcherDefault) {
         logDebug(TAG, "decodeToString()")
         val jsonBytes: ByteArray = deferredJsonBytes.await() // await decompression
         jsonBytes.decodeToString()
      }

      // Deserialize JSON to people list
      val deferredPeople:Deferred<List<Person>> = async {
         logDebug(TAG, "decodeFromString()")
         val jsonString: String = deferredJsonString.await()
         logVerbose(TAG, jsonString)
         if (jsonString.isBlank()) emptyList()
         else _jsonSettings.decodeFromString(jsonString)
      }
      val people = deferredPeople.await()

      logDebug(TAG, "read(): decode JSON ${people.size} people")
      return@coroutineScope people
   }

   private suspend fun writeInternal2(people: List<Person>) = coroutineScope {

      // 1) JSON-Encoding (CPU bound: use DefaultDispatcher)
      val jsonDeferred: Deferred<String> = async(_dispatcherDefault) {
         logDebug(TAG, "encodeToString()")
         _jsonSettings.encodeToString(people)                // CPU
      }

      // 2) Compression (CPU-bound: use Dispatcher.Default)
      var jsonString:String = ""
      val compressedDeferred: Deferred<ByteArray> = async(_dispatcherDefault) {
         logDebug(TAG, "gzip()")
         jsonString = jsonDeferred.await()
         gzip(jsonString.encodeToByteArray())                       // CPU
      }

      // 3) Atomic Write (switch to IO-Dispatcher)
      withContext(_dispatcherIO) {
         val gzBytes: ByteArray = compressedDeferred.await()
         logDebug(TAG, "writeBytes()")
         val dir = File(filePath.parent.toString()).apply { if (!exists()) mkdirs() }
         val target = File(filePath.toString())
         val tmp = File(target.parent, "${target.name}.tmp")

         tmp.outputStream().use { out ->
            out.write(gzBytes)
            out.flush()
         }
         if (!tmp.renameTo(target)) { // Fallback, wenn rename nicht atomar
            tmp.copyTo(target, overwrite = true);
            tmp.delete()
         }
      }
      logVerbose(TAG, jsonString)
   }

   private fun gzip(data: ByteArray): ByteArray {
      val byteArrayOutputStream = ByteArrayOutputStream()
      GZIPOutputStream(byteArrayOutputStream).use { it.write(data) }
      return byteArrayOutputStream.toByteArray()
   }

   private fun isGzip(data: ByteArray): Boolean =
      data.size >= 2 && data[0] == 0x1f.toByte() && data[1] == 0x8b.toByte()

   private fun gunzip(data: ByteArray): ByteArray {
      ByteArrayInputStream(data).use { bis ->
         GZIPInputStream(bis).use { gis ->
            val buf = ByteArray(8 * 1024)
            val out = ByteArrayOutputStream()
            while (true) {
               val n = gis.read(buf)
               if (n <= 0) break
               out.write(buf, 0, n)
            }
            return out.toByteArray()
         }
      }
   }

   companion object {
      private const val TAG = "<-DataStore>"

      // Build (and ensure) platform-friendly path like:
      // <UserHome>/Documents/<directoryName>/<fileName>
      fun getOrCreateFilePath(
         appHome: String,
         directoryName: String,
         fileName: String
      ): Path {
         try {
            val dir: Path = Paths.get(appHome)
               .resolve("Documents")
               .resolve(directoryName)
            Files.createDirectories(dir)
            return dir.resolve(fileName)
         } catch (e: Exception) {
            logError(TAG, "Failed to create directory or build path: ${e.message}")
            throw e
         }
      }
   }
}