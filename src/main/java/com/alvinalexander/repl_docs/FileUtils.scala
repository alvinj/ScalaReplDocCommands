package com.alvinalexander.repl_docs

import java.io._

object FileUtils {
    def writeToFile(filename: String, s: String): Unit = {
        val pw = new PrintWriter(new File(filename))
        pw.write(s)
        pw.close
    }
}