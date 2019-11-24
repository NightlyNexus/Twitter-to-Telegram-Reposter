import com.squareup.moshi.JsonWriter
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer

class TelegramPoster(
  private val client: OkHttpClient,
  apiKey: String,
  chatId: String
) {
  private val httpUrlText = HttpUrl.Builder()
      .scheme("https")
      .host("api.telegram.org")
      .addEncodedPathSegments("bot$apiKey/sendMessage")
      .addEncodedQueryParameter("chat_id", chatId)
      .build()
  private val httpUrlVideo = HttpUrl.Builder()
      .scheme("https")
      .host("api.telegram.org")
      .addEncodedPathSegments("bot$apiKey/sendVideo")
      .addEncodedQueryParameter("chat_id", chatId)
      .addEncodedQueryParameter("supports_streaming", "true")
      .build()
  private val httpUrlPhoto = HttpUrl.Builder()
      .scheme("https")
      .host("api.telegram.org")
      .addEncodedPathSegments("bot$apiKey/sendPhoto")
      .addEncodedQueryParameter("chat_id", chatId)
      .build()
  private val httpUrlPhotos = HttpUrl.Builder()
      .scheme("https")
      .host("api.telegram.org")
      .addEncodedPathSegments("bot$apiKey/sendMediaGroup")
      .addEncodedQueryParameter("chat_id", chatId)
      .build()

  fun post(post: TelegramPost) {
    var gallery = false
    val url = if (post.videoUrl != null) {
      httpUrlVideo.newBuilder()
          .addQueryParameter("video", post.videoUrl)
          .addQueryParameter("thumb", post.videoThumbnailUrl)
          .addQueryParameter("caption", post.text)
          .build()
    } else if (post.photoUrls != null) {
      if (post.photoUrls.size == 1) {
        httpUrlPhoto.newBuilder()
            .addQueryParameter("photo", post.photoUrls[0])
            .addQueryParameter("caption", post.text)
            .build()
      } else {
        gallery = true
        httpUrlPhotos.newBuilder()
            .addQueryParameter("media", encodeInputMediaPhoto(post.photoUrls))
            .build()
      }
    } else {
      httpUrlText.newBuilder()
          .addQueryParameter("text", post.text)
          .build()
    }
    client.newCall(Request.Builder().url(url).build())
        .execute()
        .use { response ->
          if (!response.isSuccessful) {
            throw RuntimeException(
                "Telegram post HTTP error: ${response.code}. $url\n${response.body!!.string()}"
            )
          }
        }
    // Send text in a separate post for 2+ photos due to gallery view.
    if (gallery) {
      client.newCall(
          Request.Builder().url(
              httpUrlText.newBuilder()
                  .addQueryParameter("text", post.text)
                  .build()
          ).build()
      )
          .execute()
          .use { response ->
            if (!response.isSuccessful) {
              throw RuntimeException(
                  "Telegram post HTTP error: ${response.code}. $url\n${response.body!!.string()}"
              )
            }
          }
    }
  }

  private fun encodeInputMediaPhoto(
    photoUrls: List<String>
  ): String {
    require(photoUrls.size >= 2)
    val buffer = Buffer()
    val writer = JsonWriter.of(buffer)
    writer.beginArray()
    for (i in photoUrls.indices) {
      val photoUrl = photoUrls[i]
      writer
          .beginObject()
          .name("type")
          .value("photo")
          .name("media")
          .value(photoUrl)
          .endObject()
    }
    writer.endArray()
    return buffer.readUtf8()
  }
}
