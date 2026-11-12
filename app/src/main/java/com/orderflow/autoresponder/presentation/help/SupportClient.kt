package com.orderflow.autoresponder.presentation.help

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.firebase.FirebaseNetworkException
import dagger.hilt.android.qualifiers.ApplicationContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

data class SupportTurn(val role: String, val text: String)
fun interface SupportGateway {
    suspend fun ask(turns: List<SupportTurn>): HelpReply
}

class SupportClient @Inject constructor(
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
    @ApplicationContext private val context: Context
) : SupportGateway {
    override suspend fun ask(turns: List<SupportTurn>): HelpReply {
        val requestId = java.util.UUID.randomUUID().toString()
        var stage = "connectivity"
        try {
            return withTimeout(30_000) {
                val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
                if (network?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) != true)
                    throw java.io.IOException("No internet connection")
                stage = "authentication"
                if (auth.currentUser == null) auth.signInAnonymously().await()
                stage = "id-token"
                val user = checkNotNull(auth.currentUser)
                val token = user.getIdToken(false).await()
                check(!token.token.isNullOrBlank())
                // Validate locally without printing the token, claims, UID or any customer data.
                if (token.claims["aud"] != auth.app.options.projectId) throw SupportProjectMismatchException()
                val provider = ((token.claims["firebase"] as? Map<*, *>)?.get("sign_in_provider") as? String)
                    ?.takeIf { it in setOf("anonymous", "password", "google.com", "custom", "phone") } ?: "other"
                android.util.Log.i("ChatzySupport", "request=$requestId stage=id-token-ready audienceMatches=true provider=$provider")
                stage = "callable"
                // Project comes from this build's Firebase options, never a guessed endpoint.
                android.util.Log.i("ChatzySupport", "request=$requestId stage=callable project=${auth.app.options.projectId} region=us-central1 route=chatzySupport")
                val data = functions.getHttpsCallable("chatzySupport").call(mapOf(
                    "requestId" to requestId,
                    "turns" to turns.map { mapOf("role" to it.role, "text" to it.text) }
                )).await().data as? Map<*, *> ?: error("Invalid support response")
                stage = "decode"
                decode(data).also { android.util.Log.i("ChatzySupport", "request=$requestId stage=complete") }
            }
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException && error !is kotlinx.coroutines.TimeoutCancellationException) throw error
            val code = diagnosticCode(error)
            // Never log exception text/stack, request body, credentials, UID or provider body.
            android.util.Log.w("ChatzySupport", "request=$requestId stage=$stage code=$code")
            throw SupportRequestException(error, requestId)
        }
    }

    companion object {
        fun boundedTurns(recent: List<SupportTurn>, question: String): List<SupportTurn> {
            var history = recent.takeLast(6)
            while (history.isNotEmpty()) {
                val turns = history + SupportTurn("user", question)
                val json = com.google.gson.Gson().toJson(mapOf("turns" to turns.map { mapOf("role" to it.role, "text" to it.text) }))
                if (turns.sumOf { it.text.length } <= 10000 && json.toByteArray(Charsets.UTF_8).size <= 23000) break
                history = history.drop(2)
            }
            return history + SupportTurn("user", question)
        }

        fun diagnosticCode(error: Exception): String = when (error) {
            is FirebaseFunctionsException -> "functions/${error.code.name}"
            is com.google.firebase.auth.FirebaseAuthException -> "auth/${error.errorCode}"
            is SupportProjectMismatchException -> "configuration/token-project-mismatch"
            is FirebaseNetworkException, is java.io.IOException -> "connection"
            is kotlinx.coroutines.TimeoutCancellationException -> "timeout"
            else -> "invalid-response"
        }

        fun decode(data: Map<*, *>): HelpReply {
            val text = data["text"] as? String ?: error("Missing support answer")
            require(text.isNotBlank() && text.length <= 2000)
            // Treat all server/model action IDs as untrusted. No arbitrary routes or URLs.
            val actions = (data["actions"] as? List<*>)?.mapNotNull {
                (it as? String)?.let(HelpAction::fromId)
            }?.distinct()?.take(1).orEmpty()
            return HelpReply(text, actions = actions)
        }

        fun failureMessage(failure: Exception): String {
            val error = (failure as? SupportRequestException)?.original ?: failure
            return when {
                error is SupportProjectMismatchException ->
                    "AI chat sign-in belongs to a different Firebase project. Contact your Chatzy administrator."
                error is com.google.firebase.auth.FirebaseAuthException && error.errorCode == "ERROR_ADMIN_RESTRICTED_OPERATION" ->
                    "AI chat account creation is restricted. Ask your Chatzy administrator to allow support account creation, then tap Retry."
                error is com.google.firebase.auth.FirebaseAuthException && error.errorCode == "ERROR_OPERATION_NOT_ALLOWED" ->
                    "Anonymous sign-in is disabled for this app's Firebase project. Ask your Chatzy administrator to enable it, then tap Retry."
                error is com.google.firebase.auth.FirebaseAuthException && error.errorCode == "ERROR_CONFIGURATION_NOT_FOUND" ->
                    "Firebase Authentication is not configured for this app's project. Contact your Chatzy administrator, then tap Retry."
                error is kotlinx.coroutines.TimeoutCancellationException || error is java.net.SocketTimeoutException ||
                    error is FirebaseFunctionsException && error.code == FirebaseFunctionsException.Code.DEADLINE_EXCEEDED ->
                    "The request timed out. Tap Retry to try this message again."
                error is FirebaseFunctionsException && error.code == FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED ->
                    "AI chat quota reached. Wait a little, then tap Retry."
                error is java.io.IOException || error is FirebaseNetworkException ->
                    "Connection failed. Check your internet connection, then tap Retry."
                error is FirebaseFunctionsException && error.code == FirebaseFunctionsException.Code.NOT_FOUND ->
                    "The AI support endpoint is not deployed for this app's Firebase project. Ask your Chatzy administrator to deploy Chatzy Help, then tap Retry."
                error is FirebaseFunctionsException && error.code == FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
                    "AI chat is not configured on the server. Contact your Chatzy administrator, then tap Retry once it is available."
                error is FirebaseFunctionsException && error.code in setOf(FirebaseFunctionsException.Code.UNAUTHENTICATED, FirebaseFunctionsException.Code.PERMISSION_DENIED) ||
                    error is com.google.firebase.auth.FirebaseAuthException ->
                    "AI chat could not authenticate. Reopen Chatzy and retry. If this continues, contact your administrator."
                else -> "AI chat service is unavailable. Please try Retry later."
            }
        }
    }
}

class SupportRequestException(val original: Exception, val requestId: String) : Exception("Support request failed ($requestId)")
class SupportProjectMismatchException : Exception("Support token project mismatch")
