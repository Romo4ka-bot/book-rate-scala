package domain.users

import cats.data.EitherT
import domain.{UserAlreadyExistsError, UserNotFoundError}

trait UserValidation[F[_]] {
  def doesNotExist(user: User): EitherT[F, UserAlreadyExistsError, Unit]

  def exists(userId: Option[Long]): EitherT[F, UserNotFoundError.type, Unit]
}
