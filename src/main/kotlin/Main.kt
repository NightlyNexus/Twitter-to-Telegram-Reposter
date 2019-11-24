import TelegramPost.Companion.toTelegramPost
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.OkHttpClient
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.util.concurrent.TimeUnit.SECONDS

fun main() {
  val propertiesFile = File("src/main/resources/properties.json")
  val options = JsonReader.Options.of(
      "twitter_bearer_token",
      "twitter_handle",
      "telegram_api_key",
      "telegram_chat_id"
  )
  var twitterBearerToken: String? = null
  var twitterHandle: String? = null
  var telegramApiKey: String? = null
  var telegramChatId: String? = null
  JsonReader.of(propertiesFile.source().buffer())
      .use { reader ->
        reader.beginObject()
        while (reader.hasNext()) {
          when (reader.selectName(options)) {
            0 -> {
              if (twitterBearerToken != null) {
                throw JsonDataException("Duplicate twitter_bearer_token")
              }
              twitterBearerToken = reader.nextString()
            }
            1 -> {
              if (twitterHandle != null) {
                throw JsonDataException("Duplicate twitter_handle")
              }
              twitterHandle = reader.nextString()
            }
            2 -> {
              if (telegramApiKey != null) {
                throw JsonDataException("Duplicate telegram_api_key")
              }
              telegramApiKey = reader.nextString()
            }
            3 -> {
              if (telegramChatId != null) {
                throw JsonDataException("Duplicate telegram_chat_id")
              }
              telegramChatId = reader.nextString()
            }
            -1 -> {
              throw JsonDataException("Unexpected property: ${reader.nextName()}")
            }
            else -> throw AssertionError()
          }
        }
        if (twitterBearerToken == null) {
          throw JsonDataException("Missing twitter_bearer_token")
        }
        if (twitterHandle == null) {
          throw JsonDataException("Missing twitter_handle")
        }
        if (telegramApiKey == null) {
          throw JsonDataException("Missing telegram_api_key")
        }
        if (telegramChatId == null) {
          throw JsonDataException("Missing telegram_chat_id")
        }
        reader.endObject()
      }

  val twitterSinceIdFile = File("src/main/resources/twitter_since_id")
  val twitterSinceId = twitterSinceIdFile.source()
      .buffer()
      .use { source ->
        source.readUtf8()
      }

  val client = OkHttpClient.Builder()
      .connectTimeout(360L, SECONDS)
      .readTimeout(360L, SECONDS)
      .writeTimeout(360L, SECONDS)
      .build()
  val moshi = Moshi.Builder()
      .build()
  val tweetsAdapter =
      moshi.adapter<List<TwitterPost>>(
          Types.newParameterizedType(List::class.java, TwitterPost::class.java)
      )
  val fetcher = TwitterPostsFetcher(client, tweetsAdapter, twitterBearerToken!!, twitterHandle!!)
  val messages = fetcher.fetch(twitterSinceId)
  val poster = TelegramPoster(client, telegramApiKey!!, telegramChatId!!)

  var newTwitterSinceId: String? = null
  try {
    for (i in messages.indices) {
      val message = messages[i]
      val telegramPost = message.toTelegramPost(client)
      poster.post(telegramPost)
      newTwitterSinceId = message.id_str
    }
  } finally {
    if (newTwitterSinceId != null) {
      twitterSinceIdFile.sink()
          .buffer()
          .use { sink ->
            sink.writeUtf8(newTwitterSinceId)
          }
    }
  }
}
