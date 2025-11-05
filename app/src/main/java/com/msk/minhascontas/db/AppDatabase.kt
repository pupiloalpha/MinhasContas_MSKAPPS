package com.msk.minhascontas.db

/*
AppDatabase.kt

O que é
- Classe abstrata que estende RoomDatabase e representa a base de dados da aplicação.
- Define as entidades (Conta) e fornece o método para obter o DAO (contaDao()).
- Implementa um singleton thread-safe para acesso global.

Configuração importante
- name: "minhas_contas" — o mesmo nome usado pelo DB antigo (SQLiteOpenHelper).
  É crítico que esse nome corresponda ao arquivo de banco de dados real do app
  (verificar se a instalação tem extensão .db; ajustar se necessário).
- version: 5 — definimos uma versão superior à anterior (4) e incluímos
  MIGRATION_4_5 como no-op para permitir Room abrir o DB legado.

Como obter uma instância
- AppDatabase.getInstance(context) — retorna o singleton.
- Usar este instance para obter DAO:
    val dao = AppDatabase.getInstance(context).contaDao()

Observações de uso e segurança
- A instancia atual é mantida em memória (INSTANCE). Em cenários de import/export
  do DB você deve fechar e resetar a instância antes de substituir o arquivo do DB.
  Recomenda-se adicionar (se necessário) um método closeAndReset() no companion que:
    INSTANCE?.close()
    INSTANCE = null
  para permitir reabrir o banco depois de uma importação.
- As operações dos DAOs aqui são síncronas. Para usá-las sem bloquear a UI,
  execute em coroutines (Dispatcher.IO) ou executors.
- Em caso de mudanças de schema futuras, implemente migrações em Migrations.kt
  e adicione-as ao builder do Room.
*/

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Conta::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contaDao(): ContaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "minhas_contas"
                )
                    .addMigrations(Migrations.MIGRATION_4_5)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
