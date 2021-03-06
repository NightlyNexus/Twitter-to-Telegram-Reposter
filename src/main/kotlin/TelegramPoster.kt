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
    val url = if (post.videoUrls != null) {
      postVideo(post.videoUrls, 0, post.videoThumbnailUrl!!, post.text)
      return
    } else if (post.photoUrls != null) {
      require(post.photoUrls.isNotEmpty())
      if (post.photoUrls.size == 1) {
        httpUrlPhoto.newBuilder()
            .addQueryParameter("photo", post.photoUrls[0])
            .addQueryParameter("caption", post.text)
            .build()
      } else {
        httpUrlPhotos.newBuilder()
            .addQueryParameter("media", encodeInputMediaPhoto(post.photoUrls, post.text))
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
  }

  private fun postVideo(
    videoUrls: List<String>,
    index: Int,
    videoThumbnailUrl: String,
    text: String
  ) {
    val url = httpUrlVideo.newBuilder()
        .addQueryParameter("video", videoUrls[index])
        .addQueryParameter("thumb", videoThumbnailUrl)
        .addQueryParameter("caption", text)
        .build()
    client.newCall(Request.Builder().url(url).build())
        .execute()
        .use { response ->
          if (response.code == 400 && index + 1 != videoUrls.size) {
            postVideo(videoUrls, index + 1, videoThumbnailUrl, text)
            return
          }
          if (!response.isSuccessful) {
            throw RuntimeException(
                "Telegram post HTTP error: ${response.code}. $url\n${response.body!!.string()}"
            )
          }
        }
  }

  private fun encodeInputMediaPhoto(
    photoUrls: List<String>,
    text: String
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
          .apply {
            if (i == 0) {
              name("caption")
              value(text)
            }
          }
          .endObject()
    }
    writer.endArray()
    return buffer.readUtf8()
  }
}
