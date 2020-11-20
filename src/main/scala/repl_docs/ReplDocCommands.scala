package com.alvinalexander.repl_docs

import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import scala.collection.mutable.ArrayBuffer
import NetworkUtils.getContentFromUrl
import AppleScriptUtils._
import FileUtils.writeToFile
import scala.util.{Try,Success,Failure}

object ReplDocCommandsMain extends App {
    import ReplDocCommands._
    // println("\n___VECTOR___")
    // doc("Vector")
    browser("LazyList")
    // println("\n___LAZYLIST::withFilter___")
    // doc("LazyList", "withFilter")
    // src("Vector")
    // editor("Array")
    // help
}

object ReplDocCommands {

    val tempDir = "/Users/al/tmp"
    val githubSourceUrl = "https://github.com/scala/scala/tree"
    val githubSourceUrlRaw = "https://raw.githubusercontent.com/scala/scala"

    /**
      * print some help text
      */
    def help: Unit =
        println(s"""
        |Available Documentation Commands
        |--------------------------------
        |doc("List")                show the List class Scaladoc
        |doc("List", "withFilter")  grep for 'withFilter' in the methods of the List Scaladoc
        |src("Vector")              show the Github source code for the Vector class
        |browser("LazyList")        open the LazyList class Scaladoc in the default browser
        |editor("Vector")           open the Vector class in your default ".txt" editor
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
        val body: Either[String,String] = retrieveScaladocHtml(aScalaClassName)
        //TODO handle the Left case
        val htmlString = body.getOrElse("")
        val doc: Document = Jsoup.parse(htmlString)
        val listOfMethodsInPlainText = convertHtmlToListOfPlainTextMethods(doc)
        val matchingMethods = listOfMethodsInPlainText.filter(_.contains(stringToSearchFor))
        matchingMethods.foreach(s => println(s"\n${s.trim}"))
        println("\n")
    }

    /**
      * For the `Document` of the current Scaladoc page, go through each method on that
      * page, convert the HTML to decent-looking plain text, and then return a list
      * of strings, where each string is the documentation for a method. So, for a
      * Scaladoc collections page like `LazyList`, this method returns a list of
      * 276 elements, where each element is the documentation for a method.
      */
    private def convertHtmlToListOfPlainTextMethods(doc: Document): Seq[String] = {
        val listOfMethods = ArrayBuffer[String]()
        val methods: Elements = doc.select("li.indented0")   //Elements, 276 of them
        val plainTextLinesForCurrentMethod = ArrayBuffer[String]()
        methods.forEach{currentMethod: Element =>
            // an element is the beginning of the Scaladoc for a method.
            // create a multiline string that represents each element.
            currentMethod.children.forEach{child => 
                plainTextLinesForCurrentMethod += s"${onlyGoodChars(child.text)}\n"
            }
            val currentMethodAsMultilineString = plainTextLinesForCurrentMethod.mkString
            plainTextLinesForCurrentMethod.clear()
            listOfMethods += currentMethodAsMultilineString
        }
        listOfMethods.toSeq
    }


    private def printClassSearchMatches(methodsSeq: Seq[String]): Unit = {
        println("")
        println("Matches Found")
        println("-------------")
        println(onlyGoodChars(methodsSeq.mkString))
        println("")
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
    def browser(aScalaClassName: String): Unit = {
        val scaladocUrlTry = getScaladocUrlForClassname(aScalaClassName)
        scaladocUrlTry match {
            case Success(scaladocUrl) => 
                println(s"OPENING $scaladocUrl ...\n")
                runApplescriptCommand(s"""open location "$scaladocUrl" """)
            case Failure(e) => println(e.getMessage)
        }
    }

    /**
     * Get the source code for the given class name and open it
     * in your default editor.
     * @param aScalaClassName A simple name like "List" or "Vector".
     */
    def editor(aScalaClassName: String): Unit = {
        val sourceCodeEither: Either[String,String] = 
            getSourceCodeFromGithub(aScalaClassName: String)

        sourceCodeEither match {
            case Left(errorMsg) =>
                // val errorMsg = "Sorry, could not get the source code."
                println(errorMsg)
            case Right(sourceCode) =>
                // write to a temp file
                val filename = s"${tempDir}/${aScalaClassName}.txt"
                writeToFile(filename, sourceCode)
                AppleScriptUtils.openTxtFileWithDefaultEditor(filename)
        }
    }

    /**
      * Given a Scala class name, go to its Scaladoc page, find its
      * Github source code URL from that page, then get the source 
      * code from that Github page.
      */
    private def getSourceCodeFromGithub(aScalaClassName: String): Either[String,String] = {
        val scaladocUrlTry = getScaladocUrlForClassname(aScalaClassName)
        scaladocUrlTry match {
            case Success(scaladocUrl) =>
                // TODO use exception-handling here
                val scaladocHtml = getContentFromUrl(scaladocUrl).getOrElse("")
                val githubSourceCodeUrl = getGithubSourceCodeUrl(scaladocHtml)
                // TODO this helps for debugging atm
                println(s"sourceCodeUrl: $githubSourceCodeUrl")
                getContentFromUrl(githubSourceCodeUrl)
            case Failure(e) => Left(e.getMessage)
        }

    }

    // TODO make this private again
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
    def searchClassForStringOccurrences(htmlString: String, stringToSearchFor: String): Seq[String] = {
        val doc: Document = Jsoup.parse(htmlString)
        val lines = ArrayBuffer[String]()
        // the css class to search for
        doc.select("li.indented0").eachText.forEach(s => { lines += s"\n\n$s" })
        val matchingLines = lines.filter(_.contains(stringToSearchFor))
        matchingLines.toSeq
    }

    // TODO make this `private` again
    /**
      * @return Returns an Either, with the HTML body in the Right.
      */
    def retrieveScaladocHtml(aScalaClassName: String): Either[String,String] = {
        val urlTry = getScaladocUrlForClassname(aScalaClassName)
        urlTry match {
            case Success(url) => getContentFromUrl(url)
            case Failure(e)   => Left(e.getMessage)
        }        
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
    private def getScaladocUrlForClassname(className: String): Try[String] = {
        //s"${scaladocPrefixUrl}/${aScalaClassName}.html"
        val canonClassNameTry = classNameToCanonClassName(className)
        canonClassNameTry match {
            case Success(canonClassName) => Success(canonClassNameToUrl(canonClassName))
            case Failure(e) => Failure(e)
        }
    }

    // TODO make private again
    /**
      * the string i receive has some non-printable characters in it,
      * and i want to get rid of those.
      */
    def onlyGoodChars(s: String): String = {
        val goodChars = (' ' to '~').toList ++ List('\n', '\r', '\f')
        s.filter(c => goodChars.contains(c)).trim
    }

    /**
      * given a class name like "List", return "scala.collection.immutable.List".
      * given a class name like "Map", prompt the user for what they want
      * from a list of choices, and return that.
      */
    private def classNameToCanonClassName(className: String): Try[String] = {
        // lookup the className in the listOfClasses
        // if result.size == 0, return None
        // if result.size == 1, return Some(List)
        // if result.size > 1, prompt the user for what they want
        val listOfCanonNameMatches = Data.scalaClasses.filter(_.endsWith(s".${className}"))
        listOfCanonNameMatches.size match {
            case 0 => Failure(new Exception(s"Could not find the class '${className}'"))
            case 1 => Success(listOfCanonNameMatches.head)
            case _ => //TODO prompt the user for what they want
                      Failure(new Exception("Multiple matches found, can’t deal with that yet."))
        }
    }

    private def canonClassNameToUrl(canonClassName: String): String = {
        val uri = canonClassName.replaceAll("\\.", "/")
        val url = s"https://www.scala-lang.org/api/current/${uri}.html"
        url
    }

}






