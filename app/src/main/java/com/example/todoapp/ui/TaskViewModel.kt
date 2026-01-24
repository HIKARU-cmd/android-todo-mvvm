package com.example.todoapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todoapp.data.Task
import com.example.todoapp.data.remote.FirestoreRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

sealed class SaveResult {
    data object Success: SaveResult()
    data object Timeout: SaveResult()
    data class Error(val throwable: Throwable): SaveResult()
}

class TaskViewModel(
    private val repo: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    // tasks:Flowの設計図 tasks = 「observeAll → map → List<Task>」という設計図
    val tasks: Flow<List<Task>> = repo.observeAll().map { list ->
        list.sortedWith(
            compareBy<Task> { t -> t.dueAt == null}         // 未設定は最後
                .thenBy { t -> t.dueAt ?: Long.MAX_VALUE }  // 期限は近い順
                .thenByDescending { t -> t.createdAt }      // 作成日は新しい順
        )
    }


    fun add(title: String) = viewModelScope.launch {
        if(title.isBlank()) return@launch
        repo.add(title)
    }

    fun deleteById(id: String) = viewModelScope.launch {
        repo.delete(id)
    }

    fun updateDone(id: String, done: Boolean) = viewModelScope.launch {
        repo.updateDone(id, done)
    }

    // UI(詳細画面)が「成功、タイムアウト、失敗」で分岐して画面遷移するためsuspendで結果を返す
    suspend fun taskSave(id: String, title:String, memo:String, dueAt:Long?, done: Boolean
    ) :SaveResult {
        return try {
            withTimeout(1500L) {
                repo.updateTask(id, title, memo, dueAt,done)
            }
            SaveResult.Success
        } catch (e: TimeoutCancellationException) {
            SaveResult.Timeout
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            SaveResult.Error(e)
        }
    }
}