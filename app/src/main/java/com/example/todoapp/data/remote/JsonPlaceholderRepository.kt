package com.example.todoapp.data.remote

import com.example.todoapp.data.PostResponse
import com.example.todoapp.data.TodoResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class JsonPlaceholderRepository {

    private val api: ApiService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(ApiService::class.java)
    }

    suspend fun getTodos(): List<TodoResponse> {
        return api.getTodos()
    }

    suspend fun getPosts(): List<PostResponse> {
        return api.getPosts()
    }
}