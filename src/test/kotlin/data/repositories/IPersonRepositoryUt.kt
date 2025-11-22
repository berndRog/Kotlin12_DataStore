package data.repositories

import Globals
import MainDispatcherRule
import app.cash.turbine.test
import data.IDataStore
import data.local.Seed
import di.defModulesTest
import domain.IPersonRepository
import domain.entities.Person
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.context.GlobalContext.stopKoin
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.*

class IPersonRepositoryUt {

   @get:Rule
   val tempDir = TemporaryFolder()
   @get:Rule
   val mainRule = MainDispatcherRule()

   private lateinit var _seed: Seed
   private lateinit var _dataStore: IDataStore
   private lateinit var _repository: IPersonRepository
   private lateinit var _filePath: Path
   private lateinit var _seedPeople: List<Person>

   @Before
   fun setup() = runTest {
      // no logging during testing
      Globals.isInfo = false
      Globals.isDebug = false
      Globals.isVerbose = false

      stopKoin() // falls von anderen Tests übrig
      val testModule = defModulesTest(
         appHomePath = tempDir.root.absolutePath,
         ioDispatcher = mainRule.dispatcher() // StandardTestDispatcher als IO
      )
      val koinApp = startKoin { modules(testModule) }
      val koin = koinApp.koin
      _seed = koin.get<Seed>()
      _dataStore = koin.get<IDataStore>()
      _repository = koin.get<IPersonRepository>()

      // store filepath
      _filePath = _dataStore.filePath
      Files.deleteIfExists(_filePath)

      // read people into dataStore
      _dataStore.initialize()

      // expected
      _seedPeople = _seed.people
   }

   @After
   fun tearDown() {
      try {
         Files.delete(_filePath.fileName)
      }
      catch (_e: IOException) {
      }
      finally {
         stopKoin()
      }
   }

   @Test
   fun getAllSortByUt_ok() = runTest {
      // arrange
      val expected = _seedPeople.sortedBy { it.lastName.lowercase() }
      // act / assert
      _repository.getAllSorted().test {
         awaitItem()
            .onSuccess { assertContentEquals(expected, it.toMutableList()) }
            .onFailure { fail(it.message) }
      }
   }

   @Test
   fun insert_emitsUpdateFlow() = runTest {
      // arrange
      val newPerson = Person(
         "Bernd", "Rogalla", "b-u.rogalla@ostfalia.de", null,
         "00090001-0000-0000-0000-000000000001"
      )

      // act / assert: subscribe to flow, perform insert, expect another emission containing the new person
      _repository.getAllSorted().test {
         // consume initial emission
         val initial = awaitItem()
         initial.onFailure { fail(it.message) }
         initial.onSuccess { result ->
            assertTrue(result.size == 26)
         }

         // perform insert and assert success
         _repository.create(newPerson)
            .onFailure { fail(it.message) }
            .onSuccess { assertEquals(Unit, it) }

         // await next emission and assert it contains the inserted person
         val updated = awaitItem()
         updated.onFailure { fail(it.message) }
         updated.onSuccess { result ->
            assertTrue(result.any { p -> p.id == newPerson.id && p == newPerson }, "Inserted person not present in emitted list")
         }

         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun findByIdUt_ok() = runTest {
      // arrange
      val id = "01000000-0000-0000-0000-000000000000"
      val expected = _seedPeople.firstOrNull { person -> person.id == id  }
      assertNotNull(expected)
      // act / assert
      _repository.findById(id)
         .onSuccess { assertEquals(expected, it)  }
         .onFailure { fail(it.message) }
   }

   @Test
   fun insertUt_ok() = runTest {
      // arrange
      val person = Person(
         "Bernd", "Rogalla", "b-u.rogalla@ostfalia.de", null,
         id = "00090001-0000-0000-0000-000000000000")
      // act
      _repository.create(person)
         .onSuccess { assertEquals(Unit, it) }
         .onFailure { fail(it.message) }
      // assert
      _repository.findById(person.id)
         .onSuccess { assertEquals(person, it) }
         .onFailure { fail(it.message) }
   }

   @Test
   fun updateUt_ok() = runTest {
      // arrange
      val id = "01000000-0000-0000-0000-000000000000"
      var person: Person? = null
      _repository.findById(id)
         .onSuccess { person = it }
         .onFailure { t -> fail(t.message) }
      assertNotNull(person)
      // act
      val updated = person.copy(lastName ="Albers", email = "a.albers@gmx.de")
      _repository.update(updated)
         .onSuccess { assertEquals(Unit, it) }
         .onFailure { fail(it.message) }
      // assert
      _repository.findById(person.id)
         .onSuccess { assertEquals(updated, it) }
         .onFailure { fail(it.message) }
   }

   @Test
   fun deleteUt_ok() = runTest {
      // arrange
      val id = "01000000-0000-0000-0000-000000000000"
      val person = _dataStore.findById(id)
      assertNotNull(person)
      // act
      _repository.remove(person)
         .onSuccess { assertEquals(Unit, it) }
         .onFailure { fail(it.message) }
      // assert
      _repository.findById(person.id)
         .onSuccess { actual -> assertNull(actual) }
         .onFailure { fail(it.message) }
   }
}