package reasonchat.actors

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.stream.scaladsl.{Flow, SourceQueue}
import reasonchat.messages.ChatMessage
import reasonchat.models.{Channel, Client, Id, User}
import reasonchat.websocket.Reply

sealed trait ChatActionMessage
case class NewClient(userId: Id[User], sourceQueue: SourceQueue[ChatMessage]) extends ChatActionMessage
case class SetChannel(userId: Id[User], channelId: Id[Channel]) extends ChatActionMessage
case class Disconnect(userId: Id[User]) extends ChatActionMessage
case class RemoveChannel(channelId: Id[Channel]) extends ChatActionMessage
case class BroadcastIn(channelId: Id[Channel], msg: ChatMessage) extends ChatActionMessage
case class Unicast(msg: ChatMessage) extends ChatActionMessage

class ReasonChatServerActor extends Actor with ActorLogging {

  private var clients: IndexedSeq[Client] = Vector.empty[Client]

  private def updateClient(userId: Id[User], f: Client => Client): IndexedSeq[Client] = {
    val idx = clients.indexWhere(_.id == userId)
    if (idx >= 0)
      clients = clients.updated(idx, f(clients(idx)))
    clients
  }

  private def getClient(userId: Id[User]): Option[Client] = {
    val idx = clients.indexWhere(_.id == userId)
    if (idx >= 0)
      Some(clients(idx))
    else
      None
  }

  override def receive: Receive = {
    case NewClient(userId, sourceQueue) =>
      log.debug(s"Adding new client ${userId.value}")
      clients = clients :+ Client(userId, None, sourceQueue)
    case SetChannel(userId, channelId) =>
      log.debug(s"Select a channel ${channelId.value}")
      updateClient(userId, _.copy(channel = Some(channelId)))
    case Disconnect(userId) =>
      log.debug(s"Disconnect ${userId.value}")
    case RemoveChannel(channelId) =>
      log.debug(s"Remove channel ${channelId.value}")
      clients = clients.filter { client =>
        !client.channel.contains(channelId)
      }
    case BroadcastIn(channelId, msg: ChatMessage) =>
      log.debug(s"broadcast msg $msg in ${channelId.value}")
      clients.foreach { client =>
        client.channel
          .filter(_ == channelId)
          .foreach(_ => client.queue.offer(msg))
      }
    case Unicast(msg) =>
      log.debug(s"unicast msg $msg in ${msg.userId}")
      getClient(msg.userId).foreach(_.queue.offer(msg))
  }
}

object ReasonChatServerActor {
  def flow(actor: ActorRef): Flow[Reply, ChatMessage, NotUsed] =
    Flow[Reply].map { reply =>
      reply.actions.foreach(action => actor ! action)
      actor ! Unicast(reply.msg)
      reply.msg
    }
}