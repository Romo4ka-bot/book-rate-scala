import cats.effect._
import config._
import domain.authentication.Auth
import domain.books.{BookService, BookValidationInterpreter}
import domain.reviews.{ReviewService, ReviewValidationInterpreter}
import domain.users.{UserService, UserValidationInterpreter}
import doobie.util.ExecutionContexts
import infrastructure.endpoint.{BookEndpoints, ReviewEndpoints, UserEndpoints}
import infrastructure.repository.{DoobieAuthRepositoryInterpreter, DoobieBookRepositoryInterpreter, DoobieReviewRepositoryInterpreter, DoobieUserRepositoryInterpreter}
import io.circe.config.parser
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.{Router, Server => H4Server}
import tsec.authentication.SecuredRequestHandler
import tsec.mac.jca.HMACSHA256
import tsec.passwordhashers.jca.BCrypt

object Server extends IOApp {
  def createServer[F[_]: ContextShift: ConcurrentEffect: Timer]: Resource[F, H4Server[F]] =
    for {
      conf <- Resource.eval(parser.decodePathF[F, BookRateConfig]("bookrate"))
      serverEc <- ExecutionContexts.cachedThreadPool[F]
      connEc <- ExecutionContexts.fixedThreadPool[F](conf.db.connections.poolSize)
      txnEc <- ExecutionContexts.cachedThreadPool[F]
      xa <- DatabaseConfig.dbTransactor(conf.db, connEc, Blocker.liftExecutionContext(txnEc))
      key <- Resource.eval(HMACSHA256.generateKey[F])
      authRepo = DoobieAuthRepositoryInterpreter[F, HMACSHA256](key, xa)
      userRepo = DoobieUserRepositoryInterpreter[F](xa)
      bookRepo = DoobieBookRepositoryInterpreter[F](xa)
      reviewRepo = DoobieReviewRepositoryInterpreter[F](xa)
      userValidation = UserValidationInterpreter[F](userRepo)
      bookValidation = BookValidationInterpreter[F](bookRepo)
      reviewValidation = ReviewValidationInterpreter[F](reviewRepo)
      userService = UserService[F](userRepo, userValidation)
      bookService = BookService[F](bookRepo, bookValidation)
      reviewService = ReviewService[F](reviewRepo, reviewValidation)
      authenticator = Auth.jwtAuthenticator[F, HMACSHA256](key, authRepo, userRepo)
      routeAuth = SecuredRequestHandler(authenticator)
      httpApp = Router(
        "/users" -> UserEndpoints
          .endpoints[F, BCrypt, HMACSHA256](userService, BCrypt.syncPasswordHasher[F], routeAuth),
        "/books" -> BookEndpoints.endpoints[F, HMACSHA256](bookService, routeAuth),
        "/reviews" -> ReviewEndpoints.endpoints[F, HMACSHA256](reviewService, routeAuth)
      ).orNotFound
      _ <- Resource.eval(DatabaseConfig.initializeDb(conf.db))
      server <- BlazeServerBuilder[F](serverEc)
        .bindHttp(conf.server.port, conf.server.host)
        .withHttpApp(httpApp)
        .resource
    } yield server

  def run(args: List[String]): IO[ExitCode] = createServer.use(_ => IO.never).as(ExitCode.Success)
}
