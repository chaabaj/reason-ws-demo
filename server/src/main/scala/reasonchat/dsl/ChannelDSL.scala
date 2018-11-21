package reasonchat.dsl

import cats.Monad
import reasonchat.dsl.ChannelDSL.{ChannelStore, MessageStore}
import reasonchat.models.{Channel, Id, Message, User}
import reasonchat.IdGen
import reasonchat.dsl.Result.Result
import Result.syntax._

object ChannelDSL {
  type ChannelStore[F[_]] = StoreDSL[F, Id[Channel], Channel]
  type MessageStore[F[_]] = StoreDSL[F, Id[Channel], Seq[Message]]

  val idGen = IdGen.createGen[Channel]()
  val messageIdGen = IdGen.createGen[Message]()
}

class ChannelDSL[F[_]: Monad](channelStore: ChannelStore[F],
                              messageStore: MessageStore[F],
                              userDSL: UserDSL[F],
                              logDSL: LogDSL[F]) {


  def getAllChannels: Result[F, Seq[Channel]] =
    channelStore.getAll

  def addChannel(name: String): Result[F, Channel] = {
    val id = ChannelDSL.idGen()
    (for {
      _ <- logDSL.trace(s"Adding new channel $name").handleError
      newChannel <- channelStore.put(id, Channel(id, name)).handleError
      messages <- messageStore.put(id, Seq()).handleError
    } yield newChannel).value
  }

  def deleteChannel(channelId: Id[Channel]): Result[F, Id[Channel]] = {
    (for {
      _ <- logDSL.trace(s"deleting channel $channelId").handleError
      deleted <- channelStore.delete(channelId).handleError
      deletedMessages <- messageStore.delete(channelId).handleError
    } yield channelId).value
  }

  def addMessage(channelId: Id[Channel], userId: Id[User], content: String): Result[F, Message] =
    (for {
      _ <- logDSL.trace(s"add message to the channel $channelId from $userId").handleError
      user <- userDSL.getUser(userId).handleError
      messages <- messageStore.get(channelId).handleError
      message = Message(ChannelDSL.messageIdGen(), content, user)
      updated <- messageStore.put(channelId, messages :+ message).handleError
    } yield message).value

  def getMessages(channelId: Id[Channel]): Result[F, Seq[Message]] =
    messageStore.get(channelId)
}
