// CORS

import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._

val service = HttpRoutes.of[IO] {
  case _ =>
    Ok()
}

val request = Request[IO](Method.GET, uri"/")

service.orNotFound(request).unsafeRunSync()

import org.http4s.server.middleware._

val corsService = CORS(service)

corsService.orNotFound(request).unsafeRunSync()

val originHeader = Header("Origin", "https://somewhere.com")

val corsRequest = request.putHeaders(originHeader)

corsService.orNotFound(corsRequest).unsafeRunSync()

val googleGet = Request[IO](Method.GET, uri"/", headers = Headers.of(Header("Origin", "https://google.com")))
val yahooPut  = Request[IO](Method.PUT, uri"/", headers = Headers.of(Header("Origin", "https://yahoo.com")))
val duckPost  = Request[IO](Method.POST, uri"/", headers = Headers.of(Header("Origin", "https://duckduckgo.com")))

import scala.concurrent.duration._

val methodConfig = CORSConfig(
  anyOrigin = true,
  anyMethod = false,
  allowedMethods = Some(Set("GET", "POST")),
  allowCredentials = true,
  maxAge = 1.day.toSeconds
)

val corsMethodSvc = CORS(service, methodConfig)

corsMethodSvc.orNotFound(googleGet).unsafeRunSync()
corsMethodSvc.orNotFound(yahooPut).unsafeRunSync()
corsMethodSvc.orNotFound(duckPost).unsafeRunSync()

val originConfig = CORSConfig(
  anyOrigin = false,
  allowedOrigins = Set("https://yahoo.com", "https://duckduckgo.com"),
  allowCredentials = false,
  maxAge = 1.day.toSeconds
)

val corsOriginSvc = CORS(service, originConfig)

corsOriginSvc.orNotFound(googleGet).unsafeRunSync()
corsOriginSvc.orNotFound(yahooPut).unsafeRunSync()
corsOriginSvc.orNotFound(duckPost).unsafeRunSync()
