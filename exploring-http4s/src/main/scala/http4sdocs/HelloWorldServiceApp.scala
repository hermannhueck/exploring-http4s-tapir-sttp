package http4sdocs

import cats.effect._

import io.circe.generic.auto._
import io.circe.syntax._

import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.server.blaze._
import org.http4s.implicits._

import scala.concurrent.ExecutionContext.Implicits.global

case class User(name: String)
case class Hello(greeting: String)

object HelloWorldServiceApp extends App {

  // Needed by `BlazeServerBuilder`. Provided by `IOApp`.
  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO]     = IO.timer(global)

  implicit val decoder = jsonOf[IO, User]

  val jsonApp: HttpApp[IO] =
    HttpRoutes
      .of[IO] {
        case req @ POST -> Root / "hello" =>
          for {
            user <- req.as[User]                // Decode a User request
            resp <- Ok(Hello(user.name).asJson) // Encode a hello response
          } yield (resp)
      }
      .orNotFound

  val server = BlazeServerBuilder[IO](global).bindHttp(8080).withHttpApp(jsonApp).resource
  val fiber  = server.use(_ => IO.never).start.unsafeRunSync()

  println("========= Server Started.")
  Thread.sleep(60000L)
  println("========= Cancelling Server ...")

  fiber.cancel.unsafeRunSync()
}
