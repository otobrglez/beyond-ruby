package com.pinkstack.beyond.bikestations.apps

import cats.effect.IO
import com.typesafe.config.ConfigFactory
import io.circe.Decoder.Result
import io.circe.{Decoder, HCursor}
import io.circe.config.syntax.*
import io.circe.derivation.Configuration
import io.circe.generic.auto.*
import org.http4s.Uri
import cats.implicits.*
import eu.timepit.refined.collection.NonEmpty
import eu.timepit.refined.*
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto.*
import eu.timepit.refined.collection.NonEmpty
import com.monovore.decline.refined.*
import eu.timepit.refined.boolean.{And, Not}
import eu.timepit.refined.numeric.*
import com.pinkstack.beyond.bikestations.apps.Config.ApiKey

final case class Config private (
  prominfoQueryEndpoint: Uri,
  positionstackApiKey: ApiKey,
  openrouteApiKey: ApiKey,
  prominfoGeometry: String
)

object Config:
  type ApiKey = String Refined NonEmpty
  given Decoder[Uri] = Decoder[String].emap(Uri.fromString(_).leftMap(_.message))

  given Decoder[ApiKey] = Decoder[String].emap(raw => refineV[NonEmpty](raw))

  final val load: IO[Config] = ConfigFactory.load.asF[IO, Config]
