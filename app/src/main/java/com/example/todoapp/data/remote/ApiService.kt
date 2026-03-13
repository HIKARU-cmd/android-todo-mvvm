package com.example.todoapp.data.remote
import retrofit2.http.GET
import com.example.todoapp.data.TodoResponse
import com.example.todoapp.data.PostResponse

interface ApiService {

    @GET("todos")
    suspend fun getTodos(): List<TodoResponse>

    @GET("posts")
    suspend fun getPosts(): List<PostResponse>
}
