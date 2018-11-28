package de.miniserver.input

import de.mein.Lok
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class InputPipeReader private constructor(val workingDirectory: File, val fileName: String) {
    fun stop() {
        try {
            process.destroyForcibly()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            pipeFile.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }


    private lateinit var process: Process

    private var pipeFile: File = File(workingDirectory, fileName)

    init {
        if (pipeFile.exists())
            pipeFile.delete()

        /// build pipe
        ProcessBuilder("mkfifo", pipeFile.absolutePath).start().waitFor()
        Lok.debug("pipe created ${pipeFile.absolutePath}")

        GlobalScope.launch {
            //build cat
            val b = ProcessBuilder("cat", pipeFile.absolutePath)
            b.redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
            process = b.start()
            Lok.debug("cat started on ${pipeFile.absolutePath}")
            val input = process.inputStream.read()
            Lok.debug("stopping, read: $input")
            if (input > 0)
                System.exit(0)
        }

    }

    companion object {
        const val STOP_FILE_NAME = "stop.input"
        fun create(workingDirectory: File, fileName: String): InputPipeReader {
            return InputPipeReader(workingDirectory, fileName)
        }
    }
}