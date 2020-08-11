package http4sdocs

import io.circe.generic.auto._
import io.circe.syntax._

import cats.effect._

import fs2.Stream

import org.http4s._
import org.http4s.circe._
import org.http4s.implicits._
import org.http4s.client.dsl.io._
import org.http4s.client.blaze._

import scala.util.chaining._
import scala.concurrent.ExecutionContext.Implicits.global

object HelloWorldClientApp extends hutil.App {

  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO]     = IO.timer(global)

  // Decode the Hello response
  def helloClient(name: String): Stream[IO, Hello] = {
    // Encode a User request
    val user = User(name)
    s"------->> sending: $user" pipe println
    val req  = Method.POST(user.asJson, uri"http://localhost:8080/hello")
    // Create a client
    BlazeClientBuilder[IO](global).stream.flatMap { httpClient =>
      // Decode a Hello response
      Stream.eval(httpClient.expect(req)(jsonOf[IO, Hello]))
    }
  }

  val name       = if (args.length == 0) "Alice" else args(0)
  val helloAlice = helloClient(name)

  helloAlice.compile.last.unsafeRunSync() pipe { hello =>
    println(s"------->> got: $hello")
  }
}
