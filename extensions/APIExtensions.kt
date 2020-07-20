package com.aziz.spark.utils

import android.util.Log
import androidx.lifecycle.liveData
import com.google.gson.Gson
import com.google.gson.JsonArray
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.*

fun responseBodyLiveData(call: suspend () -> Response<ResponseBody>) = liveData{
    try {
        emit(APIResult.loading(null))
        val response = safeAPICall { call() }.string()

        if(response.hasError) emit(APIResult.error(response.errorMessage))
        else  emit(APIResult.success(response, response.successMessage))
    }
    catch (e:Exception){
        e.printStackTrace()
        val error = ResponseError(e.message?:"Something went wrong. Please try again", 500)
        emit(APIResult.error(error.message, error.errorCode))
    }
}


suspend fun<T: Any> safeAPICall(call: suspend () -> Response<T>) : T{
    val response = try {
        call.invoke()
    }
    catch (e:java.lang.Exception){
        e.printStackTrace()
        val message = if( e is ConnectException || e is SocketTimeoutException ||
            e is HttpException ||
            e.message?.contains("unexpected end of stream on Connection") == true ||
            e is SocketException
        )
            "Connection Error" else  unspecified_error
        val responseError = ResponseError(message, 500).convertToJsonString()
        Log.e("safeAPICall", "safeAPICall: error thrown = $responseError" )
        Log.e("safeAPICall", "safeAPICall: actual error = ${e.message}", e )

        //json is passed as message
        throw IOException(responseError)
    }



    if(response.isSuccessful){
        return response.body()!!
    }else{
        val error = response.errorBody()?.string()

        error?.let{
            val message = JSONObject(it).optString("message", "Something went wrong")
            val responseError = ResponseError(message.takeIf { response.code() != 500 }?:response.message(), response.code())
            throw IOException(responseError.convertToJsonString())
        }
        throw IOException(ResponseError("Something went wrong. Please try again.", 500).convertToJsonString())
    }
}

data class ResponseError(val message:String, val errorCode:Int)


data class APIResult<out T>(val status: Status, val data: T?, val message: String?, val code:Int? = null) {
    companion object {
        fun <T> success(data: T?, message: String? = null): APIResult<T> {
            return APIResult(Status.SUCCESS, data, message)
        }

        fun <T> error(msg: String?, code: Int? = null): APIResult<T> {
            return APIResult(Status.ERROR, null, msg, code)
        }

        fun <T> loading(data: T? = null): APIResult<T> {
            return APIResult(Status.LOADING, data, null)
        }
    }
}


enum class Status {
    SUCCESS,
    ERROR,
    LOADING
}


val String?.hasError
    get() = try{ JSONObject(this?:"{}").optInt("status", 200) != 200 } catch (e:Exception){ e.printStackTrace(); true}


const val unspecified_error = "Unspecified error"


val String?.errorMessage: String
    get() = try {
        JSONObject(this?.takeUnless { it.isEmpty() } ?: "{}").run {
            listOf(
                "message",
                "msg",
                "errorMsg",
                "errorMessage"
            ).find { has(it) }?.run { getString(this) } ?: unspecified_error
        }
    }
    catch (e:Exception){
        e.printStackTrace()
        "Something went wrong. Please try again"
    }
val String?.successMessage: String
    get() = try {
        JSONObject(this?.takeUnless { it.isEmpty() } ?: "{}").run {
            listOf(
                "message",
                "msg",
                "successMsg",
                "successMessage"
            ).find { has(it) }?.run { getString(this) } ?: "Success"
        }
    }
    catch (e:Exception){ "Success" }



fun <T> Call<T>.onCall(onResponse: (networkException:Throwable?, response: Response<T>?) -> Unit) {
    this.enqueue(object : Callback<T> {
        override fun onFailure(call: Call<T>, t: Throwable) {
            onResponse.invoke(t, null)
        }

        override fun onResponse(call: Call<T>, response: Response<T>) {
            onResponse.invoke(null, response)
        }

    })
}



fun String?.toRequestBody(): RequestBody {
    return (this?:"").toRequestBody("text/plain".toMediaTypeOrNull())
}

fun File.toMultipartBody(key:String): MultipartBody.Part {
    val requestBody = this.asRequestBody(MultipartBody.FORM)
    return MultipartBody.Part.createFormData(key, this.name, requestBody)
}


inline fun <reified T> JSONArray.toList():List<T?>{

    val list = mutableListOf<T?>()
    this.forEach {
        list.add(it.toString().toModel<T>())
    }
    return list
}

inline fun JSONArray.forEachNames(action: (name:String)->Unit){
    for (i in 0 until length()){
        action(getJSONObject(i).optString("name"))
    }
}

inline fun JSONArray.forEachIndexed(action: (i:Int, jsonObject:JSONObject)->Unit){
    for (i in 0 until length()){
        action(i, getJSONObject(i))
    }
}

 fun JSONArray.isEmpty() = this.length() == 0


inline fun <reified T> JSONObject.toModel(): T? = this.run {
    try {
        Gson().fromJson<T>(this.toString(), T::class.java)
    }
    catch (e:java.lang.Exception){ e.printStackTrace()
        Log.e("JSONObject to model",  e.message.toString() +" = $this" )

        null }
}

inline fun <reified T> String.toModel(): T? = this.run {
    try {
        JSONObject(this).toModel<T>()
    }
    catch (e:java.lang.Exception){
        Log.e("String to model",  e.message.toString() +" = $this"  )
        e.printStackTrace()
        null
    }
}


//JSON Extensions

inline fun JSONArray.forEach(action: (jsonObject:JSONObject)->Unit){
    for (i in 0 until length()){
        action(getJSONObject(i))
    }
}


fun String.parseServerDate():String{
    return try{
        val sdf0 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        val sdf1 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val date = sdf0.parse(this)
        sdf1.format(date!!)
    }
    catch (e:Exception) { Log.e("Error", "parseServerDate: Cannot parse $this" ); this}
}


fun Any.convertToJsonString():String{
    return try{
        Gson().toJson(this)?:""
    }
    catch (e:Exception){
        e.printStackTrace()
        Log.e("Extensions", "convertToJsonString: ${e.message}" )
        "{}"
    }
}

fun String.getResultFromJSON(key:String = "result"): String? = kotlin.run {
    try {
        JSONObject(this).optString(key) }
    catch (e:Exception){
        "Failed"
    }
}


val String.isResultSuccessful
    get() = this.getResultFromJSON().equals("ok",true)


fun List<Any>.toJsonArray(): JsonArray = kotlin.run {
    try{
        Gson().toJsonTree(this).asJsonArray
    }
    catch (e:Exception){
        e.printStackTrace()
        Log.e("Extensions", "convertToJsonString: ${e.message}" )
        JsonArray()
    }
}