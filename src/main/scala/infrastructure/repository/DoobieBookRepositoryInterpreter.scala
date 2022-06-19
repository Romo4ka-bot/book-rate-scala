package infrastructure.repository

import cats.data._
import cats.syntax.all._
import doobie._
import doobie.implicits._
import cats.effect.Bracket
import domain.books.{Book, BookRepository}
import doobie.{Transactor, Update0}
import infrastructure.repository.SQLPagination.paginate

private object BookSQL {
  def insert(book: Book): Update0 =
    sql"""
    INSERT INTO BOOK (TITLE, DESCRIPTION, AUTHOR, RATE, PUSHED_BY)
    VALUES (${book.title}, ${book.description}, ${book.author}, ${book.rate}, ${book.pushedBy})
  """.update

  def update(book: Book, id: Long): Update0 = sql"""
    UPDATE BOOK
    SET TITLE = ${book.title}, DESCRIPTION =  ${book.description}, AUTHOR = ${book.author}
    WHERE ID = $id
  """.update

  def select(id: Long): Query0[Book] =
    sql"""
    SELECT TITLE, DESCRIPTION, AUTHOR, RATE, PUSHED_BY, ID
    FROM BOOK
    WHERE ID = $id
  """.query

  def delete(id: Long): Update0 =
    sql"""
    DELETE FROM BOOK WHERE ID = $id
  """.update

  def selectByTitleAndAuthor(title: String, author: String): Query0[Book] = sql"""
    SELECT TITLE, DESCRIPTION, AUTHOR, RATE, PUSHED_BY, ID
    FROM BOOK
    WHERE TITLE = $title AND AUTHOR = $author
  """.query[Book]

  def selectAll: Query0[Book] = sql"""
    SELECT TITLE, DESCRIPTION, AUTHOR, RATE, PUSHED_BY, ID
    FROM BOOK
    ORDER BY TITLE
  """.query
}

class DoobieBookRepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](val xa: Transactor[F])
  extends BookRepository[F] {
  import BookSQL._

  override def create(book: Book): F[Book] = insert(book).withUniqueGeneratedKeys[Long]("id").map(id => book.copy(id = id.some)).transact(xa)

  override def update(book: Book): F[Option[Book]] = OptionT
    .fromOption[ConnectionIO](book.id)
    .semiflatMap(id => BookSQL.update(book, id).run.as(book))
    .value
    .transact(xa)

  override def get(id: Long): F[Option[Book]] = select(id).option.transact(xa)

  override def delete(id: Long): F[Option[Book]] =
    OptionT(select(id).option).semiflatMap(book => BookSQL.delete(id).run.as(book)).value.transact(xa)

  override def findByTitleAndAuthor(title: String, author: String): F[Set[Book]] =
    selectByTitleAndAuthor(title, author).to[List].transact(xa).map(_.toSet)

  def findAll(pageSize: Int, offset: Int): F[List[Book]] =
    paginate(pageSize, offset)(selectAll).to[List].transact(xa)
}

object DoobieBookRepositoryInterpreter {
  def apply[F[_]: Bracket[*[_], Throwable]](xa: Transactor[F]): DoobieBookRepositoryInterpreter[F] =
    new DoobieBookRepositoryInterpreter(xa)
}