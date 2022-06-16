package domain.books

import cats.Applicative
import cats.data.EitherT
import cats.syntax.all._
import domain.{BookAlreadyExistsError, BookNotFoundError}

class BookValidationInterpreter[F[_] : Applicative](bookRepo: BookRepository[F])
  extends BookValidation[F] {
  def doesNotExist(book: Book): EitherT[F, BookAlreadyExistsError, Unit] = EitherT {
    bookRepo.findByTitleAndAuthor(book.title, book.author).map { matches =>
      if (matches.forall(possibleMatch => possibleMatch.pushedBy != book.pushedBy)) {
        Right(())
      } else {
        Left(BookAlreadyExistsError(book))
      }
    }
  }

  def exists(bookId: Option[Long]): EitherT[F, BookNotFoundError.type, Unit] =
    EitherT {
      bookId match {
        case Some(id) =>
          bookRepo.get(id).map {
            case Some(_) => Right(())
            case _ => Left(BookNotFoundError)
          }
        case _ =>
          Either.left[BookNotFoundError.type, Unit](BookNotFoundError).pure[F]
      }
    }
}

object BookValidationInterpreter {
  def apply[F[_] : Applicative](repo: BookRepository[F]): BookValidation[F] =
    new BookValidationInterpreter[F](repo)
}
