package infrastructure.endpoint

import cats.effect.Sync
import cats.syntax.all._
import domain.authentication._
import domain.reviews.{Review, ReviewService}
import domain.users._
import domain.{IncorrectMarkEnteredError, NonPositiveNumberError, ReviewNotFoundError}
import infrastructure.endpoint.Pagination.{OptionalOffsetMatcher, OptionalPageSizeMatcher}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import tsec.authentication._
import tsec.jwt.algorithms.JWTMacAlgo

class ReviewEndpoints[F[_] : Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {

  implicit val reviewDecoder: EntityDecoder[F, Review] = jsonOf[F, Review]

  private def createReviewEndpoint(reviewService: ReviewService[F]): AuthEndpoint[F, Auth] = {
    case req@POST -> Root asAuthed _ =>
      val action = for {
        review <- req.request.as[Review]
        result <- reviewService.create(review).value
      } yield result

      action.flatMap {
        case Right(saved) => {
          reviewService.calculateBookRate(saved.book.get).flatMap(_ => Ok(saved.asJson))
        }
        case Left(NonPositiveNumberError) =>
          Conflict("Mark does not be non positive")
        case Left(IncorrectMarkEnteredError) =>
          Conflict("Mark does not be more 10")
      }
  }

  private def updateBookEndpoint(reviewService: ReviewService[F]): AuthEndpoint[F, Auth] = {
    case req@PUT -> Root / LongVar(_) asAuthed _ =>
      val action = for {
        review <- req.request.as[Review]
        result <- reviewService.update(review).value
      } yield result

      action.flatMap {
        case Right(saved) => {
          reviewService.calculateBookRate(saved.book.get)
          Ok(saved.asJson)
        }
        case Left(ReviewNotFoundError) => NotFound("The review was not found")
      }
  }

  private def deleteReviewEndpoint(reviewService: ReviewService[F]): AuthEndpoint[F, Auth] = {
    case DELETE -> Root / LongVar(id) asAuthed _ =>
      for {
        _ <- reviewService.delete(id)
        resp <- Ok()
      } yield resp
  }

  private def getReviewEndpoint(reviewService: ReviewService[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / LongVar(id) =>
      reviewService.get(id).value.flatMap {
        case Right(found) => Ok(found.asJson)
        case Left(ReviewNotFoundError) => NotFound("The book was not found")
      }
  }

  private def getReviewsEndpoint(reviewService: ReviewService[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? OptionalPageSizeMatcher(pageSize) :? OptionalOffsetMatcher(
    offset,
    ) =>
      for {
        reviews <- reviewService.getListOfReviews(pageSize.getOrElse(10), offset.getOrElse(0))
        resp <- Ok(reviews.asJson)
      } yield resp
  }

  def endpoints(
                 reviewService: ReviewService[F],
                 auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]],
               ): HttpRoutes[F] = {
    val authEndpoints: AuthService[F, Auth] = {
      val allRoles =
        createReviewEndpoint(reviewService).orElse(updateBookEndpoint(reviewService))
      val onlyAdmin =
        deleteReviewEndpoint(reviewService)

      Auth.allRolesHandler(allRoles)(Auth.adminOnly(onlyAdmin))
    }

    val unauthEndpoints = getReviewEndpoint(reviewService) <+> getReviewsEndpoint(reviewService)

    unauthEndpoints <+> auth.liftService(authEndpoints)
  }
}

object ReviewEndpoints {
  def endpoints[F[_] : Sync, Auth: JWTMacAlgo](
                                                reviewService: ReviewService[F],
                                                auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]],
                                              ): HttpRoutes[F] =
    new ReviewEndpoints[F, Auth].endpoints(reviewService, auth)
}