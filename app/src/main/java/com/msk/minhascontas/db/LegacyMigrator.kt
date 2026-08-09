package com.msk.minhascontas.db

import android.util.Log

/**
 * Responsável por migrar dados do banco de dados SQLite legado para o Room.
 */
class LegacyMigrator(
    private val dbContas: DBContas,
    private val appDatabase: AppDatabase
) {
    companion object {
        private const val TAG = "LegacyMigrator"
    }

    /**
     * Executa a migração se o Room estiver vazio.
     */
    suspend fun checkAndMigrate() {
        try {
            // 1. Migração de Contas
            val countRoom = appDatabase.contaDao().count()
            if (countRoom == 0) {
                val countLegacy = dbContas.quantasContas()
                if (countLegacy > 0) {
                    Log.i(TAG, "Detectado banco legado com $countLegacy contas. Migrando para Room...")
                    val legacyContas = dbContas.getAllContasDetalhado()
                    if (legacyContas.isNotEmpty()) {
                        appDatabase.contaDao().insertAll(legacyContas)
                    }
                }
            }

            // 2. Migração de Notificações
            val countNotifications = appDatabase.notificationDao().countUnread()
            if (countNotifications == 0) {
                val legacyDb = dbContas.database
                if (legacyDb != null && legacyDb.isOpen) {
                    try {
                        legacyDb.query(ContasContract.Notificacoes.TABELA_NOME, null, null, null, null, null, null).use { c ->
                            if (c != null && c.count > 0) {
                                val list = mutableListOf<Notificacao>()
                                while (c.moveToNext()) {
                                    list.add(Notificacao(
                                        titulo = c.getString(c.getColumnIndexOrThrow(ContasContract.Notificacoes.COLUNA_TITULO)),
                                        mensagem = c.getString(c.getColumnIndexOrThrow(ContasContract.Notificacoes.COLUNA_MENSAGEM)),
                                        dataCriacao = c.getLong(c.getColumnIndexOrThrow(ContasContract.Notificacoes.COLUNA_DATA)),
                                        lida = c.getInt(c.getColumnIndexOrThrow(ContasContract.Notificacoes.COLUNA_LIDA)) == 1,
                                        tipo = c.getString(c.getColumnIndexOrThrow(ContasContract.Notificacoes.COLUNA_TIPO))
                                    ))
                                }
                                appDatabase.notificationDao().insertAll(list)
                                Log.i(TAG, "Migração de ${list.size} notificações concluída.")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Tabela de notificações não encontrada ou erro na leitura do legado: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro durante a migração do legado para Room", e)
        }
    }
}
