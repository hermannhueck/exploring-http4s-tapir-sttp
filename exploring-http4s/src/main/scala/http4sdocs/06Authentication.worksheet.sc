// Authentication
// -- Built in

import cats._, cats.effect._, cats.implicits._, cats.data._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.server._

case class User(id: Long, name: String)

val authUser: Kleisli[OptionT[IO, *], Request[IO], User] =
  Kleisli(_ => OptionT.liftF(IO(???)))

val middleware: AuthMiddleware[IO, User] =
  AuthMiddleware(authUser)

val authedRoutes: AuthedRoutes[User, IO] =
  AuthedRoutes.of {
    case GET -> Root / "welcome" as user => Ok(s"Welcome, ${user.name}")
  }

val service: HttpRoutes[IO] = middleware(authedRoutes)

// Composing Authenticated Routes

val spanishRoutes: AuthedRoutes[User, IO] =
  AuthedRoutes.of {
    case GET -> Root / "hola" as user => Ok(s"Hola, ${user.name}")
  }

val frenchRoutes: HttpRoutes[IO] =
  HttpRoutes.of {
    case GET -> Root / "bonjour" => Ok(s"Bonjour")
  }

val service2: HttpRoutes[IO] = middleware(spanishRoutes) <+> frenchRoutes

// 1. Use a Router with unique route prefixes

val service3 = {
  Router(
    "/spanish" -> middleware(spanishRoutes),
    "/french"  -> frenchRoutes
  )
}

// 2. Allow fallthrough, using AuthMiddleware.withFallThrough.

val middlewareWithFallThrough: AuthMiddleware[IO, User] =
  AuthMiddleware.withFallThrough(authUser)
val service4: HttpRoutes[IO]                            = middlewareWithFallThrough(spanishRoutes) <+> frenchRoutes

// 3. Reorder the routes so that authed routes compose last

val service5: HttpRoutes[IO] = frenchRoutes <+> middleware(spanishRoutes)

// Returning an Error Response

// -- With Kleisli

val authUser6: Kleisli[IO, Request[IO], Either[String, User]] = Kleisli(_ => IO(???))

val onFailure: AuthedRoutes[String, IO] = Kleisli(req => OptionT.liftF(Forbidden(req.context)))
val middleware6                         = AuthMiddleware(authUser6, onFailure)

val service6: HttpRoutes[IO] = middleware(authedRoutes)

// Implementing authUser

// -- Cookies

import org.reactormonk.{CryptoBits, PrivateKey}

val key    = PrivateKey(scala.io.Codec.toUTF8(scala.util.Random.alphanumeric.take(20).mkString("")))
val crypto = CryptoBits(key)
val clock  = java.time.Clock.systemUTC

def verifyLogin(request: Request[IO]): IO[Either[String, User]] = ??? // gotta figure out how to do the form
val logIn: Kleisli[IO, Request[IO], Response[IO]]               = Kleisli({ request =>
  verifyLogin(request: Request[IO]).flatMap(_ match {
    case Left(error) =>
      Forbidden(error)
    case Right(user) => {
      val message = crypto.signToken(user.id.toString, clock.millis.toString)
      Ok("Logged in!").map(_.addCookie(ResponseCookie("authcookie", message)))
    }
  })
})

def retrieveUser: Kleisli[IO, Long, User] = Kleisli(id => IO(???))

val authUser7: Kleisli[IO, Request[IO], Either[String, User]] = Kleisli({ request =>
  val message = for {
    header  <- headers.Cookie.from(request.headers).toRight("Cookie parsing error")
    cookie  <- header.values.toList.find(_.name == "authcookie").toRight("Couldn't find the authcookie")
    token   <- crypto.validateSignedToken(cookie.content).toRight("Cookie invalid")
    message <- Either.catchOnly[NumberFormatException](token.toLong).leftMap(_.toString)
  } yield message
  message.traverse(retrieveUser.run)
})

// -- Authorization Header

import org.http4s.headers.Authorization

val authUser8: Kleisli[IO, Request[IO], Either[String, User]] = Kleisli({ request =>
  val message = for {
    header  <- request.headers.get(Authorization).toRight("Couldn't find an Authorization header")
    token   <- crypto.validateSignedToken(header.value).toRight("Invalid token")
    message <- Either.catchOnly[NumberFormatException](token.toLong).leftMap(_.toString)
  } yield message
  message.traverse(retrieveUser.run)
})
