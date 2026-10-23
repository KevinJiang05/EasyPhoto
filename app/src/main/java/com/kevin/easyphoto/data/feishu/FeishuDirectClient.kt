package com.kevin.easyphoto.data.feishu

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.math.max

class FeishuDirectClient(
    private val context: Context,
    private val config: FeishuNotificationConfig,
) {
    suspend fun sendPhoto(photoUri: String) {
        val image = FeishuUploadImage.prepare(context.contentResolver, Uri.parse(photoUri), context.cacheDir)
        try {
            val accessToken = getTenantAccessToken()
            val imageKey = uploadImage(accessToken, image.file)
            postWebhookImage(imageKey)
        } finally {
            image.file.delete()
        }
    }

    suspend fun testConnection() {
        getTenantAccessToken()
        postWebhookText("EasyPhoto 飞书通知连接测试成功")
    }

    private fun getTenantAccessToken(): String {
        val connection = jsonConnection(TOKEN_URL)
        return connection.useConnection {
            outputStream.bufferedWriter(Charsets.UTF_8).use {
                it.write(
                    JSONObject()
                        .put("app_id", config.appId.trim())
                        .put("app_secret", config.appSecret.trim())
                        .toString(),
                )
            }
            val response = readJsonResponse(this, "飞书令牌请求")
            response.optString("tenant_access_token").takeIf { it.isNotBlank() }
                ?: throw FeishuRequestException("飞书 App ID 或 App Secret 无效", retryable = false)
        }
    }

    private fun uploadImage(accessToken: String, file: File): String {
        val boundary = "EasyPhoto-${UUID.randomUUID()}"
        val connection = (URL(IMAGE_UPLOAD_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            doOutput = true
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }
        return connection.useConnection {
            DataOutputStream(outputStream).use { output ->
                output.writeUtf8("--$boundary\r\n")
                output.writeUtf8("Content-Disposition: form-data; name=\"image_type\"\r\n\r\n")
                output.writeUtf8("message\r\n")
                output.writeUtf8("--$boundary\r\n")
                output.writeUtf8("Content-Disposition: form-data; name=\"image\"; filename=\"EasyPhoto.jpg\"\r\n")
                output.writeUtf8("Content-Type: image/jpeg\r\n\r\n")
                BufferedInputStream(file.inputStream()).use { input -> input.copyTo(output) }
                output.writeUtf8("\r\n--$boundary--\r\n")
                output.flush()
            }
            val response = readJsonResponse(this, "飞书图片上传")
            response.optJSONObject("data")?.optString("image_key")?.takeIf { it.isNotBlank() }
                ?: throw FeishuRequestException("飞书未返回图片标识", retryable = true)
        }
    }

    private fun postWebhookImage(imageKey: String) {
        postWebhook(
            JSONObject()
                .put("msg_type", "image")
                .put("content", JSONObject().put("image_key", imageKey)),
        )
    }

    private fun postWebhookText(text: String) {
        postWebhook(
            JSONObject()
                .put("msg_type", "text")
                .put("content", JSONObject().put("text", text)),
        )
    }

    private fun postWebhook(body: JSONObject) {
        val connection = jsonConnection(config.webhookUrl.trim())
        connection.useConnection {
            outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }
            readJsonResponse(this, "飞书群消息发送")
        }
    }

    private fun jsonConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT_MILLIS
            readTimeout = READ_TIMEOUT_MILLIS
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }

    private fun readJsonResponse(connection: HttpURLConnection, action: String): JSONObject {
        val statusCode = connection.responseCode
        val input = if (statusCode in 200..299) connection.inputStream else connection.errorStream
        val body = input?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (statusCode !in 200..299) {
            throw FeishuRequestException(
                "${action}失败（HTTP $statusCode）",
                retryable = statusCode == 429 || statusCode >= 500,
            )
        }
        val json = runCatching { JSONObject(body) }
            .getOrElse { throw FeishuRequestException("${action}返回格式异常", retryable = true) }
        if (json.optInt("code", 0) != 0) {
            throw FeishuRequestException("${action}失败（飞书错误 ${json.optInt("code")}）", retryable = false)
        }
        return json
    }

    private inline fun <T> HttpURLConnection.useConnection(block: HttpURLConnection.() -> T): T =
        try {
            block()
        } catch (exception: FeishuRequestException) {
            throw exception
        } catch (exception: IOException) {
            throw FeishuRequestException("网络连接失败", retryable = true, cause = exception)
        } finally {
            disconnect()
        }

    private fun DataOutputStream.writeUtf8(value: String) {
        write(value.toByteArray(Charsets.UTF_8))
    }

    private companion object {
        const val TOKEN_URL = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal"
        const val IMAGE_UPLOAD_URL = "https://open.feishu.cn/open-apis/im/v1/images"
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 30_000
    }
}

class FeishuRequestException(
    message: String,
    val retryable: Boolean,
    cause: Throwable? = null,
) : IOException(message, cause)

private data class FeishuUploadImage(val file: File) {
    companion object {
        private const val MAX_UPLOAD_BYTES = 9_500_000L
        private const val MAX_SIDE = 4096

        fun prepare(contentResolver: ContentResolver, uri: Uri, cacheDir: File): FeishuUploadImage {
            val source = File.createTempFile("easyphoto_feishu_", ".jpg", cacheDir)
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(source).use { output -> input.copyTo(output) }
                } ?: throw IOException("无法读取本地照片")
                if (source.length() <= MAX_UPLOAD_BYTES) return FeishuUploadImage(source)

                val compressed = File.createTempFile("easyphoto_feishu_small_", ".jpg", cacheDir)
                source.delete()
                compress(uri, contentResolver, compressed)
                if (compressed.length() > MAX_UPLOAD_BYTES) {
                    compressed.delete()
                    throw FeishuRequestException("照片过大，无法发送到飞书", retryable = false)
                }
                return FeishuUploadImage(compressed)
            } catch (exception: Exception) {
                source.delete()
                throw exception
            }
        }

        private fun compress(uri: Uri, contentResolver: ContentResolver, output: File) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sampleSize = 1
            while (max(bounds.outWidth / sampleSize, bounds.outHeight / sampleSize) > MAX_SIDE) {
                sampleSize *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: throw IOException("无法压缩本地照片")
            try {
                var quality = 88
                do {
                    FileOutputStream(output).use { stream ->
                        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)) {
                            throw IOException("照片压缩失败")
                        }
                    }
                    quality -= 10
                } while (output.length() > MAX_UPLOAD_BYTES && quality >= 48)
            } finally {
                bitmap.recycle()
            }
        }
    }
}
