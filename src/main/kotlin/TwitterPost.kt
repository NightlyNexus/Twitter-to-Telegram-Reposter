import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class TwitterPost constructor(
  val id_str: String,
  val full_text: String,
  val entities: Entities,
  val extended_entities: ExtendedEntities?
) {
  @JsonClass(generateAdapter = true)
  class Entities constructor(
    val user_mentions: List<UserMention>,
    val urls: List<Url>
  )

  @JsonClass(generateAdapter = true)
  class UserMention constructor(
    val screen_name: String
  )

  @JsonClass(generateAdapter = true)
  class Url constructor(
    val url: String,
    val expanded_url: String
  )

  @JsonClass(generateAdapter = true)
  class ExtendedEntities constructor(
    val media: List<Media>
  )

  @JsonClass(generateAdapter = true)
  class Media constructor(
    val media_url_https: String,
    val url: String,
    val type: String,
    val video_info: VideoInfo?
  )

  @JsonClass(generateAdapter = true)
  class VideoInfo constructor(
    val duration_millis: Long,
    val variants: List<Variant>
  )

  @JsonClass(generateAdapter = true)
  class Variant constructor(
    val bitrate: Long?,
    val content_type: String,
    val url: String
  )
}
