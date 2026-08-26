package com.example.githubappstore.data.remote

import com.example.githubappstore.data.model.GhRelease
import com.example.githubappstore.data.model.GhRepo
import com.example.githubappstore.data.model.GhSearchResponse
import com.example.githubappstore.data.model.GhUser
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

/** GitHub REST API (v3) for repositories and release assets. */
interface GitHubApiService {

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @GET("search/repositories")
    suspend fun searchRepos(
        @Query("q") query: String,
        @Query("sort") sort: String = "stars",
        @Query("order") order: String = "desc",
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1,
        @Header("Authorization") auth: String? = null
    ): GhSearchResponse

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @GET("repos/{owner}/{repo}")
    suspend fun getRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Header("Authorization") auth: String? = null
    ): GhRepo

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @GET("repos/{owner}/{repo}/releases")
    suspend fun listReleases(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 10,
        @Header("Authorization") auth: String? = null
    ): List<GhRelease>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @GET("user/starred")
    suspend fun listStarred(
        @Query("sort") sort: String = "updated",
        @Query("direction") direction: String = "desc",
        @Query("per_page") perPage: Int = 100,
        @Query("page") page: Int = 1,
        @Header("Authorization") auth: String? = null
    ): List<GhRepo>

    @Headers("Accept: application/vnd.github+json", "X-GitHub-Api-Version: 2022-11-28")
    @GET("user")
    suspend fun getAuthenticatedUser(@Header("Authorization") auth: String? = null): GhUser
}
