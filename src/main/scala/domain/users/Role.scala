package domain.users

import cats._
import tsec.authorization.{AuthGroup, SimpleAuthEnum}

final case class Role(roleRepr: String)

object Role extends SimpleAuthEnum[Role, String] {
  val Admin: Role = Role("Admin")
  val User: Role = Role("User")

  override val values: AuthGroup[Role] = AuthGroup(Admin, User)

  override def getRepr(t: Role): String = t.roleRepr

  implicit val eqRole: Eq[Role] = Eq.fromUniversalEquals[Role]
}
