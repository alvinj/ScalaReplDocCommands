package com.alvinalexander.repl_docs

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import scala.collection.mutable.ArrayBuffer
import NetworkUtils.getContentFromUrl
import AppleScriptUtils.runApplescriptCommand
import FileUtils.writeToFile

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

    val tempDir = "/Users/al/tmp"
    val scaladocPrefixUrl = "https://www.scala-lang.org/api/current/scala/collection/immutable"
    val githubSourceUrl = "https://github.com/scala/scala/tree"
    val githubSourceUrlRaw = "https://raw.githubusercontent.com/scala/scala"

    /**
      * print some help text
      */
    def help: Unit =
        println(s"""
        |Available Documentation Commands
        |--------------------------------
        |doc("List")              show the List class Scaladoc
        |doc("List", "foldLeft")  show methods that match foldLeft in the List Scaladoc
        |src("Vector")            show the source code for the Vector class
        |open("LazyList")         open the LazyList class Scaladoc in the default browser
        """.stripMargin)

    /**
      * Given a class name return all of the text from the 
      * correct Scaladoc page.
      */
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
        println(onlyGoodChars(plainText))
    }

    /**
      * Given a class name and a string to search for, return the matching
      * lines from the Scaladoc page.
      */
    def doc(aScalaClassName: String, stringToSearchFor: String): Unit = {
        println("getting docs ...")
        val body = retrieveScaladocHtml(aScalaClassName)
        //TODO handle the Left case
        val htmlString = body.getOrElse("")
        val methodsSeq = searchClassForStringOccurrences(htmlString, stringToSearchFor)
        println(onlyGoodChars(methodsSeq.mkString))
    }

    /**
      * Return the source code for a given class name.
      */
    def src(aScalaClassName: String): Unit = {
        val sourceCode: Either[String,String] = 
            getSourceCodeFromGithub(aScalaClassName: String)

        //TODO handle the Left case
        val errorMsg = "Sorry, could not get the source code."
        val htmlString = sourceCode.getOrElse(errorMsg)
        println(htmlString)
    }

    /**
      * Open the Scaladoc page for the given class name ("List")
      * in your default browser.
      * @param aScalaClassName A simple name like "List" or "Vector".
      */
    def open(aScalaClassName: String): Unit = {
        val scaladocUrl = getScaladocUrlForClassname(aScalaClassName)
        println(s"OPENING $scaladocUrl ...\n")
        runApplescriptCommand(s"""open location "$scaladocUrl" """)
    }

    /**
     * Get the source code for the given class name and open it
     * in your default editor.
     * @param aScalaClassName A simple name like "List" or "Vector".
     */
    def edit(aScalaClassName: String): Unit = {
        val sourceCode: Either[String,String] = 
            getSourceCodeFromGithub(aScalaClassName: String)

        //TODO handle the Left case
        val errorMsg = "Sorry, could not get the source code."
        val htmlString = sourceCode.getOrElse(errorMsg)

        // write to a temp file
        val filename = s"${tempDir}/${aScalaClassName}.txt"
        writeToFile(filename, htmlString)

        // open the temp file with AppleScript and TextEdit.
        // note: vi did not like it when i tried to open it inside the repl.
        val appleScriptCmd = s"""
            |set p to "$filename" 
            |set a to POSIX file p
            |tell application "Finder"
            |    open a
            |end tell""".stripMargin
        runApplescriptCommand(appleScriptCmd)
    }

    /**
      * Given a Scala class name, go to its Scaladoc page, find its
      * Github source code URL from that page, then get the source 
      * code from that Github page.
      */
    private def getSourceCodeFromGithub(aScalaClassName: String): Either[String,String] = {
        val scaladocUrl = getScaladocUrlForClassname(aScalaClassName)
        val scaladocHtml = getContentFromUrl(scaladocUrl).getOrElse("")
        val githubSourceCodeUrl = getGithubSourceCodeUrl(scaladocHtml)
        // TODO this helps for debugging atm
        println(s"sourceCodeUrl: $githubSourceCodeUrl")
        getContentFromUrl(githubSourceCodeUrl)

    }

    /**
      * Search for all occurrences of the given string in the given HTML.
      * The code searches for a CSS class that’s in the LI tag of Scaladoc
      * pages, and this tag is used for all methods within that class.
      * Therefore, this method is specific to the current Scala 2.x Scaladoc
      * page formatting.
      *
      * @param htmlString The HTML, presumably from a Scaladoc page.
      * @param stringToSearchFor Typically a method name, i.e., you want to search for
      *                          a method within a class.
      * @return
      */
    private def searchClassForStringOccurrences(htmlString: String, stringToSearchFor: String): Seq[String] = {
        val doc: Document = Jsoup.parse(htmlString)
        val lines = ArrayBuffer[String]()
        // the css class to search for
        doc.select("li.indented0").eachText.forEach(s => { lines += s"\n\n$s" })
        val matchingLines = lines.filter(_.contains(stringToSearchFor))
        matchingLines.toSeq
    }

    /**
      * @return Returns an Either, with the HTML body in the Right.
      */
    private def retrieveScaladocHtml(aScalaClassName: String): Either[String,String] = {
        val url = getScaladocUrlForClassname(aScalaClassName)
        getContentFromUrl(url)
    }

    /**
      * Given the HTML from a Scaladoc page, this method finds the link to the
      * Github source code for whatever class this HTML comes from. It then
      * does whatever it needs to do to get the URL for the Github “raw” source
      * code page for the Scala class that belongs to this HTML.
      *
      * @param scaladocHtml The Scaladoc HTML for a given class, e.g., the
      *                     HTML from the Scaladoc page for the `List` class.
      * @return The URL for the Github “raw” source code page.
      */
    private def getGithubSourceCodeUrl(scaladocHtml: String): String = {
        val doc: Document = Jsoup.parse(scaladocHtml)
        val links = doc.select("a[href]")  //links: Elements (many on one page)
        import scala.jdk.CollectionConverters._
        val stream = links.stream.filter(elem => elem.attr("href").startsWith(s"$githubSourceUrl")) 
        val optionalResult = stream.findFirst
        val sourceCodeUrl = if (optionalResult.isPresent) optionalResult.get.attr("href") else ""
        // println(s"sourceCodeUrl: $sourceCodeUrl")

        val lastPartOf1stSourceUrl1 = sourceCodeUrl.split(s"${githubSourceUrl}/")(1)
        val lastPartOf1stSourceUrl2 = lastPartOf1stSourceUrl1.split("#")(0)
        val newSourceCodeUrl = s"${githubSourceUrlRaw}/${lastPartOf1stSourceUrl2}"
        newSourceCodeUrl
    }

    /**
      * TODO This currently only works for classes in the scala.collection.immutable
      * package.
      *
      * @param aScalaClassName A simple class name like "List" or "Vector".
      * @return A canonical URL for the correct Scaladoc page.
      */
    // 
    private def getScaladocUrlForClassname(aScalaClassName: String): String = {
        s"${scaladocPrefixUrl}/${aScalaClassName}.html"
    }

    /**
      * TODO the string i receive has some non-printable characters in it,
      * and i want to get rid of those.
      */
    private def onlyGoodChars(s: String): String = {
        val goodChars = (' ' to '~').toList ++ List('\n', '\r', '\f')
        s.filter(c => goodChars.contains(c)).trim
    }

}






