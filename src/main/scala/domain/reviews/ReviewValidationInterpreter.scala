package domain.reviews

import cats.Applicative
import cats.data.EitherT
import cats.syntax.all._
import domain.{IncorrectMarkEnteredError, NonPositiveNumberError, ReviewNotFoundError, ValidationMarkError}

class ReviewValidationInterpreter[F[_] : Applicative](bookRepo: ReviewRepository[F])
  extends ReviewValidation[F] {

  def verifyMark(review: Review): EitherT[F, ValidationMarkError, Unit] =
    EitherT {
      review.mark match {
        case mark if mark > -1 && mark < 11 => Either.right[ValidationMarkError, Unit](()).pure[F]
        case mark if mark > 10 => Either.left[ValidationMarkError, Unit](IncorrectMarkEnteredError).pure[F]
        case _ => Either.left[ValidationMarkError, Unit](NonPositiveNumberError).pure[F]
      }
    }

  def exists(reviewId: Option[Long]): EitherT[F, ReviewNotFoundError.type, Unit] =
    EitherT {
      reviewId match {
        case Some(id) =>
          bookRepo.get(id).map {
            case Some(_) => Right(())
            case _ => Left(ReviewNotFoundError)
          }
        case _ =>
          Either.left[ReviewNotFoundError.type, Unit](ReviewNotFoundError).pure[F]
      }
    }
}

object ReviewValidationInterpreter {
  def apply[F[_] : Applicative](repo: ReviewRepository[F]): ReviewValidation[F] =
    new ReviewValidationInterpreter[F](repo)
}

