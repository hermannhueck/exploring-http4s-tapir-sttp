// Streaming

// Introduction

// Streaming responses from your service

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import cats.effect._
import fs2.Stream
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._

// Provided by `cats.effect.IOApp`, needed elsewhere:
implicit val timer: Timer[IO]     = IO.timer(global)
implicit val cs: ContextShift[IO] = IO.contextShift(global)

// An infinite stream of the periodic elapsed time
val seconds = Stream.awakeEvery[IO](1.second)

val routes = HttpRoutes.of[IO] {
  case GET -> Root / "seconds" =>
    Ok(seconds.map(_.toString)) // `map` `toString` because there's no `EntityEncoder` for `Duration`
}

val request = Request[IO](Method.GET, uri"/seconds")

val response = routes.orNotFound(request).unsafeRunSync()

response
  .body
  .evalTap(s => IO(println(s)))
  .take(10)
  .compile
  .toList
  .unsafeRunSync()

// Consuming streams with the client

// see TWStreamApp
