package domain.reviews

case class Review(
              content: String,
              mark: Int,
              author: Option[Long],
              book: Option[Long],
              id: Option[Long] = None
            )
