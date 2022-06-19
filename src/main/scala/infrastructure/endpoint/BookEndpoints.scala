package infrastructure.endpoint

import cats.effect.Sync
import cats.syntax.all._
import domain.{BookAlreadyExistsError, BookNotFoundError}
import domain.authentication._
import domain.books.{Book, BookService}
import domain.users._
import infrastructure.endpoint.Pagination.{OptionalOffsetMatcher, OptionalPageSizeMatcher}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import tsec.authentication._
import tsec.jwt.algorithms.JWTMacAlgo

class BookEndpoints[F[_] : Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {

  implicit val bookDecoder: EntityDecoder[F, Book] = jsonOf[F, Book]

  private def createBookEndpoint(bookService: BookService[F]): AuthEndpoint[F, Auth] = {
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

  private def deleteBookEndpoint(bookService: BookService[F]): AuthEndpoint[F, Auth] = {
    case DELETE -> Root / LongVar(id) asAuthed _ =>
      for {
        _ <- bookService.delete(id)
        resp <- Ok()
      } yield resp
  }

  private def updateBookEndpoint(bookService: BookService[F]): AuthEndpoint[F, Auth] = {
    case req @ PUT -> Root / LongVar(_) asAuthed _ =>
      val action = for {
        book <- req.request.as[Book]
        result <- bookService.update(book).value
      } yield result

      action.flatMap {
        case Right(saved) => Ok(saved.asJson)
        case Left(BookNotFoundError) => NotFound("The book was not found")
      }
  }

  private def getBookEndpoint(bookService: BookService[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / LongVar(id) =>
      bookService.get(id).value.flatMap {
        case Right(found) => Ok(found.asJson)
        case Left(BookNotFoundError) => NotFound("The book was not found")
      }
  }

  private def getBooksEndpoint(bookService: BookService[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? OptionalPageSizeMatcher(pageSize) :? OptionalOffsetMatcher(
    offset,
    ) =>
      for {
        books <- bookService.getListOfBooks(pageSize.getOrElse(10), offset.getOrElse(0))
        resp <- Ok(books.asJson)
      } yield resp
  }

  def endpoints(
                 bookService: BookService[F],
                 auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]],
               ): HttpRoutes[F] = {
    val authEndpoints: AuthService[F, Auth] = {
      val allRoles =
        createBookEndpoint(bookService).orElse(updateBookEndpoint(bookService))
      val onlyAdmin =
        deleteBookEndpoint(bookService)

      Auth.allRolesHandler(allRoles)(Auth.adminOnly(onlyAdmin))
    }

    val unauthEndpoints = getBookEndpoint(bookService) <+> getBooksEndpoint(bookService)

    unauthEndpoints <+> auth.liftService(authEndpoints)
  }
}

object BookEndpoints {
  def endpoints[F[_] : Sync, Auth: JWTMacAlgo](
                                                bookService: BookService[F],
                                                auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]],
                                              ): HttpRoutes[F] =
    new BookEndpoints[F, Auth].endpoints(bookService, auth)
}