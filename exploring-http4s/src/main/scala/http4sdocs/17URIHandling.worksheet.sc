// URI handling

// Literals

import org.http4s._
import org.http4s.implicits._

val uri = uri"http://http4s.org"

// Building URIs

// Methods on URI

val docs = uri.withPath("/docs/0.15")

val docs2 = uri / "docs" / "0.15"

docs == docs2
assert(docs == docs2)

// URI Template

import org.http4s.UriTemplate._

val template = UriTemplate(
  authority = Some(Uri.Authority(host = Uri.RegName("http4s.org"))),
  scheme = Some(Uri.Scheme.http),
  path = List(PathElm("docs"), PathElm("0.15"))
)

val docs3 = template.toUriIfPossible
docs == docs3.get

// Receiving URIs as Strings

// use Uri.fromString

val docs4 = Uri.fromString("http://http4s.org/docs/0.15")
docs == docs4.toOption.get
