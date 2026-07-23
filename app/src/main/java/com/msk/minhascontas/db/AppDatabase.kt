package com.msk.minhascontas.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Banco de dados Room para a aplicação.
 * Atualmente contém apenas a entidade Conta, mas está estruturado para expansão.
 */
@Database(entities = [Conta::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contaDao(): ContaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Retorna a instância única do banco de dados (Singleton).
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "minhas_contas_room_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Fecha a instância do banco de dados e limpa o Singleton.
         */
        fun closeDatabase() {
            try {
                INSTANCE?.close()
            } catch (e: Exception) {
                // Silently handle close errors
            } finally {
                INSTANCE = null
            }
        }
    }
}
