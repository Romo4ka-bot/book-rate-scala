package domain.users

case class UserDto(
                 userName: String,
                 firstName: String,
                 lastName: String,
                 id: Option[Long] = None
               )
