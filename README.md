# ScalaReplDocCommands

This is a little “proof of concept” or MVP candidate
to see if having commands like these to view Scaladoc
and Scala source code in the REPL would be good:

| Command                   | Description  |
| ------------------------- | ------------- |
| `doc("List")`             | show the `List` class Scaladoc |
| `doc("List", "foldLeft")` | show methods that match `foldLeft` in the `List` Scaladoc |
| `src("Vector")`           | show the source code for the `Vector` class |
| `open("LazyList")`        | open the `LazyList` class Scaladoc in the default browser |

Note that the `open` command currently only works on MacOS systems, because it requires a little bit of AppleScript code to open the default browser on a Mac system.

This little project was inspired by a Gitter discussion that
[started here](https://gitter.im/lampepfl/dotty?at=5fac172ddc70b5159a06e74d)
and [ended here](https://gitter.im/lampepfl/dotty?at=5fac2e86c6fe0131d4ec65a6).


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

- The start the REPL with that alias:

````
$ repl
````

Inside the REPL, use the `doc`, `src`, and `open` commands shown above.















