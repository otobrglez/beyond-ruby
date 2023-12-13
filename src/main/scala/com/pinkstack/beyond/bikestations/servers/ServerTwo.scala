package com.pinkstack.beyond.bikestations.servers

import cats.effect.{IO, Ref, Resource}
import cats.syntax.all.*
import com.comcast.ip4s.*
import com.pinkstack.beyond.bikestations.*
import com.pinkstack.beyond.bikestations.Search.{Query, Size}
import com.pinkstack.beyond.bikestations.clients.{OpenrouteClient, PositionStackClient, StationsClient}
import eu.timepit.refined.auto.autoUnwrap
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.io.*
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

import scala.concurrent.duration.*

final case class ServerTwo private (search: (Query, Size) => IO[List[Destination]]) extends SearchSupport:
  given logger: LoggerFactory[IO] = Slf4jFactory.create[IO]
  private val app: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "v5" / "near" :? SearchQuery(query) :? SizeQuery(size) =>
      Ok(search(query, size)).recoverWith(handleError)
  }

object ServerTwo:
  given logger: LoggerFactory[IO] = Slf4jFactory.create[IO]

  def server(port: Port): Resource[IO, org.http4s.server.Server] =
    for
      config       <- Config.load.toResource
      client       <- EmberClientBuilder.default[IO].build
      
      stationsList <- Ref.empty[IO, List[Station]].toResource

      refreshing = StationsClient
        .getAll(config, client)
        .flatTap(stationsList.set)
        .delayBy(10.seconds)
        .foreverM

      searchApp = ServerTwo { (query, size) =>
        (
          stationsList.get,
          PositionStackClient.getLocation(config, client, query)
        ).parFlatMapN { case (stations, location) =>
          stations
            .filter(_.bikeStandFree > 0)
            .parTraverse(Destination.fromF(location)(Distance.haversine))
            .map(_.sortBy(_.distance).take((size * 1.4).ceil.toInt).map(_.station))
            .flatMap(
              _.parTraverse(station =>
                OpenrouteClient
                  .getDirection(config, client, location, station)
                  .map(direction => Destination(station, direction.distance, Some(direction.duration)))
              )
            )
            .map(_.sortBy(_.duration).take(size))
        }
      }.app.orNotFound

      server <- (
        refreshing.toResource,
        EmberServerBuilder.default[IO].withHost(ipv4"0.0.0.0").withPort(port).withHttpApp(searchApp).build
      ).parMapN((_, server) => server)
    yield server
