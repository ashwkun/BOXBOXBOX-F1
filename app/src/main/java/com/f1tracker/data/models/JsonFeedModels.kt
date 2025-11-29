package com.f1tracker.data.models

import com.google.gson.annotations.SerializedName

data class JsonFeedResponse(
    @SerializedName("items") val items: List<JsonFeedItem>
)

data class JsonFeedItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("content_html") val contentHtml: String,
    @SerializedName("url") val url: String,
    @SerializedName("date_modified") val dateModified: String,
    @SerializedName("attachments") val attachments: List<JsonFeedAttachment>?
)

data class JsonFeedAttachment(
    @SerializedName("url") val url: String,
    @SerializedName("mime_type") val mimeType: String
)
