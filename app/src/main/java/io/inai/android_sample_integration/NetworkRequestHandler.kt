package io.inai.android_sample_integration

import android.util.Base64
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlin.Exception

object NetworkRequestHandler {

    private const val GET: String = "GET"
    private const val POST: String = "POST"

    fun makeGetRequest(
        url: String?,
        authentication: String,
        resultCallback: (Result) -> Unit
    ) {
        val mUrl = URL(url)
        val conn: HttpURLConnection = mUrl.openConnection() as HttpURLConnection
        conn.apply {
            readTimeout = 3000
            connectTimeout = 3000
            requestMethod = GET
            setRequestProperty("charset", "utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", authentication)
        }
        val errorHandler = CoroutineExceptionHandler { context, error ->
            val errorResponse = Result.Failure(error.localizedMessage ?: "")
            resultCallback(errorResponse)
        }
        val coroutineScope = CoroutineScope(SupervisorJob() + errorHandler)
        coroutineScope.launch(Dispatchers.IO) {
            conn.connect()
            val responseCode: Int = conn.responseCode // To Check for 200
            if (responseCode == 201 || responseCode == 200) {
                val successResponse = Result.Success(readResponse(conn))
                resultCallback(successResponse)
            } else {
                throw Exception(conn.responseMessage)
            }
        }
    }

    fun makePostRequest(
        url: String?,
        requestBody: String,
        authentication: String,
        resultCallback: (Result) -> Unit
    ) {

        val mUrl = URL(url)
        val postData: ByteArray = requestBody.toByteArray(StandardCharsets.UTF_8)
        val conn: HttpURLConnection = mUrl.openConnection() as HttpURLConnection
        conn.apply {
            readTimeout = 3000
            connectTimeout = 3000
            requestMethod = POST
            doInput = true
            doOutput = true
            setRequestProperty("charset", "utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", authentication)
        }
        val errorHandler = CoroutineExceptionHandler { context, error ->
            val errorResponse = Result.Failure(error.localizedMessage ?: "")
            resultCallback(errorResponse)
        }

        val coroutineScope = CoroutineScope(SupervisorJob() + errorHandler)
        coroutineScope.launch(Dispatchers.IO) {
            val outputStream = DataOutputStream(conn.outputStream)
            outputStream.write(postData)
            outputStream.flush()

            conn.connect()
            val responseCode: Int = conn.responseCode // To Check for 200
            if (responseCode == 201 || responseCode == 200) {
                val successResponse = Result.Success(readResponse(conn))
                resultCallback(successResponse)
            } else {
                throw Exception(conn.responseMessage)
            }
        }
    }


    fun getEncodedAuthString(inaiSdkUsername: String, inaiSdkPassword: String): String {
        val authString = "$inaiSdkUsername:$inaiSdkPassword"
        val encodedCredentials =
            Base64.encodeToString(
                authString.toByteArray(StandardCharsets.UTF_8),
                Base64.NO_WRAP
            )
        return "BASIC $encodedCredentials"
    }

    private fun readResponse(conn: HttpURLConnection): String {
        val `in` =
            BufferedReader(InputStreamReader(conn.inputStream))
        var inputLine: String?
        val response = StringBuffer()
        while (`in`.readLine().also { inputLine = it } != null) {
            response.append(inputLine)
        }
        `in`.close()
        return response.toString()
    }

    sealed class Result {
        class Failure(val message: String) : Result()
        class Success(val message: String) : Result()
    }
}