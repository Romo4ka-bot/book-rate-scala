package infrastructure.repository

import cats.data._
import cats.syntax.all._
import doobie._
import doobie.implicits._
import cats.effect.Bracket
import domain.reviews.{Review, ReviewRepository}
import doobie.{Transactor, Update0}
import infrastructure.repository.SQLPagination.paginate

private object ReviewSQL {
  def insert(review: Review): Update0 =
    sql"""
    INSERT INTO REVIEW (CONTENT, MARK, AUTHOR, BOOK)
    VALUES (${review.content}, ${review.mark}, ${review.author}, ${review.book})
  """.update

  def update(review: Review, id: Long): Update0 =
    sql"""
    UPDATE REVIEW
    SET TITLE = ${review.content}, AUTHOR = ${review.mark}
    WHERE ID = $id
  """.update

  def select(id: Long): Query0[Review] =
    sql"""
    SELECT CONTENT, MARK, AUTHOR, BOOK, ID
    FROM REVIEW
    WHERE ID = $id
  """.query

  def delete(id: Long): Update0 =
    sql"""
    DELETE FROM REVIEW WHERE ID = $id
  """.update

  def selectAll: Query0[Review] =
    sql"""
    SELECT CONTENT, MARK, AUTHOR, BOOK, ID
    FROM REVIEW
    ORDER BY TITLE
  """.query

  def selectAllByBookId(bookId: Long): Query0[Review] =
    sql"""
    SELECT CONTENT, MARK, AUTHOR, BOOK, ID
    FROM REVIEW
    WHERE BOOK = $bookId
       """.query
}

class DoobieReviewRepositoryInterpreter[F[_] : Bracket[*[_], Throwable]](val xa: Transactor[F])
  extends ReviewRepository[F] {

  import ReviewSQL._

  override def create(review: Review): F[Review] = insert(review).withUniqueGeneratedKeys[Long]("id").map(id => review.copy(id = id.some)).transact(xa)

  override def update(review: Review): F[Option[Review]] = OptionT
    .fromOption[ConnectionIO](review.id)
    .semiflatMap(id => ReviewSQL.update(review, id).run.as(review))
    .value
    .transact(xa)

  override def get(id: Long): F[Option[Review]] = select(id).option.transact(xa)

  override def delete(id: Long): F[Option[Review]] =
    OptionT(select(id).option).semiflatMap(book => ReviewSQL.delete(id).run.as(book)).value.transact(xa)

  override def findAll(pageSize: Int, offset: Int): F[List[Review]] =
    paginate(pageSize, offset)(selectAll).to[List].transact(xa)

  override def findAllByBookId(bookId: Long): F[List[Review]] = selectAllByBookId(bookId).to[List].transact(xa)
}

object DoobieReviewRepositoryInterpreter {
  def apply[F[_] : Bracket[*[_], Throwable]](xa: Transactor[F]): DoobieReviewRepositoryInterpreter[F] =
    new DoobieReviewRepositoryInterpreter(xa)
}