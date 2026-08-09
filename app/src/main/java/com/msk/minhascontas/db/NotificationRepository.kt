package com.msk.minhascontas.db

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repositório especializado na gestão de notificações e alertas.
 */
class NotificationRepository private constructor(context: Context) {

    private val appDatabase: AppDatabase = AppDatabase.getDatabase(context)

    companion object {
        @Volatile
        private var instance: NotificationRepository? = null

        fun getInstance(context: Context): NotificationRepository {
            return instance ?: synchronized(this) {
                instance ?: NotificationRepository(context).also { instance = it }
            }
        }
    }

    /**
     * Realiza a limpeza de notificações antigas no Room.
     */
    suspend fun limparNotificacoesAntigas() = withContext(Dispatchers.IO) {
        val agora = System.currentTimeMillis()
        // Remove lidas com mais de 30 dias
        appDatabase.notificationDao().deleteOldRead(agora - (30L * 24 * 60 * 60 * 1000))
        // Remove não lidas com mais de 90 dias
        appDatabase.notificationDao().deleteVeryOld(agora - (90L * 24 * 60 * 60 * 1000))
    }

    /**
     * Verifica se existem notificações não lidas no Room.
     */
    suspend fun temNotificacoesNaoLidas(): Boolean = withContext(Dispatchers.IO) {
        appDatabase.notificationDao().countUnread() > 0
    }

    /**
     * Busca notificações não lidas no Room.
     */
    suspend fun getNotificacoesNaoLidas(): List<Notificacao> = withContext(Dispatchers.IO) {
        appDatabase.notificationDao().getUnreadSync()
    }

    /**
     * Marca uma notificação específica como lida no Room.
     */
    suspend fun marcarNotificacaoLida(id: Long) = withContext(Dispatchers.IO) {
        appDatabase.notificationDao().markAsRead(id)
    }

    /**
     * Marca todas as notificações como lidas no Room.
     */
    suspend fun marcarTodasNotificacoesLidas() = withContext(Dispatchers.IO) {
        appDatabase.notificationDao().markAllAsRead()
    }

    /**
     * Adiciona uma nova notificação no Room.
     */
    suspend fun addNotificacao(titulo: String, mensagem: String, tipo: String? = null) {
        withContext(Dispatchers.IO) {
            val duplicada = appDatabase.notificationDao().findDuplicateToday(titulo, mensagem)
            if (duplicada == null) {
                appDatabase.notificationDao().insert(Notificacao(titulo = titulo, mensagem = mensagem, tipo = tipo))
            }
        }
    }
}
