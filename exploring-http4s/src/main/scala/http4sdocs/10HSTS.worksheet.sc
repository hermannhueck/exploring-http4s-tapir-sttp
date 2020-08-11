// HSTS

import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import cats.effect.IO

val service = HttpRoutes.of[IO] {
  case _ =>
    Ok("ok")
}

val request = Request[IO](Method.GET, uri"/")

val response = service.orNotFound(request).unsafeRunSync()
response.headers

import org.http4s.server.middleware._

val hstsService = HSTS(service)
val response2   = hstsService.orNotFound(request).unsafeRunSync()

response2.headers

import org.http4s.headers._
import scala.concurrent.duration._

val hstsHeader   = `Strict-Transport-Security`.unsafeFromDuration(30.days, includeSubDomains = true, preload = true)
val hstsService3 = HSTS(service, hstsHeader)

val response3 = hstsService3.orNotFound(request).unsafeRunSync()
response3.headers
