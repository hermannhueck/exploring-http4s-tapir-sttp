package http4sdocs

import org.http4s._
import org.http4s.client.blaze._
import org.http4s.client.oauth1
import org.http4s.implicits._
import cats.effect._
import fs2.Stream
import fs2.io.stdout
import fs2.text.{lines, utf8Encode}
import io.circe.Json
import jawnfs2._
import scala.concurrent.ExecutionContext.global

class TWStream[F[_]: ConcurrentEffect: ContextShift] {
  // jawn-fs2 needs to know what JSON AST you want
  implicit val f = new io.circe.jawn.CirceSupportParser(None, false).facade

  /* These values are created by a Twitter developer web app.
   * OAuth signing is an effect due to generating a nonce for each `Request`.
   */
  def sign(consumerKey: String, consumerSecret: String, accessToken: String, accessSecret: String)(
      req: Request[F]
  ): F[Request[F]] = {
    val consumer = oauth1.Consumer(consumerKey, consumerSecret)
    val token    = oauth1.Token(accessToken, accessSecret)
    oauth1.signRequest(req, consumer, callback = None, verifier = None, token = Some(token))
  }

  /* Create a http client, sign the incoming `Request[F]`, stream the `Response[IO]`, and
   * `parseJsonStream` the `Response[F]`.
   * `sign` returns a `F`, so we need to `Stream.eval` it to use a for-comprehension.
   */
  def jsonStream(consumerKey: String, consumerSecret: String, accessToken: String, accessSecret: String)(
      req: Request[F]
  ): Stream[F, Json] =
    for {
      client <- BlazeClientBuilder(global).stream
      sr     <- Stream.eval(sign(consumerKey, consumerSecret, accessToken, accessSecret)(req))
      res    <- client.stream(sr).flatMap(_.body.chunks.parseJsonStream)
    } yield res

  /* Stream the sample statuses.
   * Plug in your four Twitter API values here.
   * We map over the Circe `Json` objects to pretty-print them with `spaces2`.
   * Then we `to` them to fs2's `lines` and then to `stdout` `Sink` to print them.
   */
  def stream(blocker: Blocker): Stream[F, Unit] = {
    val req = Request[F](Method.GET, uri"https://stream.twitter.com/1.1/statuses/sample.json")
    val s   = jsonStream("<consumerKey>", "<consumerSecret>", "<accessToken>", "<accessSecret>")(req)
    s.map(_.spaces2).through(lines).through(utf8Encode).through(stdout(blocker))
  }

  /** Compile our stream down to an effect to make it runnable */
  def run: F[Unit] =
    Stream
      .resource(Blocker[F])
      .flatMap { blocker =>
        stream(blocker)
      }
      .compile
      .drain
}

object TWStreamApp extends IOApp {
  def run(args: List[String]) =
    (new TWStream[IO]).run.as(ExitCode.Success)
}
