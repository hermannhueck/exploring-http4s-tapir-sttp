// HTTP Client

import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.blaze._

// Defining the Server

import scala.concurrent.ExecutionContext.global
implicit val cs: ContextShift[IO] = IO.contextShift(global)
implicit val timer: Timer[IO]     = IO.timer(global)

val app = HttpRoutes
  .of[IO] {
    case GET -> Root / "hello" / name =>
      Ok(s"Hello, $name.")
  }
  .orNotFound

val server =
  BlazeServerBuilder[IO](global)
    .bindHttp(8080, "localhost")
    .withHttpApp(app)
    .resource

val fiber = server.use(_ => IO.never).start.unsafeRunSync()

// Creating the client

import org.http4s.client.blaze._
import org.http4s.client._

BlazeClientBuilder[IO](global).resource.use { client =>
  // use `client` here and return an `IO`.
  // the client will be acquired and shut down
  // automatically each time the `IO` is run.
  IO.unit
}

import cats.effect.Blocker
import java.util.concurrent._

val blockingPool           = Executors.newFixedThreadPool(5)
val blocker                = Blocker.liftExecutorService(blockingPool)
val httpClient: Client[IO] = JavaNetClientBuilder[IO](blocker).create

// Describing a call

val helloJames = httpClient.expect[String]("http://localhost:8080/hello/James")
helloJames.unsafeRunSync()

import cats._, cats.effect._, cats.implicits._
// import org.http4s.Uri

def hello(name: String): IO[String] = {
  val target = uri"http://localhost:8080/hello/" / name
  httpClient.expect[String](target)
}

val people = Vector("Michael", "Jessica", "Ashley", "Christopher")

val greetingList = people.parTraverse(hello)
greetingList.unsafeRunSync()

// Making the call

val greetingsStringEffect = greetingList.map(_.mkString("\n"))
greetingsStringEffect.unsafeRunSync()

// Constructing a URI

uri"https://my-awesome-service.com/foo/bar?wow=yeah"

val validUri   = "https://my-awesome-service.com/foo/bar?wow=yeah"
val invalidUri = "yeah whatever"

val uri: Either[ParseFailure, Uri]          = Uri.fromString(validUri)
val parseFailure: Either[ParseFailure, Uri] = Uri.fromString(invalidUri)

uri"http://foo.com"
  .withPath("/bar/baz")
  .withQueryParam("hello", "world")

// Middleware

// Included Middleware

// Http4s includes some middleware Out of the Box in the org.http4s.client.middleware package. These include:

// - Following of redirect responses (Follow Redirect)
// - Retrying of requests (Retry)
// - Metrics gathering (Metrics)
// - Logging of requests (Request Logger)
// - Logging of responses (Response Logger)
// - Logging of requests and responses (Logger)
// - Metrics Middleware

// Dropwizard Metrics Middleware

import org.http4s.client.middleware.Metrics
import org.http4s.metrics.dropwizard.Dropwizard
import org.http4s.metrics.prometheus.Prometheus
import com.codahale.metrics.SharedMetricRegistries

implicit val clock          = Clock.create[IO]
val registry                = SharedMetricRegistries.getOrCreate("default")
val requestMethodClassifier = (r: Request[IO]) => Some(r.method.toString.toLowerCase)

val meteredClientDropwizard =
  Metrics[IO](Dropwizard(registry, "prefix"), requestMethodClassifier)(httpClient)

// Prometheus Metrics Middleware

val meteredClientPrometheus: Resource[IO, Client[IO]] =
  for {
    registry <- Prometheus.collectorRegistry[IO]
    metrics  <- Prometheus.metricsOps[IO](registry, "prefix")
  } yield Metrics[IO](metrics, requestMethodClassifier)(httpClient)

// Examples

// Send a GET request, treating the response as a string

val io1 = httpClient.expect[String](uri"https://avm.de/")
io1.unsafeRunSync().take(50)

{
  import org.http4s.client.dsl.io._
  import org.http4s.headers._
  import org.http4s.MediaType
  import org.http4s.Method._

  val request = GET(
    uri"https://my-lovely-api.com/",
    Authorization(Credentials.Token(AuthScheme.Bearer, "open sesame")),
    Accept(MediaType.application.json)
  )

  httpClient.expect[String](request)
}

// Post a form, decoding the JSON response to a case class

object postForm {

  import org.http4s.client.dsl.io._
  import org.http4s.Method._

  case class AuthResponse(access_token: String)

  // See the JSON page for details on how to define this
  implicit
  val authResponseEntityDecoder: EntityDecoder[IO, AuthResponse] = null

  val postRequest = POST(
    UrlForm(
      "grant_type"    -> "client_credentials",
      "client_id"     -> "my-awesome-client",
      "client_secret" -> "s3cr3t"
    ),
    uri"https://my-lovely-api.com/oauth2/token"
  )

  val authResp = httpClient
    .expect[AuthResponse](postRequest)
    .unsafeRunSync()

  val token = authResp.access_token
}

// Calls to a JSON API

// -- Take a look at json.

// Body decoding / encoding

// The reusable way to decode/encode a request is to write a custom EntityDecoder and EntityEncoder.
// For that topic, take a look at entity.

// If you prefer a more fine-grained approach, some of the methods take a Response[F] => F[A] argument,
// such as run or get, which lets you add a function which includes the decoding functionality,
// but ignores the media type.

// client.run(req).use {
//   case Status.Successful(r) => r.attemptAs[A].leftMap(_.message).value
//   case r => r.as[String]
//     .map(b => Left(s"Request $req failed with status ${r.status.code} and body $b"))
// }

// However, your function has to consume the body before the returned F exits. Don’t do this:

// // will come back to haunt you
// client.get[EntityBody]("some-url")(response => response.body)

// Passing it to a EntityDecoder is safe.

// client.get[T]("some-url")(response => jsonOf(response.body))
