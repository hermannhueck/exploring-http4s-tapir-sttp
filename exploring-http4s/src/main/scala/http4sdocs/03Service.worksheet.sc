// Service

import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import scala.concurrent.ExecutionContext.Implicits.global

implicit val cs: ContextShift[IO] = IO.contextShift(global)
implicit val timer: Timer[IO]     = IO.timer(global)

val helloWorldService = HttpRoutes.of[IO] {
  case GET -> Root / "hello" / name =>
    Ok(s"Hello, $name.")
}

case class Tweet(id: Int, message: String)

implicit
def tweetEncoder: EntityEncoder[IO, Tweet]                = ???
implicit def tweetsEncoder: EntityEncoder[IO, Seq[Tweet]] = ???

def getTweet(tweetId: Int): IO[Tweet]  = ???
def getPopularTweets(): IO[Seq[Tweet]] = ???

val tweetService = HttpRoutes.of[IO] {
  case GET -> Root / "tweets" / "popular"       =>
    getPopularTweets().flatMap(Ok(_))
  case GET -> Root / "tweets" / IntVar(tweetId) =>
    getTweet(tweetId).flatMap(Ok(_))
}

import cats.implicits._
import org.http4s.server.blaze._
import org.http4s.implicits._
import org.http4s.server.Router

val services = tweetService <+> helloWorldService

val httpApp = Router("/" -> helloWorldService, "/api" -> services).orNotFound

val serverBuilder = BlazeServerBuilder[IO](global).bindHttp(8080, "localhost").withHttpApp(httpApp)

// val fiber = serverBuilder.resource.use(_ => IO.never).start.unsafeRunSync()

// fiber.cancel.unsafeRunSync()

// Running your service as an App

// import cats.effect._
// import org.http4s.HttpRoutes
// import org.http4s.dsl.io._
// import org.http4s.implicits._
// import org.http4s.server.blaze._
// import scala.concurrent.ExecutionContext.global
// object Main extends IOApp {

//   val helloWorldService = HttpRoutes.of[IO] {
//     case GET -> Root / "hello" / name =>
//       Ok(s"Hello, $name.")
//   }.orNotFound

//   def run(args: List[String]): IO[ExitCode] =
//     BlazeServerBuilder[IO](global)
//       .bindHttp(8080, "localhost")
//       .withHttpApp(helloWorldService)
//       .serve
//       .compile
//       .drain
//       .as(ExitCode.Success)
// }

// You may also create the server within an IOApp using resource:

// object MainWithResource extends IOApp {

//   def run(args: List[String]): IO[ExitCode] =
//     BlazeServerBuilder[IO](global)
//       .bindHttp(8080, "localhost")
//       .withHttpApp(Main.helloWorldService)
//       .resource
//       .use(_ => IO.never)
//       .as(ExitCode.Success)
// }
