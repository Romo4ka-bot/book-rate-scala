package infrastructure.endpoint

import cats.effect.Sync
import cats.syntax.all._
import domain.BookAlreadyExistsError
import domain.authentication._
import domain.books.{Book, BookService}
import domain.users._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import tsec.authentication._
import tsec.jwt.algorithms.JWTMacAlgo

class BookEndpoints[F[_] : Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {

  implicit val petDecoder: EntityDecoder[F, Book] = jsonOf[F, Book]

  def createBookEndpoint(bookService: BookService[F]): AuthEndpoint[F, Auth] = {
    case req@POST -> Root asAuthed _ =>
      val action = for {
        book <- req.request.as[Book]
        result <- bookService.create(book).value
      } yield result

      action.flatMap {
        case Right(saved) =>
          Ok(saved.asJson)
        case Left(BookAlreadyExistsError(existing)) =>
          Conflict(s"The book with title = ${existing.title} already exists")
      }
  }

  def deletePetEndpoint(bookService: BookService[F]): AuthEndpoint[F, Auth] = {
    case DELETE -> Root / LongVar(id) asAuthed _ =>
      for {
        _ <- bookService.delete(id)
        resp <- Ok()
      } yield resp
  }

  def endpoints(
                 bookService: BookService[F],
                 auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]],
               ): HttpRoutes[F] = {
    val authEndpoints: AuthService[F, Auth] = {
      val allRoles =
        createBookEndpoint(bookService)
      val onlyAdmin =
        deletePetEndpoint(bookService)

      Auth.allRolesHandler(allRoles)(Auth.adminOnly(onlyAdmin))
    }

    auth.liftService(authEndpoints)
  }
}

object BookEndpoints {
  def endpoints[F[_] : Sync, Auth: JWTMacAlgo](
                                                bookService: BookService[F],
                                                auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]],
                                              ): HttpRoutes[F] =
    new BookEndpoints[F, Auth].endpoints(bookService, auth)
}