package infrastructure.repository

import cats.data._
import cats.syntax.all._
import doobie._
import doobie.implicits._
import cats.effect.Bracket
import domain.books.{Book, BookRepository}
import doobie.{Transactor, Update0}

private object BookSQL {
  def insert(book: Book): Update0 =
    sql"""
    INSERT INTO BOOK (TITLE, AUTHOR, RATE, PUSHED_BY)
    VALUES (${book.title}, ${book.author}, ${book.rate}, ${book.pushedBy})
  """.update

  def update(book: Book, id: Long): Update0 = sql"""
    UPDATE BOOK
    SET TITLE = ${book.title}, AUTHOR = ${book.author}, RATE = ${book.rate}, PUSHED_BY = ${book.pushedBy}
    WHERE id = $id
  """.update

  def select(id: Long): Query0[Book] =
    sql"""
    SELECT TITLE, AUTHOR, RATE, PUSHED_BY, ID
    FROM BOOK
    WHERE ID = $id
  """.query

  def delete(id: Long): Update0 =
    sql"""
    DELETE FROM BOOK WHERE ID = $id
  """.update

  def selectByTitleAndAuthor(title: String, author: String): Query0[Book] = sql"""
    SELECT TITLE, AUTHOR, RATE, PUSHED_BY, ID
    FROM BOOK
    WHERE TITLE = $title AND AUTHOR = $author
  """.query[Book]
}

class DoobieBookRepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](val xa: Transactor[F])
  extends BookRepository[F] {
  import BookSQL._

  override def create(book: Book): F[Book] = insert(book).withUniqueGeneratedKeys[Long]("ID").map(id => book.copy(id = id.some)).transact(xa)

  override def update(book: Book): F[Option[Book]] = OptionT
    .fromOption[ConnectionIO](book.id)
    .semiflatMap(id => BookSQL.update(book, id).run.as(book))
    .value
    .transact(xa)

  override def get(id: Long): F[Option[Book]] = select(id).option.transact(xa)

  override def delete(id: Long): F[Option[Book]] =
    OptionT(select(id).option).semiflatMap(book => BookSQL.delete(id).run.as(book)).value.transact(xa)

  override def findByTitleAndAuthor(title: String, author: String): F[Set[Book]] = {
    selectByTitleAndAuthor(title, author).to[List].transact(xa).map(_.toSet)
  }
}

object DoobieBookRepositoryInterpreter {
  def apply[F[_]: Bracket[*[_], Throwable]](xa: Transactor[F]): DoobieBookRepositoryInterpreter[F] =
    new DoobieBookRepositoryInterpreter(xa)
}