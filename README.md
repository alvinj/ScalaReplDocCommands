# ScalaReplDocCommands

This is a little “proof of concept” (POC) or MVP candidate
to see if having commands like these to view Scaladoc
and Scala source code in the REPL would be good:

| Command                   | Description  |
| ------------------------- | ------------- |
| `doc("List")`             | show the `List` class Scaladoc |
| `doc("List", "foldLeft")` | show methods that match `foldLeft` in the `List` Scaladoc |
| `src("Vector")`           | show the source code for the `Vector` class |
| `open("LazyList")`        | open the `LazyList` class Scaladoc in the default browser |
| `editor("Vector")`        | open the `Vector` class in your default ".txt" editor |

Note that the `open` command currently only works on MacOS systems, because it requires a little bit of AppleScript code to open the default browser on a Mac system.

This little project was inspired by a Gitter discussion that
[started here](https://gitter.im/lampepfl/dotty?at=5fac172ddc70b5159a06e74d)
and [ended here](https://gitter.im/lampepfl/dotty?at=5fac2e86c6fe0131d4ec65a6).


## Disclaimer

Most of this code is written in a “worst practices” style because I just
want to see if a tool like this is worth developing, so I’ve taken a lot
of shortcuts, and often don’t handle possible errors.


## Configuration

The following commands should work on Mac and Unix/Linux systems.

Once you create a JAR file with `sbt assembly`, follow these steps:

- Create a _/Users/al/tmp_ directory (change this to match a directory on your system)
- Copy the JAR file to that directory (the JAR is currently named _ScalaReplDocCommands-assembly-0.1.jar_)
- Create a file named _repl-commands_ in that directory
- Put this content in that file:

```scala
import com.alvinalexander.repl_docs.ReplDocCommands.{doc,src,open}
```

- Create an alias to start the REPL:

````
alias repl="scala -cp ScalaReplDocCommands-assembly-0.1.jar -i _/Users/al/tmp/repl-commands_"
````

- Then start the REPL with that alias:

````
$ repl
````

Inside the REPL, use the `doc`, `src`, `open`, and `editor` commands shown above.


## Discussion

As mentioned, I created this as a POC/MVP candidate to see if this is a good idea. The easiest way for me to do this was to get the docs off the internet, from URLs like this one:

- _https://www.scala-lang.org/api/current/scala/collection/immutable/List.html_

Everything after that is screen-scraping with Jsoup. A much better (i.e., real) implementation would probably read from the source code JAR files. But, this was much easier for me to do quickly.


## Jsoup

Some useful Jsoup URLs:

- [DOM navigation](https://jsoup.org/cookbook/extracting-data/dom-navigation)
- [Cookbook](https://jsoup.org/cookbook/)
- [Selector-style syntax to find elements](https://jsoup.org/cookbook/extracting-data/selector-syntax)
- [Extracting links](https://jsoup.org/cookbook/extracting-data/example-list-links)
- [`Elements` Javadoc](https://jsoup.org/apidocs/org/jsoup/select/Elements.html)
- [`Element` Javadoc](https://jsoup.org/apidocs/org/jsoup/nodes/Element.html)

