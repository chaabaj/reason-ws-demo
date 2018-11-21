package reasonchat.dsl

import reasonchat.dsl.ErrorCodes.ErrorCodes

import scala.reflect.ClassTag

object ErrorCodes extends Enumeration {
  type ErrorCodes = Value

  val NotFound = Value(1)
  val NicknameAlreadyTaken = Value(2)
  val InternalError = Value(3)
}

sealed trait AppError extends RuntimeException {
  val code: ErrorCodes
}
case class NotFound[Key, A](key: Key)(implicit classTag: ClassTag[A]) extends AppError {
  override val code: ErrorCodes = ErrorCodes.NotFound
  override def getMessage: String =
    s"Cannot found object of type $classTag with key $key"
}
case class NicknameAlreadyTaken(nickname: String) extends AppError {
  override val code: ErrorCodes = ErrorCodes.NicknameAlreadyTaken
  override def getMessage: String =
    s"Nickname $nickname is already taken"
}

case class LoggingFailed(ex: Throwable) extends AppError {
  override val code: ErrorCodes = ErrorCodes.InternalError
  override def getMessage: String =
    s"Cannot log: ${ex.getMessage}"

  override def getCause: Throwable = ex
}

case class InternalError(ex: Throwable) extends AppError {
  override val code: ErrorCodes = ErrorCodes.InternalError
  override def getCause: Throwable = ex
}