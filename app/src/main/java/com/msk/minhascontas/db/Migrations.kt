package com.msk.minhascontas.db

/*
Migrations.kt

O que é
- Objeto que agrupa migrações do schema para Room. Contém MIGRATION_4_5
  (um Migration 4→5) configurada como "no-op".

Por que existe
- O DB legado, gerenciado por SQLiteOpenHelper em DBContas.java, usava versão 4.
  Ao migrar para Room definimos a versão do Room para 5. Para evitar que Room
  tente recriar/destruir o banco ao detectar diferença de versão, adicionamos
  uma migração 4→5 que, no primeiro momento, não altera estrutura — apenas
  permite que Room abra o arquivo existente sem perda de dados.

Como estender
- Se futuramente quiser alterar a tabela (adicionar coluna, renomear, etc.),
  implemente statements SQL dentro do corpo de migrate(database: SupportSQLiteDatabase),
  testando a migração em backups:
    val MIGRATION_5_6 = object : Migration(5, 6) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE contas ADD COLUMN nova_coluna INTEGER DEFAULT 0 NOT NULL")
      }
    }
  e adicione .addMigrations(MIGRATION_4_5, MIGRATION_5_6) ao Room.databaseBuilder.

Cuidados
- Antes de modificar o schema, crie testes de migração em um ambiente controlado
  com uma cópia do DB legado para garantir que nenhuma informação seja perdida.
- Se a estrutura antiga contiver colunas com nomes diferentes/incertos, escreva
  migrações que mapeiem/transformem dados explicitamente (criar tabela temporária,
  copiar, dropar e renomear).
*/

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    // Keep a no-op migration from version 4 (legacy) to 5 (Room)
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // No-op: we intentionally keep the existing 'contas' table structure as-is so Room can read it.
            // If future schema changes are needed, implement the alter table statements here.
        }
    }
}
