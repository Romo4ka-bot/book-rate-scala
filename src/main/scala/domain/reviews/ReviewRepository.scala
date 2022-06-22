package domain.reviews

trait ReviewRepository [F[_]] {

  def create(review: Review): F[Review]

  def update(review: Review): F[Option[Review]]

  def get(id: Long): F[Option[Review]]

  def delete(id: Long): F[Option[Review]]

  def findAll(pageSize: Int, offset: Int): F[List[Review]]

  def findAllByBookId(bookId: Long): F[List[Review]]
}
