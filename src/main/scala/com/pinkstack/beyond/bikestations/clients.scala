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

object clients:
  object StationsClient:
    import decoders.given

    val getAll: (Config, Client[IO]) => IO[List[Station]] = (config, client) =>
      client.expect[List[Station]](
        config.prominfoQueryEndpoint +?
          ("f", "json") +?
          ("outFields", "*") +?
          ("outSr", "4326") +?
          ("geometry", config.prominfoGeometry)
      )

    def all(using config: Config, client: Client[IO]): IO[List[Station]] = getAll(config, client)

  object PositionStackClient:
    import decoders.given

    private val endpoint: Uri = Uri.unsafeFromString("http://api.positionstack.com/v1/forward")
    val getLocation: (Config, Client[IO], Query) => IO[CurrentLocation] = (config, client, query) =>
      client.expect[CurrentLocation](
        endpoint +? ("access_key", config.positionstackApiKey.toString) +? ("query", query.toString)
      )

    def query(query: Query)(using config: Config, client: Client[IO]): IO[CurrentLocation] =
      getLocation(config, client, query)

  object OpenrouteClient:
    private val endpoint: Uri = Uri.unsafeFromString("https://api.openrouteservice.org/v2")
    import decoders.given

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
      client.expect[Direction](
        Request[IO](
          method = Method.POST,
          headers = Headers(
            Header.Raw(ci"Authorization", config.openrouteApiKey.toString),
            Accept(MediaType.application.json)
          ),
          uri = endpoint / "directions" / profile.name
        ).withEntity(payload(origin, destination))
      )

    def direction(origin: Location, profile: Profile = Profile.Walking)(destination: Location)(using
      config: Config,
      client: Client[IO]
    ): IO[Direction] =
      getDirection(config, client, origin, destination, profile)

    def getDirectionsWithDestination(
      config: Config,
      client: Client[IO],
      origin: Location,
      destination: Location,
      profile: Profile = Profile.Walking
    ): IO[(Location, Direction)] =
      getDirection(config, client, origin, destination, profile)
        .map(direction => destination -> direction)
