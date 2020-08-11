// JSON Handling

// Add the JSON support module(s)

// Sending raw JSON

import cats.effect._
import io.circe._
import io.circe.literal._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._

def hello(name: String): Json =
  json"""{"hello": $name}"""

val greeting = hello("world")

// Ok(greeting).unsafeRunSync()
// error: Cannot convert from io.circe.Json to an Entity, because no EntityEncoder[cats.effect.IO, io.circe.Json] instance could be found.

import org.http4s.circe._

Ok(greeting).unsafeRunSync()

// The same EntityEncoder[Json] we use on server responses is also useful on client requests:

import org.http4s.client.dsl.io._

POST(json"""{"name": "Alice"}""", uri"/hello").unsafeRunSync()

// Encoding case classes as JSON

case class Hello(name: String)
case class User(name: String)

import io.circe.syntax._

// Hello("Alice").asJson
// error: could not find implicit value for parameter encoder: io.circe.Encoder[repl.Session.App.Hello]
//   Encoder.instance { (hello: Hello) =>
//                              ^

implicit val HelloEncoder: Encoder[Hello] =
  Encoder.instance { (hello: Hello) =>
    json"""{"hello": ${hello.name}}"""
  }

Hello("Alice").asJson
Hello("Alice").asJson.toString

import io.circe.generic.auto._

User("Alice").asJson
User("Alice").asJson.toString

Ok(Hello("Alice").asJson).unsafeRunSync()
POST(User("Bob").asJson, uri"/hello").unsafeRunSync()

{
  import org.http4s.circe.CirceEntityEncoder._
  Ok(Hello("Alice")).unsafeRunSync()
  POST(User("Bob"), uri"/hello").unsafeRunSync()
}
// Thus there’s no more need in calling asJson on result. However, it may introduce ambiguity errors
// when we also build some json by hand within the same scope.

// Receiving raw JSON

Ok("""{"name":"Alice"}""").flatMap(_.as[Json]).unsafeRunSync()
POST("""{"name":"Bob"}""", uri"/hello").flatMap(_.as[Json]).unsafeRunSync()

// Decoding JSON to a case class

implicit val userDecoder = jsonOf[IO, User]

Ok("""{"name":"Alice"}""").flatMap(_.as[User]).unsafeRunSync()
POST("""{"name":"Bob"}""", uri"/hello").flatMap(_.as[User]).unsafeRunSync()

// If we are always decoding from JSON to a typed model, we can use the following import:

// import org.http4s.circe.CirceEntityDecoder._

// This creates an EntityDecoder[A] for every A that has a Decoder instance.

// However, be cautious when using this. Having this implicit in scope does mean that we would always try to decode HTTP entities from JSON (even if it is XML or plain text, for instance).

// For more convenience there is import combining both encoding and decoding derivation:

// import org.http4s.circe.CirceEntityCodec._

// Putting it all together

// See Hello world service: HelloWorldServiceApp

// See Hello world client: HelloWorldClientApp
