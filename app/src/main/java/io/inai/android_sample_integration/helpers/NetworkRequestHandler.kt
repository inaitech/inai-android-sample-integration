package io.inai.android_sample_integration.helpers

import android.util.Base64
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

object NetworkRequestHandler {

    private const val GET: String = "GET"
    private const val POST: String = "POST"
    private const val UNKNOWN_ERROR = "UNKNOWN_ERROR"

    fun makeGetRequest(
        url: String?,
        authentication: String,
        resultCallback: (Result) -> Unit
    ) {
        val mUrl = URL(url)
        //  Open Http Connection
        val conn: HttpURLConnection = mUrl.openConnection() as HttpURLConnection
        //  Set connection properties for a GET request
        conn.apply {
            readTimeout = 3000
            connectTimeout = 3000
            requestMethod = GET
            setRequestProperty("charset", "utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", authentication)
        }
        //  Initialize a CoroutineExceptionHandler. This handler catches any exceptions
        //  that are thrown during the network call and provides a single method to handle
        //  the failure cases. This eliminates the need of multiple try/catch blocks.
        val errorHandler = CoroutineExceptionHandler { context, error ->
            //  Parse the error as a Result.Failure object and send it in the callback.
            val errorResponse = Result.Failure(error.localizedMessage ?: UNKNOWN_ERROR)
            resultCallback(errorResponse)
        }
        //  Initialize a coroutine scope with a job and a error handler.
        val coroutineScope = CoroutineScope(SupervisorJob() + errorHandler)
        // Launch a child coroutine inside the parent scope on the Dispatchers.IO thread.
        coroutineScope.launch(Dispatchers.IO) {
            //  Make the network call.
            conn.connect()
            val responseCode: Int = conn.responseCode
            //  If result is a 200 / 201 success then parse the response as a Result.Success object
            //  and send it in the callback. If its not a success then throw an exception
            //  with proper message. This Exception will be caught by the errorHandler
            //  and failure message will be sent back to the caller.
            if (responseCode == 201 || responseCode == 200) {
                val successResponse = Result.Success(readResponse(conn))
                coroutineScope.launch(Dispatchers.Main) {
                    resultCallback(successResponse)
                }
            } else {
                //  Parse the error as a Result.Failure object and send it in the callback.
                val errorResponse = Result.Failure(conn.responseMessage ?: UNKNOWN_ERROR)
                resultCallback(errorResponse)
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
        //  Convert requestBody string into a ByteArray
        val postData: ByteArray = requestBody.toByteArray(StandardCharsets.UTF_8)
        //  Open Http Connection
        val conn: HttpURLConnection = mUrl.openConnection() as HttpURLConnection
        //  Set connection properties for a POST request
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
        //  Initialize a CoroutineExceptionHandler. This handler catches any exceptions
        //  that are thrown during the network call and provides a single method to handle
        //  the failure cases. This eliminates the need of multiple try/catch blocks.
        val errorHandler = CoroutineExceptionHandler { context, error ->
            //  Parse the error as a Result.Failure object and send it in the callback.
            val errorResponse = Result.Failure(error.localizedMessage ?: UNKNOWN_ERROR)
            resultCallback(errorResponse)
        }
        //  Initialize a coroutine scope with a job and a error handler.
        val coroutineScope = CoroutineScope(SupervisorJob() + errorHandler)
        // Launch a child coroutine inside the parent scope on the Dispatchers.IO thread.
        coroutineScope.launch(Dispatchers.IO) {
            //  Write the request body to the connection object output stream.
            val outputStream = DataOutputStream(conn.outputStream)
            outputStream.write(postData)
            outputStream.flush()
            //  Make the network call.
            conn.connect()
            //  If result is a 200 / 201 success then parse the response as a Result.Success object
            //  and send it in the callback. If its not a success then throw an exception
            //  with proper message. This Exception will be caught by the errorHandler
            //  and failure message will be sent back to the caller.
            val responseCode: Int = conn.responseCode
            if (responseCode == 201 || responseCode == 200) {
                val successResponse = Result.Success(readResponse(conn))
                coroutineScope.launch(Dispatchers.Main) {
                    resultCallback(successResponse)
                }
            } else {
                //  Parse the error as a Result.Failure object and send it in the callback.
                val errorResponse = Result.Failure(conn.responseMessage ?: UNKNOWN_ERROR)
                resultCallback(errorResponse)
            }
        }
    }

    //  This method takes the username and password as params and encode them into
    //  a Base64 string for authentication.
    fun getEncodedAuthString(inaiSdkUsername: String, inaiSdkPassword: String): String {
        val authString = "$inaiSdkUsername:$inaiSdkPassword"
        val encodedCredentials =
            Base64.encodeToString(
                authString.toByteArray(StandardCharsets.UTF_8),
                Base64.NO_WRAP
            )
        return "BASIC $encodedCredentials"
    }

    //  Reads the response from the connection object input stream
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

    //  A sealed class which contains two child classes - Success and Failure.
    //  This enables us to pass the success or failure result as a single Result
    //  instance. This result object can then be checked for a Success  instance
    //  or a Failure instance. This eliminates the need for separate callbacks
    //  for success and failure cases.
    sealed class Result {
        class Success(val message: String) : Result()
        class Failure(val message: String) : Result()
    }
}