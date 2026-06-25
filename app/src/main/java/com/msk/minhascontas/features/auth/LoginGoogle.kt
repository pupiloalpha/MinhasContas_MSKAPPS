package com.msk.minhascontas.features.auth

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.msk.minhascontas.R
import com.msk.minhascontas.ui.theme.MinhasContasTheme
import java.security.MessageDigest
import java.util.UUID

/**
 * Atividade responsável por realizar o login com o Google utilizando o Credential Manager.
 * Utiliza o fluxo moderno recomendado pelo Google e integra com Firebase Auth.
 */
class LoginGoogle : ComponentActivity() {

    companion object {
        private const val TAG = "LoginGoogle"
    }

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MinhasContasTheme {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                LaunchedEffect(Unit) {
                    performLogin()
                }
            }
        }
    }

    private suspend fun performLogin() {
        val credentialManager = CredentialManager.create(this)

        val webClientId = getString(R.string.default_web_client_id)
        if (webClientId.isEmpty()) {
            Log.e(TAG, "Web Client ID não configurado!")
            handleError(getString(R.string.google_sign_in_error))
            return
        }

        // Geração de nonce para segurança (recomendado pelo Google para Credential Manager)
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // Mostra todas as contas se o auto-select falhar
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true) // Login automático se houver apenas uma conta autorizada
            .setNonce(hashedNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        try {
            val result = credentialManager.getCredential(this, request)
            val credential = result.credential

            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                auth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "Firebase Auth: Sucesso")
                            Toast.makeText(applicationContext, R.string.google_sign_in_success, Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        } else {
                            Log.e(TAG, "Firebase Auth: Falha", task.exception)
                            handleError(getString(R.string.google_sign_in_error), task.exception)
                        }
                    }
            } else {
                Log.e(TAG, "Tipo de credencial inesperado: ${credential.type}")
                handleError(getString(R.string.google_sign_in_error))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i(TAG, "Login cancelado pelo usuário")
            finish()
        } catch (e: NoCredentialException) {
            Log.e(TAG, "Nenhuma credencial disponível: ${e.message}")
            handleError(getString(R.string.google_sign_in_error_no_account), e)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Erro no Credential Manager: ${e.type}", e)
            handleError(getString(R.string.google_sign_in_error), e)
        } catch (e: Exception) {
            Log.e(TAG, "Erro inesperado no login", e)
            handleError(getString(R.string.google_sign_in_error), e)
        }
    }

    private fun handleError(message: String, exception: Exception? = null) {
        val errorMessage = exception?.let { "$message (${it.message})" } ?: message
        Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_LONG).show()
        finish()
    }
}
