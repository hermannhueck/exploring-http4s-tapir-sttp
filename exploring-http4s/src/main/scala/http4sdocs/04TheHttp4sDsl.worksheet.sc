// The Http4s DSL

import org.http4s.dsl.request
import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._

import scala.concurrent.ExecutionContext

implicit val ec: ExecutionContext = ExecutionContext.global
implicit val timer: Timer[IO]     = IO.timer(ec)
implicit val cs: ContextShift[IO] = IO.contextShift(ec)

// The simplest service

val service = HttpRoutes.of[IO] {
  case _ =>
    IO(Response(Status.Ok))
}

// Testing the Service

val getRoot = Request[IO](Method.GET, uri"/")

val io = service.orNotFound.run(getRoot)

val response = io.unsafeRunSync()

// Generating responses
// -- Status Codes

val okIo = Ok()

val ok = okIo.unsafeRunSync()

HttpRoutes
  .of[IO] {
    case _ => Ok()
  }
  .orNotFound
  .run(getRoot)
  .unsafeRunSync()

HttpRoutes
  .of[IO] {
    case _ => NoContent()
  }
  .orNotFound
  .run(getRoot)
  .unsafeRunSync()

// -- Headers

Ok("Ok response.").unsafeRunSync().headers

import org.http4s.headers.`Cache-Control`
import org.http4s.CacheDirective.`no-cache`
import cats.data.NonEmptyList

Ok("Ok response.", `Cache-Control`(NonEmptyList(`no-cache`(), Nil))).unsafeRunSync().headers

Ok("Ok response.", Header("X-Auth-Token", "value")).unsafeRunSync().headers

// -- Cookies

Ok("Ok response.").map(_.addCookie(ResponseCookie("foo", "bar"))).unsafeRunSync().headers

val cookieResp = {
  for {
    resp <- Ok("Ok response.")
    now  <- HttpDate.current[IO]
  } yield resp.addCookie(ResponseCookie("foo", "bar", expires = Some(now), httpOnly = true, secure = true))
}
cookieResp.unsafeRunSync().headers

Ok("Ok response.").map(_.removeCookie("foo")).unsafeRunSync().headers

// Responding with a body
// -- Simple bodies

Ok("Received request.").unsafeRunSync()

import java.nio.charset.StandardCharsets.UTF_8
Ok("binary".getBytes(UTF_8)).unsafeRunSync()

// NoContent("does not compile")
// error: type mismatch;
//  found   : String("does not compile")
//  required: org.http4s.Header
// NoContent("does not compile")
//           ^^^^^^^^^^^^^^^^^^
NoContent().unsafeRunSync()

// -- Asynchronous responses

import scala.concurrent.Future

val io2 = Ok(IO.fromFuture(IO(Future {
  println("I run when the future is constructed.")
  "Greetings from the future!"
})))

io2.unsafeRunSync()

val io3 = Ok(IO {
  println("I run when the IO is run.")
  "Mission accomplished!"
})

io3.unsafeRunSync()

// Streaming bodies

import fs2.Stream
import scala.concurrent.duration._

val drip: Stream[IO, String] =
  Stream.awakeEvery[IO](100.millis).map(_.toString).take(10)

val dripOutIO = drip.through(fs2.text.lines).through(_.evalMap(s => { IO { println(s); s } })).compile.drain

dripOutIO.unsafeRunSync()

Ok(drip)

// Matching and extracting requests
// -- The -> object

HttpRoutes.of[IO] {
  case GET -> Root / "hello" => Ok("hello")
}

// -- Path info

// -- Matching paths

HttpRoutes.of[IO] {
  case GET -> Root => Ok("root")
}

import fs2.text

implicit final class Http4sBodyOps(private val body: EntityBody[IO]) {
  @inline def runToString: String =
    body
      .through(text.utf8Decode[IO])
      .through(text.lines)
      .compile
      .string
      .unsafeRunSync()
}

implicit final class HttpRoutesOps(private val routes: HttpRoutes[IO]) {
  @inline def runWithRequest(request: Request[IO]): String =
    routes
      .orNotFound
      .run(request)
      .unsafeRunSync()
      .body
      .runToString
}

HttpRoutes
  .of[IO] {
    case GET -> Root / "hello" / name => Ok(s"Hello, $name!")
  }
  .runWithRequest {
    Request[IO](Method.GET, uri"/hello/Alice")
  }

HttpRoutes
  .of[IO] {
    case GET -> "hello" /: rest => Ok(s"""Hello, ${rest.toList.mkString(" and ")}!""")
  }
  .runWithRequest {
    Request[IO](Method.GET, uri"/hello/Alice/Bob")
  }

HttpRoutes
  .of[IO] {
    case GET -> Root / file ~ "json" => Ok(s"""{"response": "You asked for $file"}""")
  }
  .runWithRequest {
    Request[IO](Method.GET, uri"myContent.json")
  }

// -- Handling path parameters

