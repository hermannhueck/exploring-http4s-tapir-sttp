// Middleware

import cats.data.Kleisli
import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._

def myMiddle(service: HttpRoutes[IO], header: Header): HttpRoutes[IO] =
  Kleisli { (req: Request[IO]) =>
    service(req).map {
      case Status.Successful(resp) =>
        resp.putHeaders(header)
      case resp                    =>
        resp
    }
  }

val service = HttpRoutes.of[IO] {
  case GET -> Root / "bad" =>
    BadRequest()
  case _                   =>
    Ok()
}

val goodRequest = Request[IO](Method.GET, uri"/")

val badRequest = Request[IO](Method.GET, uri"/bad")

service.orNotFound(goodRequest).unsafeRunSync()
service.orNotFound(badRequest).unsafeRunSync()

val wrappedService = myMiddle(service, Header("SomeKey", "SomeValue"));

val resp1 = wrappedService.orNotFound(goodRequest).unsafeRunSync()
resp1.headers
val resp2 = wrappedService.orNotFound(badRequest).unsafeRunSync()
resp2.headers

object MyMiddle {

  def addHeader(resp: Response[IO], header: Header) =
    resp match {
      case Status.Successful(resp) => resp.putHeaders(header)
      case resp                    => resp
    }

  def apply(service: HttpRoutes[IO], header: Header) =
    service.map(addHeader(_, header))
}

val newService = MyMiddle(service, Header("SomeKey", "SomeValue"))

val resp3 = newService.orNotFound(goodRequest).unsafeRunSync()
resp3.headers
val resp4 = newService.orNotFound(badRequest).unsafeRunSync()
resp4.headers

// Composing Services with Middleware

val apiService = HttpRoutes.of[IO] {
  case GET -> Root / "api" =>
    Ok()
}

val aggregateService = apiService <+> MyMiddle(service, Header("SomeKey", "SomeValue"))

val apiRequest = Request[IO](Method.GET, uri"/api")

aggregateService.orNotFound(goodRequest).unsafeRunSync()
aggregateService.orNotFound(apiRequest).unsafeRunSync()

// Note that goodRequest ran through the MyMiddle middleware and the Result had our header added to it.
// But, apiRequest did not go through the middleware and did not have the header added to it’s Result.

// Included Middleware

// Metrics Middleware

// Dropwizard Metrics Middleware

import org.http4s.server.middleware.Metrics
import org.http4s.metrics.dropwizard.Dropwizard
import com.codahale.metrics.SharedMetricRegistries

implicit val clock = Clock.create[IO]

val registry = SharedMetricRegistries.getOrCreate("default")

val meteredRoutes = Metrics[IO](Dropwizard(registry, "server"))(apiService)

// Prometheus Metrics Middleware

import cats.effect.{Clock, IO, Resource}
import org.http4s.HttpRoutes
import org.http4s.metrics.prometheus.{Prometheus, PrometheusExportService}
import org.http4s.server.Router
// import org.http4s.server.middleware.Metrics

// implicit val clock = Clock.create[IO]

val meteredRouter: Resource[IO, HttpRoutes[IO]] =
  for {
    metricsSvc <- PrometheusExportService.build[IO]
    metrics    <- Prometheus.metricsOps[IO](metricsSvc.collectorRegistry, "server")
    router      = Router[IO](
                    "/api" -> Metrics[IO](metrics)(apiService),
                    "/"    -> metricsSvc.routes
                  )
  } yield router

// X-Request-ID Middleware

import org.http4s.server.middleware.RequestId
import org.http4s.util.CaseInsensitiveString

val requestIdService = RequestId.httpRoutes(HttpRoutes.of[IO] {
  case req =>
    val reqId = req.headers.get(CaseInsensitiveString("X-Request-ID")).fold("null")(_.value)
    // use request id to correlate logs with the request
    IO(println(s"request received, cid=$reqId")) *> Ok()
})
val responseIO       = requestIdService.orNotFound(goodRequest)

// generated request id can be correlated with logs
val resp = responseIO.unsafeRunSync()

resp.headers
resp.attributes.lookup(RequestId.requestIdAttrKey)
