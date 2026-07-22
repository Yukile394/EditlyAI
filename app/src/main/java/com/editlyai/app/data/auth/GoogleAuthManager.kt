package com.editlyai.app.data.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Google Sign-In + Firebase Auth entegrasyonu.
 *
 * KURULUM:
 * 1. Firebase Console'da projenizi oluşturup `google-services.json` dosyasını
 *    indirip app/ klasörüne koyun (bu repo'da bir PLACEHOLDER dosya var,
 *    kendi projenizinkiyle değiştirmeden Google Sign-In ÇALIŞMAZ).
 * 2. Firebase Console > Authentication > Sign-in method > Google'ı etkinleştirin.
 * 3. `default_web_client_id` otomatik olarak google-services.json'dan gelir.
 *
 * Hesap değiştiğinde: signIn() her çağrıldığında güncel hesabı döner;
 * kullanıcı hesabı değiştirirse Firebase UID değişir, bu durumda
 * UserRepository sunucudaki (Firestore) o UID'ye bağlı abonelik/hak
 * verisini tekrar çeker (bkz. README -> "Hesap değişince senkronizasyon").
 */
class GoogleAuthManager(private val context: Context) {

    private val googleSignInClient: GoogleSignInClient

    init {
        val webClientId = context.getString(
            context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        )
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    fun getLastSignedInAccount(): GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(context)

    suspend fun handleSignInResult(task: Task<GoogleSignInAccount>): Result<String> {
        return try {
            val account = task.await()
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val authResult = FirebaseAuth.getInstance().signInWithCredential(credential).await()
            Result.success(authResult.user?.uid.orEmpty())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        FirebaseAuth.getInstance().signOut()
        googleSignInClient.signOut()
    }

    fun currentUserId(): String? = FirebaseAuth.getInstance().currentUser?.uid
}
