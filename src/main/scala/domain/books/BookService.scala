package domain.books

import cats.data.EitherT
import cats.implicits.toFunctorOps
import cats.{Functor, Monad}
import domain.BookAlreadyExistsError

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
}

object BookService {
  def apply[F[_]](
                   repository: BookRepository[F],
                   validation: BookValidation[F],
                 ): BookService[F] =
    new BookService[F](repository, validation)
}