package com.example.giniyatullina_5_2

import android.content.Context
import android.content.SharedPreferences
import androidx.room.*
import com.google.gson.Gson

// ---------- Пользователь (SharedPreferences + JSON) ----------
data class User(val name: String)

class AuthManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveUser(user: User) {
        prefs.edit().putString("user", gson.toJson(user)).apply()
    }

    fun getUser(): User? {
        val json = prefs.getString("user", null) ?: return null
        return gson.fromJson(json, User::class.java)
    }

    fun isLoggedIn() = getUser() != null
}

// ---------- Room ----------
@Entity(tableName = "facts")
data class FactEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val year: String,
    val text: String
)

@Dao
interface FactDao {
    @Query("SELECT * FROM facts ORDER BY id DESC")
    fun getAll(): List<FactEntity>

    @Insert
    fun insert(fact: FactEntity)

    @Update
    fun update(fact: FactEntity)

    @Delete
    fun delete(fact: FactEntity)
}

@Database(entities = [FactEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): FactDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "facts.db")
                    .build().also { INSTANCE = it }
            }
    }
}