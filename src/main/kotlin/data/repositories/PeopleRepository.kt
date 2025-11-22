package data.repositories

import data.IDataStore
import domain.IPersonRepository
import domain.entities.Person
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class PersonRepository(
   private val _dataStore: IDataStore
) : IPersonRepository {

   override fun getAllSorted(): Flow<Result<List<Person>>> =
// _dataStore.selectAllSorted().asResult()
   _dataStore.selectAllSorted()
      .map { it -> Result.success(it) }
      .catch { e ->
          if (e is CancellationException) throw e
          emit(Result.failure(e))
      }

   override suspend fun findById(id: String): Result<Person?> =
//    try { Result.success( _dataStore.findById(id) ) }
//    catch (e: CancellationException) { throw e }
//    catch (e: Exception) { Result.failure(e) }
      tryCatching { _dataStore.findById(id)  }

   override suspend fun create(person: Person): Result<Unit> =
      tryCatching { _dataStore.insert(person) }

   override suspend fun update(person: Person): Result<Unit> =
      tryCatching { _dataStore.update(person) }

   override suspend fun remove(person: Person): Result<Unit> =
      tryCatching { _dataStore.delete(person) }

}
