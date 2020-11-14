package com.alvinalexander.repl_docs

object AppleScriptUtils {
    def runApplescriptCommand(cmd: String): Unit = {
        val runtime = Runtime.getRuntime
        val code = Array("osascript", "-e", cmd)
        val process = runtime.exec(code)
    }
}
