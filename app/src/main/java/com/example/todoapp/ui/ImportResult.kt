package com.example.todoapp.ui

sealed class ImportResult {
    data class Success(
        val title: String,
        val memo: String,
        val done: Boolean
    ): ImportResult()

    object Timeout: ImportResult()
    object Empty: ImportResult()
    data class Error(val throwable: Throwable): ImportResult()
}