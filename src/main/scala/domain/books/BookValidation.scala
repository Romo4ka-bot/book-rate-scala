package domain.books

import cats.data.EitherT
import domain.{BookAlreadyExistsError, BookNotFoundError}

trait BookValidation[F[_]] {
  def doesNotExist(book: Book): EitherT[F, BookAlreadyExistsError, Unit]

  def exists(bookId: Option[Long]): EitherT[F, BookNotFoundError.type, Unit]
}
