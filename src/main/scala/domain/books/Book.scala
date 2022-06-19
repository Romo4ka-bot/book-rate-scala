package domain.books

case class Book(
                 title: String,
                 description: String,
                 author: String,
                 rate: Float,
                 pushedBy: Option[Long],
                 id: Option[Long] = None
               )
