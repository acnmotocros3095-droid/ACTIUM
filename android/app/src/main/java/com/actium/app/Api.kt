package com.actium.app
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
private val client=OkHttpClient.Builder().connectTimeout(15,TimeUnit.SECONDS).readTimeout(30,TimeUnit.SECONDS).build()
private val jsonType="application/json; charset=utf-8".toMediaType()
internal class Api {
    private val base=BuildConfig.SUPABASE_URL.trimEnd('/')
    private val key=BuildConfig.SUPABASE_ANON_KEY
    var token:String?=null
    var userId:String?=null
    val configured get()=base.startsWith("https://") && key.isNotBlank()
    fun request(method:String,path:String,body:JSONObject?=null):String {
        val b=Request.Builder().url(base+path).header("apikey",key)
        token?.let { b.header("Authorization","Bearer $it") }
        b.header("Content-Type","application/json")
        if(method=="POST") b.header("Prefer","return=representation")
        val rb=when(method) {
            "POST"->b.post((body?:JSONObject()).toString().toRequestBody(jsonType))
            "PATCH"->b.patch((body?:JSONObject()).toString().toRequestBody(jsonType))
            "DELETE"->b.delete()
            else->b.get()
        }
        client.newCall(rb.build()).execute().use { response ->
            val text=response.body?.string().orEmpty()
            if(!response.isSuccessful) {
                val msg=runCatching{JSONObject(text).optString("message",text)}.getOrDefault(text)
                throw IllegalStateException("HTTP ${response.code}: $msg")
            }
            return text
        }
    }
    fun askCoach(question:String):String {
        val result=JSONObject(request("POST","/functions/v1/actium-coach",JSONObject().put("question",question)))
        if(result.has("error"))throw IllegalStateException(result.optString("error"))
        return result.optString("answer","Sin respuesta")
    }
    fun auth(email:String,password:String,signup:Boolean):String {
        val payload=JSONObject().put("email",email).put("password",password)
        val raw=request("POST",if(signup)"/auth/v1/signup" else "/auth/v1/token?grant_type=password",payload)
        val obj=JSONObject(raw)
        val t=obj.optString("access_token")
        if(t.isNotBlank()) {token=t;userId=obj.optJSONObject("user")?.optString("id")}
        return if(t.isBlank()) "Revisa tu correo para confirmar la cuenta y luego inicia sesión." else "Sesión iniciada"
    }
    fun rows(table:String):List<JSONObject> {
        val result=mutableListOf<JSONObject>()
        var offset=0
        do {
            val json=JSONArray(request("GET","/rest/v1/$table?select=*&user_id=eq.$userId&order=id&limit=200&offset=$offset"))
            result.addAll((0 until json.length()).map{json.getJSONObject(it)})
            offset+=200
            if(json.length()<200) break
        } while(true)
        return result
    }
    fun add(table:String,fields:JSONObject) {
        if(table=="meal_bundle") {
            request("POST","/rest/v1/rpc/actium_add_meal",fields)
            return
        }
        fields.put("user_id",userId)
        request("POST","/rest/v1/$table",fields)
    }
}
