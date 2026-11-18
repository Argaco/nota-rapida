package com.notarapida.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    @ColumnInfo(defaultValue = "0") val pinned: Boolean = false,
)

@Dao
interface NoteDao {
    /** Los fijados primero y, dentro de cada grupo, el más reciente primero. */
    @Query("SELECT * FROM notes WHERE text LIKE '%' || :query || '%' ORDER BY pinned DESC, id DESC")
    fun observe(query: String): Flow<List<Note>>

    @Query("SELECT * FROM notes ORDER BY pinned DESC, id DESC")
    suspend fun getAll(): List<Note>

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)
}

@Database(entities = [Note::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        /** Versión 2: se añade la marca de "fijado". Los apuntes existentes quedan sin fijar. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notas.db",
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}

/**
 * Guarda apuntes desde fuera de la pantalla principal (por ejemplo, la nota rápida).
 * Usa su propio alcance para que el guardado termine aunque la pantalla se cierre.
 */
object NoteStore {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun add(context: Context, text: String) {
        val dao = AppDatabase.get(context).noteDao()
        scope.launch { dao.insert(Note(text = text)) }
    }
}
