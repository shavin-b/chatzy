package com.orderflow.autoresponder.device.auth

import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctionsException
import com.orderflow.autoresponder.core.logger.StructuredLogger

/** Never include exception messages, payloads or tokens in registration diagnostics. */
object DeviceRegistrationDiagnostics {
    fun failure(operation: String, deviceId: String, error: Throwable) {
        val code = when (error) {
            is FirebaseFirestoreException -> "firestore/${error.code.name}"
            is FirebaseFunctionsException -> "functions/${error.code.name}"
            is FirebaseAuthException -> "auth/${error.errorCode}"
            else -> error.javaClass.simpleName
        }
        StructuredLogger.e("DeviceRegistration", "operation=$operation path=devices/$deviceId code=$code")
    }
}
