package com.pinkstack.beyond.bikestations.apps

import cats.effect.{ExitCode, IO, Resource}
import com.monovore.decline.*
import com.monovore.decline.effect.*
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}
import org.typelevel.log4cats.slf4j.Slf4jFactory
import cats.syntax.all.*
import com.pinkstack.beyond.bikestations.apps.Search.{Query, Size}
import eu.timepit.refined.*
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto.*
import eu.timepit.refined.collection.NonEmpty
import com.monovore.decline.refined.*
import com.pinkstack.beyond.bikestations.Haversine
import eu.timepit.refined.boolean.{And, Not}
import eu.timepit.refined.numeric.*
import io.circe.Json
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import io.circe.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.circe.CirceEntityDecoder.*

trait Location { def longitude: Float; def latitude: Float }
final case class CurrentLocation(name: String, longitude: Float, latitude: Float)             extends Location
final case class Station(name: String, bikeStandFree: Int, longitude: Float, latitude: Float) extends Location
final case class Destination(station: Station, geoDistance: Double)

object JSONDecoders:
  given Decoder[List[Station]] = Decoder[Json].emap { json =>
    (for
      features <- json.hcursor.downField("features").as[List[Json]]
      stations <- features.traverse { json =>
        for
          name          <- json.hcursor.downField("attributes").downField("name").as[String]
          bikeStandFree <- json.hcursor.downField("attributes").downField("bike_stand_free").as[Int]
          longitude     <- json.hcursor.downField("geometry").downField("x").as[Float]
          latitude      <- json.hcursor.downField("geometry").downField("y").as[Float]
        yield Station(name, bikeStandFree, longitude, latitude)
      }
    yield stations).leftMap(_.toString)
  }

  given Decoder[CurrentLocation] = Decoder[Json].emap { json =>
    (for
      data      <- json.hcursor.downField("data").as[List[Json]]
      locations <- data.traverse { json =>
        for
          name      <- json.hcursor.downField("name").as[String]
          latitude  <- json.hcursor.downField("latitude").as[Float]
          longitude <- json.hcursor.downField("longitude").as[Float]
        yield CurrentLocation(name, longitude, latitude)
      }
    yield locations.headOption).leftMap(_.toString).flatMap {
      case Some(v) => Right(v)
      case None    => Left("No locations...")
    }
  }

object StationsClient:
  import JSONDecoders.given
  val getAll: (Config, Client[IO]) => IO[List[Station]] = (config, client) =>
    client.expect[List[Station]](
      config.prominfoQueryEndpoint +?
        ("f", "json") +?
        ("outFields", "*") +?
        ("outSr", "4326") +?
        ("geometry", config.prominfoGeometry)
    )

object PositionStackClient:
  import JSONDecoders.given
  private val endpoint: Uri = Uri.unsafeFromString("http://api.positionstack.com/v1/forward")
  val getLocation: (Config, Client[IO], String) => IO[CurrentLocation] = (config, client, query) =>
    client.expect[CurrentLocation](
      endpoint +? ("access_key", config.positionstackApiKey.toString) +? ("query", query)
    )

object Distance:
  def haversine(a: Location, b: Location): Double = Haversine.distance(a.latitude, a.longitude, b.latitude, b.longitude)

final class Search private (config: Config, client: Client[IO]):
  def near(query: Query, size: Size): IO[List[Destination]] =
    for
      stations <- StationsClient.getAll(config, client).map(_.filter(_.bikeStandFree > 0))
      location <- PositionStackClient.getLocation(config, client, query.toString)
      sorted   <- IO:
        stations
          .map(station => Destination(station, Distance.haversine(location, station)))
          .sortBy(_.geoDistance)
          .take(size)
    yield sorted

object Search:
  type Query = String Refined NonEmpty
  type Size  = Int Refined Positive

  private given loggerFactory: LoggerFactory[IO] = Slf4jFactory.create[IO]

  val fromConfig: Resource[IO, Search] =
    for
      config <- Config.load.toResource
      client <- EmberClientBuilder.default[IO].build
    yield new Search(config, client)

object SearchApp
    extends CommandIOApp(
      name = "search",
      header = "Search nearest"
    ):

  def main: Opts[IO[ExitCode]] =
    val queryOpts            = Opts.argument[Query](metavar = "query")
    val sizeOpts: Opts[Size] = Opts
      .option[Size]("size", help = "Number of records")
      .withDefault(3.asInstanceOf[Int Refined Positive])

    (queryOpts, sizeOpts).mapN { case (query, size) =>
      IO.println(s"Query: ${query} with size ${size}") *>
        Search.fromConfig.use { search =>
          for
            results <- search.near(query, size)
            _       <- IO(results.foreach(result => println(result)))
          yield ()
        } *>
        IO.pure(ExitCode.Success)
    }
