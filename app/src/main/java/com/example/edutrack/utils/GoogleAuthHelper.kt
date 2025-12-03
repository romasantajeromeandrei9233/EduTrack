package com.example.edutrack.utils

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

object GoogleAuthHelper {

    private const val TAG = "GoogleAuthHelper"
    private const val SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
    private const val TOKEN_URL = "https://oauth2.googleapis.com/token"

    private var cachedAccessToken: String? = null
    private var tokenExpiryTime: Long = 0

    /**
     * Get OAuth 2.0 Access Token for FCM v1 API
     * Caches token until expiry
     */
    fun getAccessToken(context: Context): String? {
        // Return cached token if still valid
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpiryTime) {
            Log.d(TAG, "✅ Using cached access token")
            return cachedAccessToken
        }

        try {
            // 1. Read service account JSON from assets
            val serviceAccountJson = context.assets.open("service-account.json")
                .bufferedReader()
                .use { it.readText() }

            val jsonObject = JSONObject(serviceAccountJson)
            val privateKeyPem = jsonObject.getString("private_key")
            val clientEmail = jsonObject.getString("client_email")
            val projectId = jsonObject.getString("project_id")

            Log.d(TAG, "📧 Service Account: $clientEmail")
            Log.d(TAG, "🆔 Project ID: $projectId")

            // 2. Create JWT
            val jwt = createJWT(clientEmail, privateKeyPem)

            // 3. Exchange JWT for Access Token
            val accessToken = exchangeJWTForAccessToken(jwt)

            if (accessToken != null) {
                cachedAccessToken = accessToken
                tokenExpiryTime = System.currentTimeMillis() + (55 * 60 * 1000) // 55 minutes
                Log.d(TAG, "✅ Access token obtained successfully")
            }

            return accessToken

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get access token: ${e.message}", e)
            return null
        }
    }

    /**
     * Create JWT (JSON Web Token) for Google OAuth
     */
    private fun createJWT(clientEmail: String, privateKeyPem: String): String {
        val now = System.currentTimeMillis() / 1000
        val expiry = now + 3600 // 1 hour

        // JWT Header
        val header = JSONObject().apply {
            put("alg", "RS256")
            put("typ", "JWT")
        }

        // JWT Payload (Claims)
        val payload = JSONObject().apply {
            put("iss", clientEmail)
            put("scope", SCOPE)
            put("aud", TOKEN_URL)
            put("iat", now)
            put("exp", expiry)
        }

        // Encode header and payload
        val encodedHeader = base64UrlEncode(header.toString().toByteArray())
        val encodedPayload = base64UrlEncode(payload.toString().toByteArray())

        // Create signature
        val signatureInput = "$encodedHeader.$encodedPayload"
        val signature = signRSA256(signatureInput, privateKeyPem)

        // Return complete JWT
        return "$signatureInput.$signature"
    }

    /**
     * Sign data with RSA-SHA256
     */
    private fun signRSA256(data: String, privateKeyPem: String): String {
        try {
            // Remove PEM header/footer and whitespace
            val privateKeyContent = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\s+".toRegex(), "")

            // Decode Base64 private key
            val keyBytes = Base64.getDecoder().decode(privateKeyContent)
            val keySpec = PKCS8EncodedKeySpec(keyBytes)
            val keyFactory = KeyFactory.getInstance("RSA")
            val privateKey: PrivateKey = keyFactory.generatePrivate(keySpec)

            // Sign with RSA-SHA256
            val signature = Signature.getInstance("SHA256withRSA")
            signature.initSign(privateKey)
            signature.update(data.toByteArray())
            val signatureBytes = signature.sign()

            // Return Base64URL encoded signature
            return base64UrlEncode(signatureBytes)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to sign JWT: ${e.message}", e)
            throw e
        }
    }

    /**
     * Exchange JWT for Google Access Token
     */
    private fun exchangeJWTForAccessToken(jwt: String): String? {
        return try {
            val url = URL(TOKEN_URL)
            val connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                doOutput = true
            }

            // Request body
            val requestBody = "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=$jwt"
            connection.outputStream.write(requestBody.toByteArray())

            // Read response
            val responseCode = connection.responseCode

            if (responseCode == 200) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonResponse = JSONObject(response)
                jsonResponse.getString("access_token")
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                Log.e(TAG, "❌ Token exchange failed ($responseCode): $errorResponse")
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to exchange JWT: ${e.message}", e)
            null
        }
    }

    /**
     * Base64 URL-safe encoding (without padding)
     */
    private fun base64UrlEncode(data: ByteArray): String {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(data)
    }
}