package com.alvinalexander.repl_docs

import sttp.client3._
import scala.concurrent.duration._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import scala.collection.mutable.ArrayBuffer

//TODO need something like a `more` and/or `limit` command for long output
//TODO methods like head, tail, take
//TODO add a `methods` command that lists all of the methods for a class
//TODO work for other classes besides immutable collections
//TODO add a command to jump to source on Github
//TODO refactor the new `vi/edit` command
//TODO add a `doc_help` command or something like that

object ReplDocCommandsMain extends App {
    import ReplDocCommands.{doc,src,open,edit}
    // println("\n___VECTOR___")
    // doc("Vector")
    // open("LazyList")
    // println("\n___LAZYLIST::withFilter___")
    // doc("LazyList", "withFilter")
    // src("Vector")
    edit("Vector")
}

object ReplDocCommands {

    // TODO this one needs some formatting work for sure; output is long and ugly
    // given just a class name
    def doc(aScalaClassName: String): Unit = {
        println("getting docs ...")
        val body: Either[String,String] = retrieveScaladocHtml(aScalaClassName)
        //TODO handle the Left case
        val htmlString = body.getOrElse("")
        val doc: Document = Jsoup.parse(htmlString)
        val formatter = new JsoupFormatter
        val plainText = formatter.getPlainText(doc)
        //TODO figure this out
        //plainText.split("\n").map(line => s"\n$line").toSeq
        //plainText.split("\n").toSeq
        //Seq(plainText)
        println(plainText)
    }

    // given a class name and method
    def doc(aScalaClassName: String, methodName: String): Unit = {
        println("getting docs ...")
        val body = retrieveScaladocHtml(aScalaClassName)
        //TODO handle the Left case
        val htmlString = body.getOrElse("")
        val methodsSeq = extractClassMethods(htmlString, methodName)
        println(methodsSeq.mkString)
    }

    /**
      * Return the source code for a given class name.
      */
    def src(aScalaClassName: String): Unit = {
        // get the scaladoc url
        val scaladocUrl = getUrlForClassname(aScalaClassName)
        // println(s"scaladocUrl: $scaladocUrl")

        // get the html from that page
        val scaladocHtml = getHtmlFromUrl(scaladocUrl).getOrElse("")
        // println(s"scaladocHtml: $scaladocHtml")

        // get the source code url from the scaladoc html
        val sourceCodeUrl = getSourceCodeUrl(scaladocHtml)
        println(s"sourceCodeUrl: $sourceCodeUrl")

        val sourceCode: Either[String,String] = getHtmlFromUrl(sourceCodeUrl)
        // println(s"sourceCode: ${sourceCode.getOrElse("").take(100)}")

        //TODO handle the Left case
        val errorMsg = s"""
          |Sorry, could not get the source code from this URL:
          |$sourceCodeUrl""".stripMargin
        val htmlString = sourceCode.getOrElse(errorMsg)
        println(htmlString)
    }

    /**
      * Return the source code for a given class name.
      */
    def edit(aScalaClassName: String): Unit = {
        // get the scaladoc url
        val scaladocUrl = getUrlForClassname(aScalaClassName)
        // println(s"scaladocUrl: $scaladocUrl")

        // get the html from that page
        val scaladocHtml = getHtmlFromUrl(scaladocUrl).getOrElse("")
        // println(s"scaladocHtml: $scaladocHtml")

        // get the source code url from the scaladoc html
        val sourceCodeUrl = getSourceCodeUrl(scaladocHtml)
        println(s"sourceCodeUrl: $sourceCodeUrl")

        val sourceCode: Either[String,String] = getHtmlFromUrl(sourceCodeUrl)
        // println(s"sourceCode: ${sourceCode.getOrElse("").take(100)}")

        //TODO handle the Left case
        val errorMsg = s"""
          |Sorry, could not get the source code from this URL:
          |$sourceCodeUrl""".stripMargin
        val htmlString = sourceCode.getOrElse(errorMsg)

        // write to a temp file
        val filename = s"/Users/al/tmp/${aScalaClassName}.txt"
        import java.io._
        val pw = new PrintWriter(new File(filename))
        pw.write(htmlString)
        pw.close

        // open the temp file with AppleScript and TextEdit
        val appleScriptCmd = s"""
            |set p to "$filename" 
            |set a to POSIX file p
            |tell application "Finder"
            |    open a
            |end tell""".stripMargin
        val runtime = Runtime.getRuntime
        val code = Array("osascript", "-e", appleScriptCmd)
        val process = runtime.exec(code)

            //         |tell application "TextEdit"
            // |    activate
            // |    open targetFilepath
            // |end tell""".stripMargin


        // import sys.process._
        // val cmd = Seq(
        //     "/bin/sh",
        //     "-c",
        //     s"""echo "Hello, world" > foo.scala && vi foo.scala"""
        // ).!!
        //     // s"echo $htmlString | vi -"
        //     // s"vim <(echo $htmlString)"

    }


    def open(aScalaClassName: String): Unit = {
        val scaladocUrl = getUrlForClassname(aScalaClassName)
        println(s"OPENING $scaladocUrl ...\n")
        // Thread.sleep(1000)
        val appleScriptCmd = s"""open location "$scaladocUrl" """
        val runtime = Runtime.getRuntime
        val code = Array("osascript", "-e", appleScriptCmd)
        val process = runtime.exec(code)
    }


    private def extractClassMethods(htmlString: String, methodName: String): Seq[String] = {
        val doc: Document = Jsoup.parse(htmlString)
        val lines = ArrayBuffer[String]()
        doc.select("li.indented0").eachText.forEach(s => { lines += s"\n\n$s" })
        val matchingLines = lines.filter(_.contains(methodName))
        matchingLines.toSeq
    }

    /**
      * @return Returns an Either, with the HTML body in the Right.
      */
    private def retrieveScaladocHtml(aScalaClassName: String): Either[String,String] = {
        val url = getUrlForClassname(aScalaClassName)
        getHtmlFromUrl(url)
    }

    private def getHtmlFromUrl(url: String): Either[String,String] = {
        val backend = HttpURLConnectionBackend(
            options = SttpBackendOptions.connectionTimeout(5.seconds)
        )
        val response = basicRequest
                          .get(uri"$url")
                          .send(backend)
        response.body
    }

    private def getSourceCodeUrl(scaladocHtml: String): String = {
        val doc: Document = Jsoup.parse(scaladocHtml)
        val links = doc.select("a[href]")  //links: Elements (many on one page)
        import scala.jdk.CollectionConverters._
        val stream = links.stream.filter(elem => elem.attr("href").startsWith("https://github.com/scala/scala/tree/")) 
        val optionalResult = stream.findFirst
        val sourceCodeUrl = if (optionalResult.isPresent) optionalResult.get.attr("href") else ""
        // println(s"sourceCodeUrl: $sourceCodeUrl")

        val lastPartOf1stSourceUrl1 = sourceCodeUrl.split("https://github.com/scala/scala/tree/")(1)
        val lastPartOf1stSourceUrl2 = lastPartOf1stSourceUrl1.split("#")(0)
        val newSourceCodeUrl = s"https://raw.githubusercontent.com/scala/scala/${lastPartOf1stSourceUrl2}"
        newSourceCodeUrl
    }


    private def getUrlForClassname(aScalaClassName: String): String = {
        s"https://www.scala-lang.org/api/current/scala/collection/immutable/${aScalaClassName}.html"
    }

}






