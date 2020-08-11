// GZip Compression

import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._

val service = HttpRoutes.of[IO] {
  case _ =>
    Ok("I repeat myself when I'm under stress. " * 3)
}

val request = Request[IO](Method.GET, uri"/")

val response = service.orNotFound(request).unsafeRunSync()

val body = response.as[String].unsafeRunSync()
body.length

// Now we can wrap the service in the GZip middleware.

import org.http4s.server.middleware._
val zipService = GZip(service)

val response2 = zipService.orNotFound(request).unsafeRunSync()

val body2 = response2.as[String].unsafeRunSync()
body2.length

// So far, there was no change. That’s because the caller needs to inform us
// that they will accept GZipped responses via an Accept-Encoding header.
// Acceptable values for the Accept-Encoding header are “gzip”, “x-gzip”, and “*”.

val acceptHeader = Header("Accept-Encoding", "gzip")
val zipRequest   = request.putHeaders(acceptHeader)

val ioResponse3 = zipService.orNotFound(zipRequest)
val response3   = ioResponse3.unsafeRunSync()

val body3 = response3.as[String].unsafeRunSync()
body3.length

// response3
//   .body
//   .through(fs2.compress.gunzip[IO](4096))
//   .through(fs2.text.utf8Decode[IO])
//   .through(fs2.text.lines)
//   .compile
//   .string
//   .unsafeRunSync()

import cats.data.EitherT
import cats.syntax.either._

val gzipDec: EntityDecoder[IO, String] =
  EntityDecoder.decodeBy(MediaType.text.plain) { (m: Media[IO]) =>
    EitherT[IO, DecodeFailure, String] {
      m.as[String].map(s => s.asRight[DecodeFailure])
    }
  }

gzipDec.matchesMediaType(MediaType.text.plain)
// ioResponse3.flatMap(gzipDec.).unsafeRunSync()
