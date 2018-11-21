package reasonchat.messages

import akka.stream.scaladsl.SourceQueue
import reasonchat.dsl.ErrorCodes.ErrorCodes
import reasonchat.models.{Channel, Id, Message, User}

sealed trait ChatMessage {
  def userId: Id[User]
}
case class UserConnected(userId: Id[User], name: String, queue: SourceQueue[ChatMessage]) extends ChatMessage
case object NoReply extends ChatMessage {
  val userId = Id(0)
}
case class SelectChannel(userId: Id[User], channelId: Id[Channel]) extends ChatMessage
case class UserInfo(userId: Id[User], user: User) extends ChatMessage
case class GetChannels(userId: Id[User]) extends ChatMessage
case class SendMessage(userId: Id[User], channelId: Id[Channel], message: String) extends ChatMessage
case class NewMessage(userId: Id[User], channelId: Id[Channel], message: Message) extends ChatMessage
case class AllChannels(userId: Id[User], channels: Seq[Channel]) extends ChatMessage
case class GetMessages(userId: Id[User], channelId: Id[Channel]) extends ChatMessage
case class AllMessages(userId: Id[User], channelId: Id[Channel], messages: Seq[Message]) extends ChatMessage
case class UnknownMessage(userId: Id[User], content: String) extends ChatMessage
case class ErrorOccured(userId: Id[User], code: ErrorCodes) extends ChatMessage