package hutil
package syntax

package object either {

  implicit final class EitherMappingSyntax[L, R](private val either: Either[L, R]) extends AnyVal {

    @inline def leftMap[L2](f: L => L2): Either[L2, R] =
      either match {
        case Left(a)      => Left(f(a))
        case r @ Right(_) => r.asInstanceOf[Either[L2, R]]
      }

    @inline def biMap[L2, R2](fLeft: L => L2, fRight: R => R2): Either[L2, R2] =
      either.leftMap(fLeft).map(fRight)
  }
}
