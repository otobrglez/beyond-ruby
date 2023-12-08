package com.pinkstack.beyond.bikestations.apps

import cats.data.ValidatedNel
import cats.effect.*
import cats.syntax.all.*
import com.comcast.ip4s.*
import com.monovore.decline.effect.CommandIOApp
import com.monovore.decline.{Argument, Opts}
import com.pinkstack.beyond.bikestations.servers.{ServerOne, ServerTwo}
import org.http4s.*

object ServerApp extends CommandIOApp(name = "server", header = "Server"):
  given Argument[Port] = new Argument[Port] {
    override def read(string: String): ValidatedNel[String, Port] = Port.fromString(string) match {
      case None       => "Provided port is invalid".invalidNel[Port]
      case Some(port) => port.validNel
    }

    override def defaultMetavar: String = "port"
  }

  override def main: Opts[IO[ExitCode]] =
    val pathOpts: Opts[Port] = Opts.option[Port]("port", "Server port", short = "P").withDefault(port"4444")
    pathOpts.map { port =>
      val nextPort = Port.fromInt(port.toString.toInt + 1).get
      (ServerOne.server(port), ServerTwo.server(nextPort))
        .parMapN((_, _) => ())
        .useForever
        .as(ExitCode.Success)
    }
