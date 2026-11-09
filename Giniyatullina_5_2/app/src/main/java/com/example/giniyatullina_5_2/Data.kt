package com.example.giniyatullina_5_2

import android.content.Context
import android.content.SharedPreferences
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class User(val name: String, val password: String? = null)

class AuthManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveUser(user: User) {
        prefs.edit().putString("user", gson.toJson(user)).apply()
    }

    fun getUser(): User? {
        val json = prefs.getString("user", null) ?: return null
        return try {
            gson.fromJson(json, User::class.java)
        } catch (e: Exception) {
            null
        }
    }
}

data class WikiOnThisDay(
    val events: List<WikiEvent>? = null
)

data class WikiEvent(
    val year: Int? = null,
    val text: String? = null
)

class FactsPrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("facts_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveFacts(facts: List<FactEntity>) {
        val saved = loadFacts().toMutableList()
        val texts = saved.map { it.text }.toMutableSet()
        facts.forEach { if (texts.add(it.text)) saved.add(it) }
        prefs.edit().putString(KEY_FACTS, gson.toJson(saved)).apply()
    }

    fun loadFacts(): List<FactEntity> {
        val json = prefs.getString(KEY_FACTS, null) ?: return emptyList()
        return try {
            gson.fromJson<List<FactEntity>>(
                json,
                object : TypeToken<List<FactEntity>>() {}.type
            ) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val KEY_FACTS = "facts_json"
    }
}

@Entity(tableName = "facts")
data class FactEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val year: String,
    val text: String,
    val isFavorite: Boolean = false
)

@Dao
interface FactDao {
    @Query("SELECT * FROM facts ORDER BY id DESC")
    fun getAll(): List<FactEntity>

    @Query("SELECT * FROM facts WHERE isFavorite = 1 ORDER BY id DESC")
    fun getFavorites(): List<FactEntity>

    @Query("SELECT * FROM facts WHERE id IN (:ids) ORDER BY id DESC")
    fun getByIds(ids: List<Int>): List<FactEntity>

    @Insert
    fun insert(fact: FactEntity): Long

    @Update
    fun update(fact: FactEntity)

    @Delete
    fun delete(fact: FactEntity)
}

@Database(entities = [FactEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): FactDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE facts ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "facts.db")
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
    }
}
