package domain

import domain.books.Book
import users.User

sealed trait ValidationError extends Product with Serializable
case object UserNotFoundError extends ValidationError
case class UserAlreadyExistsError(user: User) extends ValidationError
case class UserAuthenticationFailedError(userName: String) extends ValidationError
case class BookAlreadyExistsError(book: Book) extends ValidationError
case object BookNotFoundError extends ValidationError
case object ReviewNotFoundError extends ValidationError
sealed trait ValidationMarkError extends ValidationError
case object NonPositiveNumberError extends ValidationMarkError
case object IncorrectMarkEnteredError extends ValidationMarkError
