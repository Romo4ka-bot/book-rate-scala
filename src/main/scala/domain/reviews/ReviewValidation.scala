package domain.reviews

import cats.data.EitherT
import domain.{ReviewNotFoundError, ValidationMarkError}

trait ReviewValidation[F[_]] {
  def verifyMark(review: Review): EitherT[F, ValidationMarkError, Unit]

  def exists(reviewId: Option[Long]): EitherT[F, ReviewNotFoundError.type, Unit]
}

