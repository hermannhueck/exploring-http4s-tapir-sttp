// Testing

// Introduction

import cats.implicits._
import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._
import cats.effect._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.implicits._

case class User(name: String, age: Int)

implicit
val UserEncoder: Encoder[User] = deriveEncoder[User]

trait UserRepo[F[_]] {
  def find(userId: String): F[Option[User]]
}

def service[F[_]](repo: UserRepo[F])(
    implicit
    F: Effect[F]
): HttpRoutes[F] =
  HttpRoutes.of[F] {
    case GET -> Root / "user" / id =>
      repo.find(id).map {
        case Some(user) => Response(status = Status.Ok).withEntity(user.asJson)
        case None       => Response(status = Status.NotFound)
      }
  }

def check[A](actual: IO[Response[IO]], expectedStatus: Status, expectedBody: Option[A])(
    implicit
    ev: EntityDecoder[IO, A]
): Boolean = {
  val actualResp  = actual.unsafeRunSync()
  val statusCheck = actualResp.status == expectedStatus
  val bodyCheck   = expectedBody.fold[Boolean](
    actualResp.body.compile.toVector.unsafeRunSync().isEmpty
  )( // Verify Response's body is empty.
    expected => actualResp.as[A].unsafeRunSync() == expected
  )
  statusCheck && bodyCheck
}

// Let’s define service by passing a UserRepo that returns Ok(user).

val success: UserRepo[IO] = new UserRepo[IO] {
  def find(id: String): IO[Option[User]] =
    IO.pure(Some(User("johndoe", 42)))
}

val response: IO[Response[IO]] = service[IO](success)
  .orNotFound
  .run(
    Request(method = Method.GET, uri = uri"/user/not-used")
  )

val expectedJson = Json.obj(
  ("name", Json.fromString("johndoe")),
  ("age", Json.fromBigInt(42))
)

check[Json](response, Status.Ok, Some(expectedJson))

// Next, let’s define a service with a userRepo that returns None to any input.

val foundNone: UserRepo[IO] = new UserRepo[IO] {
  def find(id: String): IO[Option[User]] =
    IO.pure(None)
}

val responseNone: IO[Response[IO]] = service[IO](foundNone)
  .orNotFound
  .run(
    Request(method = Method.GET, uri = uri"/user/not-used")
  )

check[Json](responseNone, Status.NotFound, None)

// Finally, let’s pass a Request which our service does not handle.

val doesNotMatter: UserRepo[IO] = new UserRepo[IO] {
  def find(id: String): IO[Option[User]] =
    IO.raiseError(new RuntimeException("Should not get called!"))
}

val responseNotMatchingPath: IO[Response[IO]] = service[IO](doesNotMatter)
  .orNotFound
  .run(
    Request(method = Method.GET, uri = uri"/not-a-matching-path")
  )

check[String](responseNotMatchingPath, Status.NotFound, Some("Not found"))
