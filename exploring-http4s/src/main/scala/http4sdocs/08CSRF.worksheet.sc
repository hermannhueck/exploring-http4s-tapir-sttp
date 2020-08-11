// CSRF

import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.middleware._

val service = HttpRoutes.of[IO] {
  case _ =>
    Ok()
}

val request = Request[IO](Method.GET, uri"/")

service.orNotFound(request).unsafeRunSync()

val cookieName                                 = "csrf-token"
val key                                        = CSRF.generateSigningKey[IO].unsafeRunSync()
val defaultOriginCheck: Request[IO] => Boolean =
  CSRF.defaultOriginCheck[IO](_, "localhost", Uri.Scheme.http, None)
val csrfBuilder                                = CSRF[IO, IO](key, defaultOriginCheck)

val csrf = csrfBuilder
  .withCookieName(cookieName)
  .withCookieDomain(Some("localhost"))
  .withCookiePath(Some("/"))
  .build

val dummyRequest: Request[IO] =
  Request[IO](method = Method.GET)
    .putHeaders(Header("Origin", "http://localhost"))

val resp =
  csrf
    .validate()(service.orNotFound)(dummyRequest)
    .unsafeRunSync()

val cookie = resp.cookies.head

val dummyPostRequest: Request[IO] =
  Request[IO](method = Method.POST)
    .putHeaders(
      Header("Origin", "http://localhost"),
      Header("X-Csrf-Token", cookie.content)
    )
    .addCookie(RequestCookie(cookie.name, cookie.content))

val validateResp = csrf.validate()(service.orNotFound)(dummyPostRequest).unsafeRunSync()
