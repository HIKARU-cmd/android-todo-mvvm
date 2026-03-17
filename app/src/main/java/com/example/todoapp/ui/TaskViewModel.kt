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
import com.example.todoapp.data.remote.JsonPlaceholderRepository
import kotlinx.coroutines.withTimeout

sealed class SaveResult {
    data object Success: SaveResult()
    data object Timeout: SaveResult()
    data class Error(val throwable: Throwable): SaveResult()
}

class TaskViewModel(
    private val repo: FirestoreRepository = FirestoreRepository(),
    private val jsonPlaceholderRepository: JsonPlaceholderRepository = JsonPlaceholderRepository()
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

    // jsonplaceholderサーバーよりサンプルタスクを取得
    suspend fun importSampleTask(): ImportResult {
        return try {
            withTimeout(3000L) {
                val todos = jsonPlaceholderRepository.getTodos()
                val posts = jsonPlaceholderRepository.getPosts()

                if(todos.isEmpty() || posts.isEmpty()) {
                    return@withTimeout ImportResult.Empty
                }

                val todo = todos.first()
                val post = posts.first()

                ImportResult.Success(
                    title = todo.title,
                    memo = post.body,
                    done = todo.completed
                )
            }
        } catch (e: TimeoutCancellationException) {
            ImportResult.Timeout
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ImportResult.Error(e)
        }
    }

    // firestoreへサンプルタスクを保存
    suspend fun importAndSaveSampleTask(): ImportResult {
        return when(val result = importSampleTask()) {
            is ImportResult.Success -> {
                repo.addTask(
                    title = result.title,
                    memo = result.memo,
                    done = result.done,
                    dueAt = null
                )
                result
            }
            ImportResult.Timeout -> ImportResult.Timeout
            ImportResult.Empty -> ImportResult.Empty
            is ImportResult.Error -> result
        }
    }
}