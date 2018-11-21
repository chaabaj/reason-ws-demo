package reasonchat

import monix.execution.atomic.AtomicInt
import reasonchat.models.Id

object IdGen {

  type IdGen[A] = () => Id[A]

  def createGen[A](): IdGen[A] = {
    val id = AtomicInt(0)
    () => {
      id.add(1)
      Id[A](id.get)
    }
  }
}

