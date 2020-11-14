package com.alvinalexander.repl_docs

import sttp.client3._
import scala.concurrent.duration._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}

object NetworkUtils {
    def getContentFromUrl(url: String): Either[String,String] = {
        val backend = HttpURLConnectionBackend(
            options = SttpBackendOptions.connectionTimeout(5.seconds)
        )
        val response = basicRequest
                          .get(uri"$url")
                          .send(backend)
        response.body
    }

}