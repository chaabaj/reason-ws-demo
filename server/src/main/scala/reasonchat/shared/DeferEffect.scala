package reasonchat.shared

import cats.Applicative
import monix.eval.Task
import simulacrum.typeclass

@typeclass
trait DeferEffect[F[_]] {
  def deferEffect[A](a: => A): F[A]
}

object DeferEffectInstance {
  implicit val taskDefer = new DeferEffect[Task] {
    override def deferEffect[A](a: => A): Task[A] =
      Task(a)
  }
}