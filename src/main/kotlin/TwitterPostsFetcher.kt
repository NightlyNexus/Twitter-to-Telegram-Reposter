import com.squareup.moshi.JsonAdapter
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class TwitterPostsFetcher(
  private val client: OkHttpClient,
  private val adapter: JsonAdapter<List<TwitterPost>>,
  bearerToken: String,
  handle: String
) {
  private val request = Request.Builder()
      .addHeader("Authorization", "Bearer $bearerToken")
      .url(
          HttpUrl.Builder()
              .scheme("https")
              .host("api.twitter.com")
              .addEncodedPathSegments("1.1/statuses/user_timeline.json")
              .addEncodedQueryParameter("screen_name", handle)
              .addEncodedQueryParameter("trim_user", "true")
              .addEncodedQueryParameter("exclude_replies", "true") // TODO
              .addEncodedQueryParameter("include_rts", "false") // TODO
              .addEncodedQueryParameter("tweet_mode", "extended")
              .addEncodedQueryParameter("count", "200")
              .build()
      )
      .build()

  fun fetch(sinceId: String): List<TwitterPost> {
    val response = client.newCall(
        request.newBuilder()
            .url(
                request.url.newBuilder()
                    .addEncodedQueryParameter("since_id", sinceId)
                    .build()
            )
            .build()
    )
        .execute()
    if (!response.isSuccessful) {
      throw RuntimeException(
          "Twitter fetch HTTP error: ${response.code}\n${response.body!!.string()}"
      )
    }
    response.body!!.source()
        .use { source ->
          return adapter.fromJson(source)!!.asReversed()
        }
  }
}
