package com.f1tracker.data.models

import org.simpleframework.xml.*

@Root(name = "rss", strict = false)
data class MotorsportRssFeed @JvmOverloads constructor(
    @field:Element(name = "channel", required = false)
    @param:Element(name = "channel", required = false)
    var channel: MotorsportChannel? = null
)

@Root(name = "channel", strict = false)
data class MotorsportChannel @JvmOverloads constructor(
    @field:ElementList(entry = "item", inline = true, required = false)
    @param:ElementList(entry = "item", inline = true, required = false)
    var items: List<MotorsportItem>? = null
)

@Root(name = "item", strict = false)
data class MotorsportItem @JvmOverloads constructor(
    @field:Element(name = "title", required = false)
    @param:Element(name = "title", required = false)
    var title: String? = null,

    @field:Element(name = "link", required = false)
    @param:Element(name = "link", required = false)
    var link: String? = null,

    @field:Element(name = "description", required = false)
    @param:Element(name = "description", required = false)
    var description: String? = null,

    @field:Element(name = "pubDate", required = false)
    @param:Element(name = "pubDate", required = false)
    var pubDate: String? = null,

    @field:Element(name = "enclosure", required = false)
    @param:Element(name = "enclosure", required = false)
    var enclosure: MotorsportEnclosure? = null
)

@Root(name = "enclosure", strict = false)
data class MotorsportEnclosure @JvmOverloads constructor(
    @field:Attribute(name = "url", required = false)
    @param:Attribute(name = "url", required = false)
    var url: String? = null,

    @field:Attribute(name = "type", required = false)
    @param:Attribute(name = "type", required = false)
    var type: String? = null
)
