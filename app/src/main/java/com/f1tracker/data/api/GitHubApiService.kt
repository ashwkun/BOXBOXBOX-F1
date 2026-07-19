package com.f1tracker.data.api

import com.f1tracker.data.models.GitHubRelease
import retrofit2.http.GET

interface GitHubApiService {
    @GET("repos/boxcreate/boxboxbox/releases/latest")
    suspend fun getLatestRelease(): GitHubRelease
}
