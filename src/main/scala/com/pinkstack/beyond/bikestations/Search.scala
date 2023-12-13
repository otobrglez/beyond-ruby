package com.pinkstack.beyond.bikestations

import cats.effect.{IO, Resource}
import cats.implicits.*
import com.pinkstack.beyond.bikestations.Search.*
import com.pinkstack.beyond.bikestations.clients.*
import com.pinkstack.beyond.bikestations.clients.OpenrouteClient.Profile.*
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import eu.timepit.refined.*
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto.*
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.Positive

type Longitude = Double
type Latitude  = Double
type Distance  = Double
type Duration  = Double
trait Location { def longitude: Longitude; def latitude: Latitude }
final case class CurrentLocation(name: String, longitude: Longitude, latitude: Latitude)             extends Location
final case class Station(name: String, bikeStandFree: Int, longitude: Longitude, latitude: Latitude) extends Location
final case class Direction(distance: Distance, duration: Duration, longitude: Longitude, latitude: Latitude)
    extends Location
final case class Destination(station: Station, distance: Distance, duration: Option[Duration] = None)

object Destination:
  def from(origin: Location)(f: (Location, Location) => Distance)(destination: Station): Destination =
    Destination(destination, distance = f(origin, destination))

  def fromF(origin: Location)(f: (Location, Location) => Distance)(destination: Station): IO[Destination] =
    IO(from(origin)(f)(destination))

object Distance:
  def haversine(a: Location, b: Location): Double = Haversine.distance(a.latitude, a.longitude, b.latitude, b.longitude)

final class Search private (using
  config: Config,
  client: Client[IO]
):
  def near(query: Query, size: Size): IO[List[Destination]] =
    for
      stations <- StationsClient.all
      location <- PositionStackClient.query(query)
      sorted = stations
        .filter(_.bikeStandFree > 0)
        .map(Destination.from(origin = location)(Distance.haversine))
        .sortBy(_.distance)
        .take(size)
    yield sorted

  def nearPar(query: Query, size: Size): IO[List[Destination]] =
    (
      StationsClient.all,
      PositionStackClient.query(query)
    ).parMapN { case (stations, location) =>
      stations
        .filter(_.bikeStandFree > 0)
        .map(Destination.from(location)(Distance.haversine))
        .sortBy(_.distance)
        .take(size)
    }

  def nearParSeq(query: Query, size: Size): IO[List[Destination]] =
    (
      StationsClient.getAll(config, client),
      PositionStackClient.getLocation(config, client, query)
    ).parFlatMapN { case (stations, location) =>
      stations
        .filter(_.bikeStandFree > 0)
        .parTraverse(Destination.fromF(location)(Distance.haversine))
        .map(_.sortBy(_.distance).take(size))
    }

  def nearDuration(
    query: Query,
    size: Size
  ): IO[List[Destination]] =
    (
      StationsClient.all,
      PositionStackClient.query(query)
    ).parFlatMapN { case (stations, location) =>
      stations
        .filter(_.bikeStandFree > 0)
        .parTraverse(Destination.fromF(location)(Distance.haversine))
        .map(_.map(_.station))
        .flatMap(
          _.parTraverse(station =>
            OpenrouteClient
              .direction(location)(station)
              .map(direction => Destination(station, direction.distance, Some(direction.duration)))
          )
        )
        .map(_.sortBy(_.duration).take(size))
    }

  def nearDuration2(
    query: Query,
    size: Size,
    scopeFactor: Double = 1.6
  ): IO[List[Destination]] =
    (
      StationsClient.all,
      PositionStackClient.query(query)
    ).parFlatMapN { case (stations, location) =>
      stations
        .filter(_.bikeStandFree > 0)
        .parTraverse(Destination.fromF(location)(Distance.haversine))
        .map(_.sortBy(_.distance).take((size * scopeFactor).ceil.toInt).map(_.station))
        .flatMap(
          _.parTraverse(station =>
            OpenrouteClient
              .direction(location)(station)
              .map(direction => Destination(station, direction.distance, Some(direction.duration)))
          )
        )
        .map(_.sortBy(_.duration).take(size))
    }

object Search:
  type Query = String Refined NonEmpty
  type Size  = Int Refined Positive

  private given loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

  val withConfig: Resource[IO, Search] =
    for
      config <- Config.load.toResource
      client <- EmberClientBuilder.default[IO].build
    yield {
      given c: Config      = config
      given cl: Client[IO] = client
      new Search
    }

  def fromConfig(config: Config): Resource[IO, Search] =
    for client <- EmberClientBuilder.default[IO].build
    yield {
      given c: Config      = config
      given cl: Client[IO] = client
      new Search
    }
