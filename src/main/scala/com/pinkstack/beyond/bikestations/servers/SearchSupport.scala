package com.pinkstack.beyond.bikestations.servers

import cats.data.*
import cats.effect.IO
import com.pinkstack.beyond.bikestations.Search.{Query, Size}
import com.pinkstack.beyond.bikestations.{Destination, Search}
import eu.timepit.refined.*
import eu.timepit.refined.cats.syntax.*
import eu.timepit.refined.types.numeric.PosInt
import eu.timepit.refined.types.string.NonEmptyString
import io.circe.*
import org.http4s.circe.CirceEntityEncoder.*
import org.http4s.dsl.io.*
import org.http4s.*
import org.typelevel.log4cats.LoggerFactory

trait SearchSupport:
  import Json.{fromDoubleOrNull, fromFields, fromInt, fromString, fromValues}
  given logger: LoggerFactory[IO]

  protected given QueryParamDecoder[Search.Query] = { case QueryParameterValue(value) =>
    NonEmptyString.validateNel(value).leftMap { e =>
      NonEmptyList.one(ParseFailure.apply(s"boom - $e", "Failed."))
    }
  }

  protected given QueryParamDecoder[Search.Size] = { case QueryParameterValue(value) =>
    PosInt.validateNel(value.toInt).leftMap { e =>
      NonEmptyList.one(ParseFailure.apply(s"boom - $e", "Failed."))
    }
  }

  protected object SearchQuery extends QueryParamDecoderMatcher[Search.Query]("query")
  protected object SizeQuery   extends QueryParamDecoderMatcher[Search.Size]("size")

  protected given Encoder[List[Destination]] = Encoder.instance { (destinations: List[Destination]) =>
    fromFields(Seq("results" -> fromValues(destinations.map { destination =>
      fromFields(
        List(
          "name"        -> fromString(destination.station.name),
          "free_stands" -> fromInt(destination.station.bikeStandFree),
          "distance"    -> fromDoubleOrNull(destination.distance),
          "latitude"    -> fromDoubleOrNull(destination.station.latitude),
          "longitude"   -> fromDoubleOrNull(destination.station.longitude)
        ) ++ destination.duration.toList.map(duration => "duration" -> fromDoubleOrNull(duration))
      )
    })))
  }

  protected val handleError: PartialFunction[Throwable, IO[Response[IO]]] = { case th: Throwable =>
    logger.getLogger.error(th)(s"Served error - $th") *>
      Ok(Json.fromFields(Seq("error" -> Json.fromString(th.getMessage))))
        .map(_.withStatus(Status.InternalServerError))
  }
