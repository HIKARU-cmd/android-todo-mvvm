package com.example.todoapp

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.todoapp.data.Task
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import android.util.Log
import androidx.activity.viewModels
import com.example.todoapp.ui.SaveResult
import com.example.todoapp.ui.TaskViewModel

class TaskDetailActivity : AppCompatActivity() {

    // Intentで値を渡すためのkeyを設定
    companion object {
        private const val EXTRA_TASK_ID = "task_id"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_MEMO = "memo"
        private const val EXTRA_DUE_AT = "dueAt"
        private const val EXTRA_DONE = "done"

        // 呼び出し側から使用するhelper
        fun start(context: Context, task:Task) {
            val intent = Intent(context,TaskDetailActivity::class.java).apply {
                putExtra(EXTRA_TASK_ID, task.id)
                putExtra(EXTRA_TITLE, task.title)
                putExtra(EXTRA_MEMO, task.memo)
                if (task.dueAt != null) {               // nullの場合はputExtraを呼ばない　keyが存在しない状態にする
                    putExtra(EXTRA_DUE_AT, task.dueAt)
                }
                putExtra(EXTRA_DONE, task.done)
            }
            context.startActivity(intent)
        }
    }

    // 画面上のUI部品操作のための変数
    private lateinit var editTitle: EditText
    private lateinit var editMemo: EditText
    private lateinit var textDue: TextView
    private lateinit var buttonChangeDue: Button
    private lateinit var buttonClearDue: Button
    private lateinit var buttonSave: Button
    private lateinit var checkDone: CheckBox

    // 詳細画面に渡された値を保持する変数
    private var taskId: String = ""
    private var dueAt: Long? = null
    private var done: Boolean = false

    private val viewModel: TaskViewModel by viewModels()


    // intentから取り出した値の入れ物
    private data class TaskArgs(
        val taskId: String,
        val title: String,
        val memo: String,
        val dueAt: Long?,
        val done: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail)

        setupActionBar()
        bindViews()

        val args = loadFromIntent()
        taskId = args.taskId
        dueAt = args.dueAt
        done = args.done

        render(args)
        setupListeners()

    }


    // 戻るボタン
    private fun setupActionBar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    // ViewをfindViewById
    private fun bindViews() {
        editTitle = findViewById(R.id.editTitle)
        editMemo = findViewById(R.id.editMemo)
        textDue = findViewById(R.id.textDue)
        buttonChangeDue = findViewById(R.id.buttonChangeDue)
        buttonClearDue = findViewById(R.id.buttonClearDue)
        buttonSave = findViewById(R.id.buttonSave)
        checkDone = findViewById(R.id.checkDone)
    }

    // Intentから値を取り出す
    private fun loadFromIntent(): TaskArgs {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: ""
        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val memo = intent.getStringExtra(EXTRA_MEMO) ?: ""
        dueAt = if (intent.hasExtra(EXTRA_DUE_AT)) {
            intent.getLongExtra(EXTRA_DUE_AT, 0L)   // Longはnullを許容できないため、hasExtraで未設定を区別
        } else {
            null
        }
        done = intent.getBooleanExtra(EXTRA_DONE, false)

        return TaskArgs(
            taskId = taskId,
            title = title,
            memo = memo,
            dueAt = dueAt,
            done = done
        )
    }

    // 詳細画面遷移後の画面描画
    private fun render(args: TaskArgs) {
        editTitle.setText(args.title)
        editMemo.setText(args.memo)
        checkDone.isChecked = done
        textDue.text = formatDueText(dueAt)
    }

    private fun setupListeners() {
        // 完了フラグの変更
        checkDone.setOnCheckedChangeListener {_,isChecked->   // 保存ボタンでfirebaseに保存する仕様
            done = isChecked
        }

        // 期限変更ボタン　
        buttonChangeDue.setOnClickListener {
            showDatePicker { pickedMillis ->
                dueAt = pickedMillis
                textDue.text = formatDueText(dueAt)
            }
        }

        // 期限クリアボタン　
        buttonClearDue.setOnClickListener {
            dueAt = null
            textDue.text = formatDueText(dueAt)
            toast("期限をクリアしました")
        }

        // 保存ボタン
        buttonSave.setOnClickListener {
            onclickSave()
        }
    }

    private fun onclickSave() {
        val newTitle = editTitle.text.toString().trim()
        val newMemo = editMemo.text.toString().trim()

        val error = validateInput(newTitle, newMemo)
        if (error != null) {
            toast(error)
            return
        }

        buttonSave.isEnabled = false    // 後に活用
        lifecycleScope.launch {
            when(val result =
                viewModel.taskSave(taskId, newTitle, newMemo, dueAt, done)
            ) {
                SaveResult.Success -> {
                    toast("保存しました")
                    finish()
                }
                SaveResult.Timeout -> {
                    toast("オフラインの可能性があります。接続後に同期されます")
                    finish()
                }
                is SaveResult.Error -> {
                    Log.d("TaskDetail", "save failed", result.throwable)
                    toast("保存に失敗しました。通信状態を確認してください")
                    buttonSave.isEnabled = true
                }
            }
        }
    }

    // 期限表示用のフォーマット
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.JAPAN)
    private fun formatDueText(dueAt: Long?): String {
        return if(dueAt == null){
            "期限：未設定"
        } else {
            val date = Date(dueAt)
            val formatted = dateFormat.format(date)
            "期限：$formatted"
        }
    }

    // タスク期限の設定
    private fun showDatePicker(onPicked: (Long?) -> Unit) {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, y, m, d ->
                cal.set(Calendar.YEAR, y)
                cal.set(Calendar.MONTH, m)
                cal.set(Calendar.DAY_OF_MONTH, d)
                onPicked(cal.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // アクションバー左上の「←戻る」ボタンが押されたときの処理
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // バリデーション関数
    private fun validateInput(title: String, memo: String): String? {
        if(title.isBlank()) return "タイトルを入力してください"
        if(title.length > 50) return "タイトルは50文字以内にしてください"
        if(memo.length > 300) return "メモは300文字以内にしてください"
        return null
    }

    // トーストをまとめる
    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}