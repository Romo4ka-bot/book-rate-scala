package domain.users

import cats.Monad
import cats.data._
import cats.syntax.functor._
import domain.{UserAlreadyExistsError, UserNotFoundError}

class UserService[F[_]: Monad](userRepo: UserRepository[F], validation: UserValidation[F]) {
  def createUser(user: User): EitherT[F, UserAlreadyExistsError, User] =
    for {
      _ <- validation.doesNotExist(user)
      saved <- EitherT.liftF(userRepo.create(user))
    } yield saved

  def getUser(userId: Long): EitherT[F, UserNotFoundError.type, User] =
    userRepo.get(userId).toRight(UserNotFoundError)

  def getUserByName(
      userName: String,
  ): EitherT[F, UserNotFoundError.type, User] =
    userRepo.findByUserName(userName).toRight(UserNotFoundError)

  def deleteUser(userId: Long): F[Unit] =
    userRepo.delete(userId).value.void

  def deleteByUserName(userName: String): F[Unit] =
    userRepo.deleteByUserName(userName).value.void

  def update(user: User): EitherT[F, UserNotFoundError.type, User] =
    for {
      _ <- validation.exists(user.id)
      saved <- userRepo.update(user).toRight(UserNotFoundError)
    } yield saved

  def list(page: Int, size: Int): F[List[User]] =
    userRepo.list(page, size)
}

object UserService {
  def apply[F[_]: Monad](
                   repository: UserRepository[F],
                   validation: UserValidation[F],
  ): UserService[F] =
    new UserService[F](repository, validation)
}
