package com.alvinalexander.repl_docs

object AppleScriptUtils {
    def runApplescriptCommand(cmd: String): Unit = {
        val runtime = Runtime.getRuntime
        val code = Array("osascript", "-e", cmd)
        val process = runtime.exec(code)
    }

    // i only used this to create the video
    def openUrlInSafari(url: String): Unit = {
        val cmd = s"""
            |tell application "Safari"
            |  open location "$url"
            |  activate
            |end tell""".stripMargin
        runApplescriptCommand(cmd)
    }

    def openTxtFileWithDefaultEditor(filename: String): Unit = {
        val appleScriptCmd = s"""
            |set p to "$filename" 
            |set a to POSIX file p
            |tell application "Finder"
            |    open a
            |end tell""".stripMargin
        runApplescriptCommand(appleScriptCmd)

    }
}
