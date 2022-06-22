package domain.reviews

import cats.Monad
import cats.data.EitherT
import cats.implicits.toFunctorOps
import cats.syntax.all._
import domain.books.BookService
import domain.{ReviewNotFoundError, ValidationMarkError}

class ReviewService[F[_]: Monad](
                           repository: ReviewRepository[F],
                           validation: ReviewValidation[F],
                           bookService: BookService[F]
                         ) {
  def delete(id: Long): F[Unit] =
    repository.delete(id).as(())

  def create(review: Review): EitherT[F, ValidationMarkError, Review] = {
    for {
      _ <- validation.verifyMark(review)
      saved <- EitherT.liftF(repository.create(review))
    } yield saved
  }

  def update(review: Review): EitherT[F, ReviewNotFoundError.type, Review] = {
    for {
      _ <- validation.exists(review.id)
      saved <- EitherT.fromOptionF(repository.update(review), ReviewNotFoundError)
    } yield saved
  }

  def get(id: Long): EitherT[F, ReviewNotFoundError.type, Review] =
    EitherT.fromOptionF(repository.get(id), ReviewNotFoundError)

  def getListOfReviews(pageSize: Int, offset: Int): F[List[Review]] =
    repository.findAll(pageSize, offset)

  def calculateBookRate(bookId: Long): F[Float] = {
    for {
      marks <- repository.findAllByBookId(bookId)
      book <- bookService.get(bookId)
      rate = medianValue(marks.map(mark => mark.mark), book.get.rate)
      _ <- bookService.updateRateById(rate, book.get.id)
    } yield rate
  }

  private def medianValue(marks: List[Int], bookRate: Float): Float = ((marks.sum.toFloat + bookRate) / (marks.size + 1).toFloat)
}

object ReviewService {
  def apply[F[_]: Monad](
                   repository: ReviewRepository[F],
                   validation: ReviewValidation[F],
                   bookService: BookService[F]
                 ): ReviewService[F] =
    new ReviewService[F](repository, validation, bookService)
}
