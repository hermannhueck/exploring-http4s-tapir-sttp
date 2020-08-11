// Entity Handling

// Why Entity*

// Construction and Media Types

// Chaining Decoders

import org.http4s._
import org.http4s.headers.`Content-Type`
import org.http4s.dsl.io._
import cats._, cats.effect._, cats.implicits._, cats.data._

sealed trait Resp
case class Audio(body: String) extends Resp
case class Video(body: String) extends Resp

val response = Ok("").map(_.withContentType(`Content-Type`(MediaType.audio.ogg)))

val audioDec         = EntityDecoder.decodeBy(MediaType.audio.ogg) { (m: Media[IO]) =>
  EitherT {
    m.as[String].map(s => Audio(s).asRight[DecodeFailure])
  }
}
val videoDec         = EntityDecoder.decodeBy(MediaType.video.ogg) { (m: Media[IO]) =>
  EitherT {
    m.as[String].map(s => Video(s).asRight[DecodeFailure])
  }
}
implicit val bothDec = audioDec.widen[Resp] orElse videoDec.widen[Resp]

println(response.flatMap(_.as[Resp]).unsafeRunSync())

// Presupplied Encoders/Decoders
// The EntityEncoder/EntityDecoders shipped with http4s.

// Raw Data Types

// JSON

// With jsonOf for the EntityDecoder, and jsonEncoderOf for the EntityEncoder:

// argonaut: "org.http4s" %% "http4s-argonaut" % http4sVersion
// circe: "org.http4s" %% "http4s-circe" % http4sVersion
// json4s-native: "org.http4s" %% "http4s-json4s-native" % http4sVersion
// json4s-jackson: "org.http4s" %% "http4s-json4s-jackson" % http4sVersion

// XML

// Support for Twirl and Scalatags
