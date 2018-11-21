package reasonchat.dsl

import org.apache.logging.log4j.LogManager
import reasonchat.dsl.Result.Result
import reasonchat.shared.DeferEffect

import scala.util.Try
import scala.util.control.NonFatal

trait LogDSL[F[_]] {
  def debug(msg: String): Result[F, Unit]
  def trace(msg: String): Result[F, Unit]
  def error(msg: String, err: Option[AppError] = None): Result[F, Unit]
  def warn(msg: String): Result[F, Unit]
}

/**
  * Generic logger for any Type that have a Lazy typeclass instance
  * @tparam F
  */
class Log4jDSL[F[_]: DeferEffect] extends LogDSL[F] {
  private val logger = LogManager.getLogger

  private val deferEffectInstance = implicitly(DeferEffect[F])

  /**
    * Just in case log4j2 throw some exception we can handle it here
    * @param f
    * @return
    */
  private def handleException(f: => Unit): Result[F, Unit] =
    deferEffectInstance.deferEffect {
      Try(f).map(Right(_)).recover {
        case NonFatal(ex) =>
          Left(LoggingFailed(ex))
      }.get
    }

  override def debug(msg: String): Result[F, Unit] =
    handleException(logger.debug(msg))

  override def error(msg: String, err: Option[AppError]): Result[F, Unit] =
    handleException(logger.error(msg, err))

  override def trace(msg: String): Result[F, Unit] =
    handleException(logger.trace(msg))

  override def warn(msg: String): Result[F, Unit] =
    handleException(logger.warn(msg))
}