package com.example.todoapp.data.remote
import retrofit2.http.GET

interface ApiService {

    @GET("todos")
    suspend fun getTodos(): List<TodoResponse>

    @GET("posts")
    suspend fun getPosts(): List<PostResponse>
}
