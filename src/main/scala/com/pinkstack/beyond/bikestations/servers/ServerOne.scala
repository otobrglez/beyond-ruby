package com.pinkstack.beyond.bikestations.servers

import cats.data.*
import cats.effect.{IO, Resource}
import IO.raiseError
import com.comcast.ip4s.*
import com.pinkstack.beyond.bikestations.{Config, Search}
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.{HttpRoutes, Response}
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

final case class ServerOne private (
  search: Search
) extends SearchSupport:
  given logger: LoggerFactory[IO] = Slf4jFactory.create[IO]

  private val app: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / version / "near" :? SearchQuery(query) :? SizeQuery(size) =>
      version match
        case "v1"   => Ok(search.near(query, size)).recoverWith(handleError)
        case "v2"   => Ok(search.nearPar(query, size)).recoverWith(handleError)
        case "v2-2" => Ok(search.nearParSeq(query, size)).recoverWith(handleError)
        case "v3"   => Ok(search.nearDuration(query, size)).recoverWith(handleError)
        case x      => Ok(raiseError(new RuntimeException(s"Unsupported version $x"))).recoverWith(handleError)
  }

object ServerOne:
  given logger: LoggerFactory[IO]                                = Slf4jFactory.create[IO]
  def server(port: Port): Resource[IO, org.http4s.server.Server] =
    for
      config <- Config.load.toResource
      search <- Search.fromConfig(config)
      app = ServerOne(search).app.orNotFound
      server <- EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port)
        .withHttpApp(app)
        .build
        .evalTap(_ => logger.getLogger.info(s"Booted server on port $port"))
    yield server
