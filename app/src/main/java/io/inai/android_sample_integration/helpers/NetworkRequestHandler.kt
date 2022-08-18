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

    const val KEY_URL = "URL"
    const val KEY_AUTH_STRING = "AUTH_STRING"
    const val KEY_POST_DATA_JSON = "POST_DATA_JSON"
    const val KEY_REQUEST_TYPE = "REQUEST_TYPE"
    const val GET: String = "GET"
    const val POST: String = "POST"
    private const val UNKNOWN_ERROR = "UNKNOWN_ERROR"
    //  We use a coroutine scope to launch network requests and switch between threads.
    //  The scope should always be cancelled if the operation is completed or the operation
    //  is no more necessary.
    private var coroutineScope : CoroutineScope? = null

    fun makeNetworkRequest(
        networkConfig: Map<String, String>,
        resultCallback: (Result) -> Unit
    ){
        //  Initialize a CoroutineExceptionHandler. This handler catches any exceptions
        //  that are thrown during the network call and provides a single method to handle
        //  the failure cases. This eliminates the need of multiple try/catch blocks.
        val errorHandler = CoroutineExceptionHandler { context, error ->
            //  Parse the error as a Result.Failure object and send it in the callback on the main thread.
            coroutineScope?.launch(Dispatchers.Main) {
                val errorResponse = Result.Failure(error.localizedMessage ?: UNKNOWN_ERROR)
                resultCallback(errorResponse)
            }
        }
        val mUrl = URL(networkConfig[KEY_URL])
        val conn: HttpURLConnection = mUrl.openConnection() as HttpURLConnection
        //  Set connection properties based on request type.
        conn.apply {
            if (networkConfig[KEY_REQUEST_TYPE] == POST){
                doInput = true
                doOutput = true
            }
            readTimeout = 3000
            connectTimeout = 3000
            requestMethod = GET
            setRequestProperty("charset", "utf-8")
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", networkConfig[KEY_AUTH_STRING])
        }

        //  Initialize a coroutine scope with a job and a error handler.
        coroutineScope = CoroutineScope(SupervisorJob() + errorHandler)
        // Launch a child coroutine inside the parent scope on the Dispatchers.IO thread.
        coroutineScope?.launch(Dispatchers.IO) {
            if (networkConfig[KEY_REQUEST_TYPE] == POST){
                //  Write the request body to the connection object output stream.
                //  Convert requestBody string into a ByteArray
                val postData: ByteArray = networkConfig.getOrElse(KEY_POST_DATA_JSON) { "" }.toByteArray(StandardCharsets.UTF_8)
                val outputStream = DataOutputStream(conn.outputStream)
                outputStream.write(postData)
                outputStream.flush()
            }
            //  Make the network call.
            conn.connect()
            val responseCode: Int = conn.responseCode
            //  If result is a 200 / 201 success then parse the response as a Result.Success object
            //  and send it in the callback. If its not a success then throw an exception
            //  with proper message. This Exception will be caught by the errorHandler
            //  and failure message will be sent back to the caller.
            if (responseCode == 201 || responseCode == 200) {
                val successResponse = Result.Success(readResponse(conn))
                coroutineScope?.launch(Dispatchers.Main) {
                    resultCallback(successResponse)
                }
            } else {
                //  Parse the error as a Result.Failure object and send it in the callback.
                coroutineScope?.launch(Dispatchers.Main) {
                    val errorResponse = Result.Failure(conn.responseMessage ?: UNKNOWN_ERROR)
                    resultCallback(errorResponse)
                }
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
    //  This cancels any running coroutine scopes. WHen a scope cancels any background work
    //  that was executing will be cancelled.
    fun cancelCoroutineScope(){
        coroutineScope?.cancel()
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