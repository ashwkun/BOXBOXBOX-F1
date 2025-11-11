package com.f1tracker.data.models

import org.simpleframework.xml.*

@Root(name = "rss", strict = false)
@NamespaceList(
    Namespace(prefix = "itunes", reference = "http://www.itunes.com/dtds/podcast-1.0.dtd"),
    Namespace(prefix = "content", reference = "http://purl.org/rss/1.0/modules/content/"),
    Namespace(prefix = "media", reference = "http://search.yahoo.com/mrss/")
)
data class PodcastRssFeed @JvmOverloads constructor(
    @field:Element(name = "channel", required = false)
    @param:Element(name = "channel", required = false)
    var channel: PodcastChannel? = null
)

@Root(name = "channel", strict = false)
@NamespaceList(
    Namespace(prefix = "itunes", reference = "http://www.itunes.com/dtds/podcast-1.0.dtd")
)
data class PodcastChannel @JvmOverloads constructor(
    @field:ElementList(entry = "title", inline = true, required = false)
    @param:ElementList(entry = "title", inline = true, required = false)
    var titles: List<String>? = null,
    
    @field:Element(name = "description", required = false)
    @param:Element(name = "description", required = false)
    var description: String? = null,
    
    @field:ElementList(entry = "image", inline = true, required = false)
    @param:ElementList(entry = "image", inline = true, required = false)
    var images: List<PodcastChannelImage>? = null,
    
    @field:ElementList(entry = "item", inline = true, required = false)
    @param:ElementList(entry = "item", inline = true, required = false)
    var items: List<PodcastItem>? = null
) {
    // Helper property to get the first title
    val title: String?
        get() = titles?.firstOrNull()
    
    // Helper property to get image URL from either source
    fun getFinalImageUrl(): String? {
        images?.forEach { img ->
            img.href?.let { return it }
            img.url?.let { return it }
        }
        return null
    }
}

@Root(name = "image", strict = false)
@NamespaceList(
    Namespace(prefix = "itunes", reference = "http://www.itunes.com/dtds/podcast-1.0.dtd")
)
data class PodcastChannelImage @JvmOverloads constructor(
    @field:Attribute(name = "href", required = false)
    @param:Attribute(name = "href", required = false)
    var href: String? = null,
    
    @field:Element(name = "url", required = false)
    @param:Element(name = "url", required = false)
    var url: String? = null
)

@Root(name = "image", strict = false)
@NamespaceList(
    Namespace(prefix = "itunes", reference = "http://www.itunes.com/dtds/podcast-1.0.dtd")
)
data class PodcastImage @JvmOverloads constructor(
    @field:Attribute(name = "href", required = false)
    @param:Attribute(name = "href", required = false)
    var href: String? = null
)

@Root(name = "item", strict = false)
@NamespaceList(
    Namespace(prefix = "itunes", reference = "http://www.itunes.com/dtds/podcast-1.0.dtd"),
    Namespace(prefix = "content", reference = "http://purl.org/rss/1.0/modules/content/")
)
data class PodcastItem @JvmOverloads constructor(
    @field:ElementList(entry = "title", inline = true, required = false)
    @param:ElementList(entry = "title", inline = true, required = false)
    var titles: List<String>? = null,
    
    @field:Element(name = "description", required = false)
    @param:Element(name = "description", required = false)
    var description: String? = null,
    
    @field:Element(name = "encoded", required = false)
    @param:Element(name = "encoded", required = false)
    var contentEncoded: String? = null,
    
    @field:Element(name = "pubDate", required = false)
    @param:Element(name = "pubDate", required = false)
    var pubDate: String? = null,
    
    @field:Element(name = "duration", required = false)
    @param:Element(name = "duration", required = false)
    var duration: String? = null,
    
    @field:Element(name = "enclosure", required = false)
    @param:Element(name = "enclosure", required = false)
    var enclosure: PodcastEnclosure? = null,
    
    @field:Element(name = "image", required = false)
    @param:Element(name = "image", required = false)
    var image: PodcastImage? = null
) {
    // Helper property to get the first title
    val title: String?
        get() = titles?.firstOrNull()
}

@Root(name = "enclosure", strict = false)
data class PodcastEnclosure @JvmOverloads constructor(
    @field:Attribute(name = "url", required = false)
    @param:Attribute(name = "url", required = false)
    var url: String? = null,
    
    @field:Attribute(name = "type", required = false)
    @param:Attribute(name = "type", required = false)
    var type: String? = null
)

// Simplified model for UI
data class Podcast(
    val name: String,
    val imageUrl: String,
    val episodes: List<PodcastEpisode>
)

data class PodcastEpisode(
    val title: String,
    val description: String,
    val audioUrl: String,
    val duration: String,
    val publishedDate: String,
    val imageUrl: String
)

