package domain.users

import cats.Applicative
import tsec.authorization.AuthorizationInfo

case class User(
                 userName: String,
                 firstName: String,
                 lastName: String,
                 email: String,
                 hashPassword: String,
                 id: Option[Long] = None,
                 role: Role,
) {
//  def mapToUserDto(user: User): UserDto = {
//    UserDto(userName = user.userName, firstName = user.firstName, lastName = user.lastName, id = user.id)
//  }
}

object User {
  implicit def authRole[F[_]](implicit F: Applicative[F]): AuthorizationInfo[F, Role, User] =
    (u: User) => F.pure(u.role)

  def mapToUserDto(user: User): UserDto = {
    UserDto(userName = user.userName, firstName = user.firstName, lastName = user.lastName, id = user.id)
  }
}
