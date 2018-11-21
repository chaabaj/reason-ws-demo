package reasonchat.models

import akka.stream.scaladsl.SourceQueue
import reasonchat.messages.ChatMessage

case class Client(
  id: Id[User],
  channel: Option[Id[Channel]] = None,
  queue: SourceQueue[ChatMessage]
)
