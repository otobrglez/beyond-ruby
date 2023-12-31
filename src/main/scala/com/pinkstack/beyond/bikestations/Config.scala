package com.pinkstack.beyond.bikestations

import cats.effect.IO
import cats.data.NonEmptyList
import cats.implicits.*
import com.monovore.decline.refined.*
import com.pinkstack.beyond.bikestations.Config.*
import com.pinkstack.beyond.bikestations.Config
import com.typesafe.config.ConfigFactory
import eu.timepit.refined.*
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto.*
import eu.timepit.refined.boolean.{And, Not}
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.numeric.*
import io.circe.Decoder.Result
import io.circe.config.syntax.*
import io.circe.derivation.Configuration
import io.circe.generic.auto.*
import io.circe.{Decoder, HCursor}
import org.http4s.Uri

final case class Config private (
  prominfoQueryEndpoint: Uri,
  positionstackApiKeys: NonEmptyList[ApiKey],
  openrouteApiKeys: NonEmptyList[ApiKey],
  prominfoGeometry: ProminfoGeometry
) {
  val positionstackApiKey: ApiKey = positionstackApiKeys.head
  val openrouteApiKey: ApiKey     = openrouteApiKeys.head
}

object Config:
  type ApiKey           = String Refined NonEmpty
  type ProminfoGeometry = String Refined NonEmpty
  given Decoder[Uri] = Decoder[String].emap(Uri.fromString(_).leftMap(_.message))
  given Decoder[String Refined NonEmpty] = Decoder[String].emap { raw =>
    refineV[NonEmpty](raw)
  }

  given Decoder[NonEmptyList[ApiKey]] = Decoder[String].emap(
    _.split("\\|").toList.map(s => refineV[NonEmpty](s)).sequence match
      case Left(value)  => Left(value)
      case Right(value) => NonEmptyList.fromList(value).toRight("Can't build list.")
  )

  final val load: IO[Config] = ConfigFactory.load.asF[IO, Config]
