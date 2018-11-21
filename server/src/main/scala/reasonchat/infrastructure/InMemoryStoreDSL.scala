package reasonchat.infrastructure

import monix.eval.Task
import reasonchat.dsl.{NotFound, Result, StoreDSL}
import reasonchat.dsl.Result.Result

import scala.collection.immutable.HashMap
import scala.reflect.ClassTag

class InMemoryStoreDSL[Key, A](implicit classTag: ClassTag[A]) extends StoreDSL[Task, Key, A] {
  private var store = HashMap.empty[Key, A]

  type TaskResult[B] = Result[Task, B]

  override def getAll: TaskResult[Seq[A]] =
    Result.success[Task, Seq[A]](store.toSeq.map(_._2))

  override def get(key: Key): TaskResult[A] =
    if (store.contains(key))
      Result.success[Task, A](store(key))
    else
      Result.error[Task, A](NotFound[Key, A](key))

  override def put(key: Key, obj: A): TaskResult[A] = {
    Task {
      if (store.contains(key))
        store = store.updated(key, obj)
      else
        store = store + (key -> obj)
      Right(obj)
    }
  }

  override def delete(key: Key): TaskResult[Unit] =
    Task {
      store = store - key
      Right(())
    }
}
