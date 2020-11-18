name := "ScalaReplDocCommands"
version := "0.2"
scalaVersion := "2.13.3"

libraryDependencies ++= Seq(
    "com.softwaremill.sttp.client3" %% "okhttp-backend" % "3.0.0-RC6",
    "org.jsoup" % "jsoup" % "1.13.1"
)

// see https://tpolecat.github.io/2017/04/25/scalac-flags.html for scalacOptions descriptions
scalacOptions ++= Seq(
    "-deprecation",     //emit warning and location for usages of deprecated APIs
    "-unchecked",       //enable additional warnings where generated code depends on assumptions
    "-explaintypes",    //explain type errors in more detail
    "-Ywarn-dead-code", //warn when dead code is identified
    "-Xfatal-warnings"  //fail the compilation if there are any warnings
)
    
