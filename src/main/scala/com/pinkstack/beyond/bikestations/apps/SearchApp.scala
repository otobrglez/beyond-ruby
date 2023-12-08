package com.pinkstack.beyond.bikestations.apps

import cats.effect.{ExitCode, IO}
import cats.syntax.all.*
import com.monovore.decline.*
import com.monovore.decline.effect.*
import com.monovore.decline.refined.*
import com.pinkstack.beyond.bikestations.Search
import com.pinkstack.beyond.bikestations.Search.{Query as Q, Size}
import eu.timepit.refined.*
import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.Not
import eu.timepit.refined.numeric.*

object SearchApp
    extends CommandIOApp(
      name = "search",
      header = "Search nearest"
    ):

  def main: Opts[IO[ExitCode]] =
    val queryOpts            = Opts.argument[Q](metavar = "query")
    val sizeOpts: Opts[Size] = Opts
      .option[Size]("size", help = "Number of records")
      .withDefault(3.asInstanceOf[Int Refined Positive])

    (queryOpts, sizeOpts).mapN { case (query, size) =>
      IO.println(s"Query: ${query} with size ${size}") *>
        Search.withConfig.use { search =>
          for
            results <- search.nearDuration(query, size)
            _       <- IO(results.foreach(result => println(result)))
          yield ()
        } *>
        IO.pure(ExitCode.Success)
    }
