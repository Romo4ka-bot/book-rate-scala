package domain.books

import cats.data.EitherT
import cats.implicits.toFunctorOps
import cats.{Functor, Monad}
import domain.{BookAlreadyExistsError, BookNotFoundError}

class BookService[F[_]](
                         repository: BookRepository[F],
                         validation: BookValidation[F],
                       ) {
  def delete(id: Long)(implicit F: Functor[F]): F[Unit] =
    repository.delete(id).as(())

  def create(book: Book)(implicit M: Monad[F]): EitherT[F, BookAlreadyExistsError, Book] = {
    for {
      _ <- validation.doesNotExist(book)
      saved <- EitherT.liftF(repository.create(book))
    } yield saved
  }

  def update(book: Book)(implicit M: Monad[F]): EitherT[F, BookNotFoundError.type, Book] =
    for {
      _ <- validation.exists(book.id)
      saved <- EitherT.fromOptionF(repository.update(book), BookNotFoundError)
    } yield saved

  def get(id: Long)(implicit F: Functor[F]): EitherT[F, BookNotFoundError.type, Book] =
    EitherT.fromOptionF(repository.get(id), BookNotFoundError)

  def getListOfBooks(pageSize: Int, offset: Int): F[List[Book]] =
    repository.findAll(pageSize, offset)
}

object BookService {
  def apply[F[_]](
                   repository: BookRepository[F],
                   validation: BookValidation[F],
                 ): BookService[F] =
    new BookService[F](repository, validation)
}