def getUserName(userId: Int): IO[String] =
  IO.pure(s"John Doe ($userId)")

val usersService = HttpRoutes.of[IO] {
  case GET -> Root / "users" / IntVar(userId) =>
    Ok(getUserName(userId))
}
usersService
  .runWithRequest {
    Request[IO](Method.GET, uri"/users/42")
  }

import java.time.LocalDate
import scala.util.Try
import org.http4s.client.dsl.io._
object LocalDateVar {
  def unapply(str: String): Option[LocalDate] = {
    if (!str.isEmpty)
      Try(LocalDate.parse(str)).toOption
    else
      None
  }
}

def getTemperatureForecast(date: LocalDate): IO[Double] = IO(42.23)

val dailyWeatherService = HttpRoutes.of[IO] {
  case GET -> Root / "weather" / "temperature" / LocalDateVar(localDate) =>
    Ok(getTemperatureForecast(localDate).map(s"The temperature on $localDate will be: " + _))
}

val response2 = GET(uri"/weather/temperature/2016-11-05")
  .flatMap(dailyWeatherService.orNotFound(_))
  .unsafeRunSync()

response2
  .body
  .runToString

// -- Handling query parameters

// A query parameter needs to have a QueryParamDecoderMatcher provided to extract it.
// In order for the QueryParamDecoderMatcher to work there needs to be an implicit QueryParamDecoder[T] in scope.
// QueryParamDecoders for simple types can be found in the QueryParamDecoder object.
// There are also QueryParamDecoderMatchers available which can be used to return optional or validated parameter values.

import java.time.Year
object CountryQueryParamMatcher extends QueryParamDecoderMatcher[String]("country")

implicit val yearQueryParamDecoder: QueryParamDecoder[Year] =
  QueryParamDecoder[Int].map(Year.of)

object YearQueryParamMatcher extends QueryParamDecoderMatcher[Year]("year")

def getAverageTemperatureForCountryAndYear(country: String, year: Year): IO[Double] = IO.pure(17.25)

val averageTemperatureService = HttpRoutes.of[IO] {
  case GET -> Root / "weather" / "temperature" :? CountryQueryParamMatcher(country) +& YearQueryParamMatcher(year) =>
    Ok(
      getAverageTemperatureForCountryAndYear(country, year).map(s"Average temperature for $country in $year was: " + _)
    )
}

averageTemperatureService
  .runWithRequest {
    Request[IO](Method.GET, uri"/weather/temperature?country=Germany&year=2019")
  }

import java.time.Instant
import java.time.format.DateTimeFormatter
implicit val isoInstantCodec: QueryParamCodec[Instant] =
  QueryParamCodec.instantQueryParamCodec(DateTimeFormatter.ISO_INSTANT)

object IsoInstantParamMatcher extends QueryParamDecoderMatcher[Instant]("timestamp")

// -- Optional query parameters

// import java.time.Year
// import org.http4s.client.dsl.io._

// implicit val yearQueryParamDecoder: QueryParamDecoder[Year] =
//   QueryParamDecoder[Int].map(Year.of)

object OptionalYearQueryParamMatcher extends OptionalQueryParamDecoderMatcher[Year]("year")

def getAverageTemperatureForCurrentYear: IO[String]   = IO.pure("17.25")
def getAverageTemperatureForYear(y: Year): IO[String] = IO.pure("15.00")

val routes2 = HttpRoutes.of[IO] {
  case GET -> Root / "temperature" :? OptionalYearQueryParamMatcher(maybeYear) =>
    maybeYear match {
      case None       =>
        Ok(getAverageTemperatureForCurrentYear)
      case Some(year) =>
        Ok(getAverageTemperatureForYear(year))
    }
}

routes2
  .runWithRequest {
    Request[IO](Method.GET, uri"/temperature?year=2018")
  }
routes2
  .runWithRequest {
    Request[IO](Method.GET, uri"/temperature")
  }

// -- Missing required query parameters
// A request with a missing required query parameter will fall through to the following case statements
// and may eventually return a 404. To provide contextual error handling, optional query parameters
// or fallback routes can be used.

// -- Invalid query parameter handling

{

  import cats.syntax.either._

  implicit val yearQueryParamDecoder: QueryParamDecoder[Year] =
    QueryParamDecoder[Int]
      .emap { i =>
        Try(Year.of(i))
          .toEither
          .leftMap(t => ParseFailure(t.getMessage, t.getMessage))
      }

  object YearQueryParamMatcher extends ValidatingQueryParamDecoderMatcher[Year]("year")

  val routes = HttpRoutes.of[IO] {
    case GET -> Root / "temperature" :? YearQueryParamMatcher(yearValidated) =>
      yearValidated.fold(
        parseFailures => BadRequest("unable to parse argument year"),
        year => Ok(getAverageTemperatureForYear(year))
      )
  }

  routes
    .runWithRequest {
      Request[IO](Method.GET, uri"/temperature?year=2018_INVALID")
    }
}
