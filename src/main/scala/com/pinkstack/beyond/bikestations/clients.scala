package com.pinkstack.beyond.bikestations

import cats.effect.IO
import com.pinkstack.beyond.bikestations
import io.circe.Json
import org.http4s.*
import org.http4s.circe.CirceEntityDecoder.*
import org.http4s.client.Client
import org.http4s.headers.{Accept, Authorization}
import org.typelevel.ci.CIStringSyntax
import org.http4s.circe.*
import com.pinkstack.beyond.bikestations.Search.{Query, Size}
import eu.timepit.refined.auto.autoUnwrap
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory

object clients:
  object StationsClient:
    import decoders.given
    given loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]
    private val logger                     = loggerFactory.getLogger

    val getAll: (Config, Client[IO]) => IO[List[Station]] = (config, client) =>
      client
        .expect[List[Station]](
          config.prominfoQueryEndpoint +?
            ("f", "json") +?
            ("outFields", "*") +?
            ("outSr", "4326") +?
            ("geometry", config.prominfoGeometry.toString)
        )
        .flatTap(stations => logger.info(s"Collected ${stations.size} stations."))

    def all(using config: Config, client: Client[IO]): IO[List[Station]] = getAll(config, client)

  object PositionStackClient:
    import decoders.given
    given loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]
    private val logger                     = loggerFactory.getLogger

    private val endpoint: Uri = Uri.unsafeFromString("http://api.positionstack.com/v1/forward")
    val getLocation: (Config, Client[IO], Query) => IO[CurrentLocation] = (config, client, query) =>
      client
        .expect[CurrentLocation](
          endpoint +? ("access_key", config.positionstackApiKey.toString) +? ("query", query.toString)
        )
        .flatTap(currentLocation => logger.info(s"Decoded: \"$query\" as $currentLocation"))

    def query(query: Query)(using config: Config, client: Client[IO]): IO[CurrentLocation] =
      getLocation(config, client, query)

  object OpenrouteClient:
    import decoders.given
    given loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]
    private val logger                     = loggerFactory.getLogger
    private val endpoint: Uri              = Uri.unsafeFromString("https://api.openrouteservice.org/v2")

    enum Profile(val name: String):
      case Walking extends Profile("foot-walking")
      case Driving extends Profile("not-implemented")

    private def payload(
      origin: Location,
      destination: Location
    ): Json =
      Json.fromFields(
        Seq(
          "coordinates" -> Json.fromValues(
            Seq(
              Json.fromValues(Seq(origin.longitude, origin.latitude).map(Json.fromDoubleOrNull)),
              Json.fromValues(Seq(destination.longitude, destination.latitude).map(Json.fromDoubleOrNull))
            )
          )
        )
      )

    def getDirection(
      config: Config,
      client: Client[IO],
      origin: Location,
      destination: Location,
      profile: Profile = Profile.Walking
    ): IO[Direction] =
      client
        .expect[Direction](
          Request[IO](
            method = Method.POST,
            headers = Headers(
              Header.Raw(ci"Authorization", config.openrouteApiKey.toString),
              Accept(MediaType.application.json)
            ),
            uri = endpoint / "directions" / profile.name
          ).withEntity(payload(origin, destination))
        )
        .flatTap(direction =>
          logger.info(
            s"Collected direction ${origin} -> ${destination} - " +
              s"distance: ${direction.distance}, " +
              s"duration: ${direction.duration}"
          )
        )

    def direction(origin: Location, profile: Profile = Profile.Walking)(destination: Location)(using
      config: Config,
      client: Client[IO]
    ): IO[Direction] =
      getDirection(config, client, origin, destination, profile)
