package domain.books

import cats.data.EitherT
import cats.implicits.toFunctorOps
import cats.Monad
import domain.{BookAlreadyExistsError, BookNotFoundError}

class BookService[F[_]: Monad](
                         repository: BookRepository[F],
                         validation: BookValidation[F],
                       ) {
  def updateRateById(rate: Float, id: Option[Long]): F[Option[Long]] = {
    repository.updateRateById(rate, id)
  }

  def delete(id: Long): F[Unit] =
    repository.delete(id).as(())

  def create(book: Book): EitherT[F, BookAlreadyExistsError, Book] = {
    for {
      _ <- validation.doesNotExist(book)
      saved <- EitherT.liftF(repository.create(book))
    } yield saved
  }

  def update(book: Book): EitherT[F, BookNotFoundError.type, Book] =
    for {
      _ <- validation.exists(book.id)
      saved <- EitherT.fromOptionF(repository.update(book), BookNotFoundError)
    } yield saved

  def get(id: Long): F[Option[Book]] =
    repository.get(id)

  def getListOfBooks(pageSize: Int, offset: Int): F[List[Book]] =
    repository.findAll(pageSize, offset)
}

object BookService {
  def apply[F[_]: Monad](
                   repository: BookRepository[F],
                   validation: BookValidation[F],
                 ): BookService[F] =
    new BookService[F](repository, validation)
}