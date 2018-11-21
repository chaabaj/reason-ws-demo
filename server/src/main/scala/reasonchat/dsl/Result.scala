package reasonchat.dsl

import cats.data.EitherT
import cats.{Applicative, Monad}
import cats.implicits._

object Result {
  type Result[F[_], A] = F[Either[AppError, A]]

  def success[F[_]: Applicative, A](a: A): Result[F, A] = {
    val applyInstance = implicitly[Applicative[F]]
    applyInstance.pure(Right(a))
  }

  def error[F[_]: Applicative, A](error: AppError): Result[F, A] = {
    val applyInstance = implicitly[Applicative[F]]
    applyInstance.pure(Left(error))
  }

  def sequence[A](results: Seq[Either[AppError, A]]): Either[AppError, Seq[A]] = {
    type Res[B] = Either[AppError, B]
    results.toList.sequence[Res, A]
  }

  object syntax {
    implicit class ResultMonadOps[F[_]: Monad, A](result: Result[F, A]) {
      def handleError: EitherT[F, AppError, A] =
        EitherT(result)
    }
  }
}
