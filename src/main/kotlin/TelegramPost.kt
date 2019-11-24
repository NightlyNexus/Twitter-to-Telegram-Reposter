import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

class TelegramPost private constructor(
  val text: String,
  val videoUrl: String?,
  val videoThumbnailUrl: String?,
  val photoUrls: List<String>?
) {
  companion object {
    fun TwitterPost.toTelegramPost(client: OkHttpClient): TelegramPost {
      var messageText = full_text
      var videoUrl: String? = null
      var videoThumbnailUrl: String? = null
      var photos: MutableList<String>? = null
      for (j in entities.urls.indices) {
        val url = entities.urls[j]
        messageText = messageText.replace(url.url, url.expanded_url)
      }
      if (extended_entities != null) {
        for (j in extended_entities.media.indices) {
          val media = extended_entities.media[j]
          messageText = messageText.replace(media.url, "")
          when (media.type) {
            "video" -> {
              if (videoUrl != null) {
                throw RuntimeException("More than 1 video.")
              }
              videoUrl = media.video_info!!.variants[0].url.toHttpUrl()
                  .newBuilder()
                  // Telegram API does not like query parameters in the video url.
                  .removeAllEncodedQueryParameters("tag")
                  .build()
                  .toString()
              videoThumbnailUrl = media.media_url_https
            }
            "photo" -> {
              if (photos == null) {
                photos = mutableListOf()
              }
              photos.add(media.media_url_https)
            }
            else -> {
              throw RuntimeException("Unhandled media: ${media.type}")
            }
          }
        }
      }
      return TelegramPost(messageText, videoUrl, videoThumbnailUrl, photos)
    }
  }
}
