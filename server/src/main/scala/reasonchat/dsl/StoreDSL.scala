package reasonchat.dsl

import reasonchat.dsl.Result.Result

trait StoreDSL[F[_], Key, A] {
  def getAll: Result[F, Seq[A]]
  def put(key: Key, obj: A): Result[F, A]
  def get(id: Key): Result[F, A]
  def delete(id: Key): Result[F, Unit]
}

