package com.f1tracker.data.models

import org.simpleframework.xml.*

@Root(name = "feed", strict = false)
@NamespaceList(
    Namespace(reference = "http://www.w3.org/2005/Atom"),
    Namespace(prefix = "media", reference = "http://search.yahoo.com/mrss/"),
    Namespace(prefix = "yt", reference = "http://www.youtube.com/xml/schemas/2015")
)
data class YouTubeRssFeed @JvmOverloads constructor(
    @field:ElementList(entry = "entry", inline = true, required = false)
    @param:ElementList(entry = "entry", inline = true, required = false)
    var entries: List<YouTubeRssEntry>? = null
)

@Root(name = "entry", strict = false)
data class YouTubeRssEntry @JvmOverloads constructor(
    @field:Element(name = "videoId", required = false)
    @param:Element(name = "videoId", required = false)
    var videoId: String? = null,
    
    @field:Element(name = "title", required = false)
    @param:Element(name = "title", required = false)
    var title: String? = null,
    
    @field:Element(name = "published", required = false)
    @param:Element(name = "published", required = false)
    var published: String? = null,
    
    @field:Element(name = "group", required = false)
    @param:Element(name = "group", required = false)
    var mediaGroup: MediaGroup? = null
)

@Root(name = "group", strict = false)
data class MediaGroup @JvmOverloads constructor(
    @field:Element(name = "description", required = false)
    @param:Element(name = "description", required = false)
    var description: String? = null,
    
    @field:Element(name = "thumbnail", required = false)
    @param:Element(name = "thumbnail", required = false)
    var thumbnail: MediaThumbnail? = null,
    
    @field:Element(name = "community", required = false)
    @param:Element(name = "community", required = false)
    var community: MediaCommunity? = null,
    
    @field:Element(name = "content", required = false)
    @param:Element(name = "content", required = false)
    var content: MediaContent? = null
)

@Root(name = "content", strict = false)
data class MediaContent @JvmOverloads constructor(
    @field:Attribute(name = "duration", required = false)
    @param:Attribute(name = "duration", required = false)
    var duration: Int? = null
)

@Root(name = "thumbnail", strict = false)
data class MediaThumbnail @JvmOverloads constructor(
    @field:Attribute(name = "url", required = false)
    @param:Attribute(name = "url", required = false)
    var url: String? = null,
    
    @field:Attribute(name = "width", required = false)
    @param:Attribute(name = "width", required = false)
    var width: Int? = null,
    
    @field:Attribute(name = "height", required = false)
    @param:Attribute(name = "height", required = false)
    var height: Int? = null
)

@Root(name = "community", strict = false)
data class MediaCommunity @JvmOverloads constructor(
    @field:Element(name = "statistics", required = false)
    @param:Element(name = "statistics", required = false)
    var statistics: MediaStatistics? = null
)

@Root(name = "statistics", strict = false)
data class MediaStatistics @JvmOverloads constructor(
    @field:Attribute(name = "views", required = false)
    @param:Attribute(name = "views", required = false)
    var views: String? = null
)

// Simplified model for UI
data class F1Video(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val views: String,
    val publishedDate: String,
    val duration: String = "",
    val viewCount: Long = 0
)

