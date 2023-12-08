package com.pinkstack.beyond.bikestations

import cats.implicits._
import io.circe.{Decoder, Json}

object decoders:
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

  given Decoder[Direction] = Decoder[Json].emap { json =>
    (for {
      routesJson <- json.hcursor.downField("routes").as[List[Json]]
      routes     <- routesJson.traverse { json =>
        for
          distance <- json.hcursor.downField("summary").downField("distance").as[Double]
          duration <- json.hcursor.downField("summary").downField("duration").as[Double]
        yield distance -> duration
      }
    } yield routes.headOption).leftMap(_.toString).flatMap {
      case Some(distance -> duration) => Right(Direction(distance, duration, -1, -1))
      case None                       => Left("No routes.")
    }
  }
