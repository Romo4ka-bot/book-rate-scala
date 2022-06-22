package domain.books

trait BookRepository[F[_]] {
  def updateRateById(rate: Float, id: Option[Long]): F[Option[Long]]

  def findByTitleAndAuthor(title: String, author: String): F[Set[Book]]

  def create(book: Book): F[Book]

  def update(book: Book): F[Option[Book]]

  def get(id: Long): F[Option[Book]]

  def delete(id: Long): F[Option[Book]]

  def findAll(pageSize: Int, offset: Int): F[List[Book]]
}
