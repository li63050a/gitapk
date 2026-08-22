package com.git.app.log

import android.content.Context
import android.util.Log as AndroidLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

const val LEVEL_DEBUG = 0
const val LEVEL_INFO = 1
const val LEVEL_WARN = 2
const val LEVEL_ERROR = 3

data class LogEntry(
    val level: Int,
    val time: Long,
    val tag: String,
    val message: String,
    val throwable: String?
) {
    val timeText: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(time))

    val levelText: String
        get() = when (level) {
            LEVEL_DEBUG -> "D"
            LEVEL_INFO -> "I"
            LEVEL_WARN -> "W"
            else -> "E"
        }
}

object Log {
    private const val MAX_RAM_ENTRIES = 2000
    private const val LINE_REGEX = """^(\d{2}:\d{2}:\d{2}\.\d{3})\s+([DIWE])/(.+?):\s?(.*)$"""

    var maxFileBytes: Long = 0L
    var maxFiles: Int = 0

    private val queue = ConcurrentLinkedQueue<LogEntry>()
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries

    private var logDir: File? = null
    private var currentFile: File? = null

    fun init(context: Context) {
        logDir = File(context.filesDir, "logs").apply { mkdirs() }
        currentFile = logDir?.let { File(it, logFileName()) }
        loadHistory()
    }

    fun configure(maxBytes: Long, maxFiles: Int) {
        this.maxFileBytes = maxBytes
        this.maxFiles = maxFiles
    }

    fun logDirFile(): File? = logDir

    fun d(tag: String, message: String) = log(LEVEL_DEBUG, tag, message, null)

    fun i(tag: String, message: String) = log(LEVEL_INFO, tag, message, null)

    fun w(tag: String, message: String) = log(LEVEL_WARN, tag, message, null)

    fun e(tag: String, message: String, throwable: Throwable? = null) = log(LEVEL_ERROR, tag, message, throwable)

    private fun log(level: Int, tag: String, message: String, throwable: Throwable?) {
        val stack = throwable?.stackTraceToString()
        when (level) {
            LEVEL_DEBUG -> AndroidLog.d(tag, message)
            LEVEL_INFO -> AndroidLog.i(tag, message)
            LEVEL_WARN -> AndroidLog.w(tag, message)
            else -> AndroidLog.e(tag, message, throwable)
        }
        val entry = LogEntry(level, System.currentTimeMillis(), tag, message, stack)
        queue.add(entry)
        while (queue.size > MAX_RAM_ENTRIES) queue.poll()
        synchronized(this) {
            _entries.value = queue.toList()
        }
        writeFile(entry)
    }

    private fun writeFile(entry: LogEntry) {
        runCatching {
            val file = currentFile ?: return
            if (maxFileBytes > 0 && file.length() > maxFileBytes) rotate()
            val sb = StringBuilder()
            sb.append(entry.timeText).append(' ').append(entry.levelText).append('/')
                .append(entry.tag).append(": ").append(entry.message).append('\n')
            entry.throwable?.let { sb.append(it).append('\n') }
            file.appendText(sb.toString())
        }
    }

    private fun rotate() {
        val dir = logDir ?: return
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".log") && !f.name.startsWith("crash_") }
            ?.sortedBy { it.name }?.toMutableList() ?: return
        while (maxFiles > 0 && files.size >= maxFiles) {
            files.removeFirstOrNull()?.delete()
        }
        currentFile = File(dir, logFileName())
    }

    private fun logFileName(): String =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    private fun loadHistory() {
        val dir = logDir ?: return
        val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val base = System.currentTimeMillis()
        val files = dir.listFiles { f ->
            f.isFile && f.name.endsWith(".log") && !f.name.startsWith("crash_")
        }?.sortedBy { it.name } ?: return
        val parsed = ArrayList<LogEntry>()
        for (f in files) {
            runCatching {
                f.readText().lineSequence().forEach { line ->
                    parseLine(line, timeFmt, base)?.let { parsed.add(it) }
                }
            }
        }
        if (parsed.size > MAX_RAM_ENTRIES) {
            parsed.subList(0, parsed.size - MAX_RAM_ENTRIES).clear()
        }
        queue.addAll(parsed)
        synchronized(this) { _entries.value = queue.toList() }
    }

    private fun parseLine(line: String, timeFmt: SimpleDateFormat, base: Long): LogEntry? {
        val m = Regex(LINE_REGEX).matchEntire(line) ?: return null
        val (timeStr, levelStr, tag, msg) = m.destructured
        val level = when (levelStr) {
            "D" -> LEVEL_DEBUG
            "I" -> LEVEL_INFO
            "W" -> LEVEL_WARN
            else -> LEVEL_ERROR
        }
        val time = runCatching { timeFmt.parse(timeStr)?.time ?: base }.getOrDefault(base)
        return LogEntry(level, time, tag, msg, null)
    }

    fun deleteCrash(file: File): Boolean = runCatching { file.delete() }.getOrDefault(false)

    fun clear() {
        queue.clear()
        synchronized(this) { _entries.value = emptyList() }
        runCatching {
            logDir?.listFiles { f ->
                f.isFile && f.name.endsWith(".log") && !f.name.startsWith("crash_")
            }?.forEach { it.delete() }
            currentFile = logDir?.let { File(it, logFileName()) }
        }
    }
}

class CrashHandler : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        runCatching {
            val dir = Log.logDirFile() ?: return
            val file = File(dir, "crash_${System.currentTimeMillis()}.log")
            file.writeText(
                "时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n" +
                    "线程: ${thread.name}\n${throwable.stackTraceToString()}"
            )
        }
        Log.e("Crash", "未捕获异常: ${throwable.message}", throwable)
        defaultHandler?.uncaughtException(thread, throwable)
    }
}
