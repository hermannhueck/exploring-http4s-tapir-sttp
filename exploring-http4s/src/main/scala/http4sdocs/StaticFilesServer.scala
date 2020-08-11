package http4sdocs

import cats.effect._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.Server
import org.http4s.server.staticcontent._
import org.http4s.syntax.kleisli._
import scala.concurrent.ExecutionContext.global

object StaticFilesServer extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    app.use(_ => IO.never).as(ExitCode.Success)

  val app: Resource[IO, Server[IO]] =
    for {
      blocker <- Blocker[IO]
      server  <- BlazeServerBuilder[IO](global)
                   .bindHttp(8080)
                   .withHttpApp(fileService[IO](FileService.Config(".", blocker)).orNotFound)
                   .resource
    } yield server
}
