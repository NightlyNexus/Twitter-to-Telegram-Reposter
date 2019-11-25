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
      val textReplacements = mutableListOf<TextReplacement>()
      var videoUrl: String? = null
      var videoThumbnailUrl: String? = null
      var photos: MutableList<String>? = null
      for (i in entities.user_mentions.indices) {
        val userMention = entities.user_mentions[i]
        if (userMention.indices.size != 2) {
          throw RuntimeException("Unexpected user mention indices: ${userMention.indices}")
        }
        textReplacements += TextReplacement(userMention.name, userMention.indices)
      }
      for (i in entities.urls.indices) {
        val url = entities.urls[i]
        if (url.indices.size != 2) {
          throw RuntimeException("Unexpected url indices: ${url.indices}")
        }
        textReplacements += TextReplacement(url.expanded_url, url.indices)
      }
      if (extended_entities != null) {
        for (i in extended_entities.media.indices) {
          val media = extended_entities.media[i]
          if (media.indices.size != 2) {
            throw RuntimeException("Unexpected media indices: ${media.indices}")
          }
          textReplacements += TextReplacement(null, media.indices)
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
      textReplacements.sort()
      val messageText = StringBuilder(full_text.length + 20).append(full_text).apply {
        for (i in textReplacements.size - 1 downTo 0) {
          val textReplacement = textReplacements[i]
          if (textReplacement.replacementText == null) {
            delete(
                textReplacement.indices[0],
                textReplacement.indices[1]
            )
          } else {
            replace(
                textReplacement.indices[0],
                textReplacement.indices[1],
                textReplacement.replacementText
            )
          }
        }
      }.toString()
      return TelegramPost(messageText, videoUrl, videoThumbnailUrl, photos)
    }
  }
}

private class TextReplacement(
  val replacementText: String?,
  val indices: List<Int>
) : Comparable<TextReplacement> {
  override fun compareTo(other: TextReplacement) = indices[0] - other.indices[0]
}
