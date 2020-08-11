// Static Files

// Getting Started

// ETags

// Execution Context

import java.util.concurrent._
import java.io._
import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.server.staticcontent._

val blockingPool = Executors.newFixedThreadPool(4)
val blocker      = Blocker.liftExecutorService(blockingPool)

import scala.concurrent.ExecutionContext

implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

val routes = fileService[IO](FileService.Config(".", blocker))

val routes2 = HttpRoutes.of[IO] {
  case request @ Method.GET -> Root / "index.html" =>
    StaticFile
      .fromFile(new File("relative/path/to/index.html"), blocker, Some(request))
      .getOrElseF(NotFound()) // In case the file doesn't exist
}

// Serving from jars

val routes3 = resourceService[IO](ResourceService.Config("/assets", blocker))

def static(file: String, blocker: Blocker, request: Request[IO]) =
  StaticFile.fromResource("/" + file, blocker, Some(request)).getOrElseF(NotFound())

val routes4 = HttpRoutes.of[IO] {
  case request @ Method.GET -> Root / path if List(".js", ".css", ".map", ".html", ".webm").exists(path.endsWith) =>
    static(path, blocker, request)
}

// Webjars

import org.http4s.server.staticcontent.webjarService
import org.http4s.server.staticcontent.WebjarService.{Config, WebjarAsset}

// only allow js assets
def isJsAsset(asset: WebjarAsset): Boolean =
  asset.asset.endsWith(".js")

val webjars: HttpRoutes[IO] = webjarService(
  Config(
    filter = isJsAsset,
    blocker = blocker
  )
)

blockingPool.shutdown()
