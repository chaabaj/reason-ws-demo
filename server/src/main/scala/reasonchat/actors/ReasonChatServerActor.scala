package reasonchat.actors

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.stream.scaladsl.{Flow, SourceQueue}
import reasonchat.messages.ChatMessage
import reasonchat.models.{Channel, Client, Id, User}
import reasonchat.websocket.Reply

import scala.collection.immutable.HashMap

sealed trait ChatActionMessage
case class NewClient(userId: Id[User], sourceQueue: SourceQueue[ChatMessage]) extends ChatActionMessage
case class SetChannel(userId: Id[User], channelId: Id[Channel]) extends ChatActionMessage
case class Disconnect(userId: Id[User]) extends ChatActionMessage
case class RemoveChannel(channelId: Id[Channel]) extends ChatActionMessage
case class BroadcastIn(channelId: Id[Channel], msg: ChatMessage) extends ChatActionMessage
case class Unicast(msg: ChatMessage) extends ChatActionMessage

class ReasonChatServerActor extends Actor with ActorLogging {

  private var clients: HashMap[Id[User], Client] = HashMap.empty[Id[User], Client]

  private def updateClient(userId: Id[User], f: Client => Client): HashMap[Id[User], Client] = {
    clients.get(userId)
      .map(f)
      .map(clients.updated(userId, _))
      .getOrElse(clients)
  }

  override def receive: Receive = {
    case NewClient(userId, sourceQueue) =>
      log.debug(s"Adding new client ${userId.value}")
      clients = clients + (userId -> Client(userId, None, sourceQueue))
    case SetChannel(userId, channelId) =>
      log.debug(s"Select a channel ${channelId.value}")
      clients = updateClient(userId, _.copy(channel = Some(channelId)))
    case Disconnect(userId) =>
      log.debug(s"Disconnect ${userId.value}")
    case RemoveChannel(channelId) =>
      log.debug(s"Remove channel ${channelId.value}")
      clients = clients.filter {
        case (userId, client) =>
          !client.channel.contains(channelId)
      }
    case BroadcastIn(channelId, msg: ChatMessage) =>
      log.debug(s"broadcast msg $msg in ${channelId.value}")
      clients.foreach {
        case (userId, client) if client.channel.contains(channelId) =>
          client.queue.offer(msg)
        case _ => ()
      }
    case Unicast(msg) =>
      log.debug(s"unicast msg $msg in ${msg.userId}")
      clients.get(msg.userId).foreach(_.queue.offer(msg))
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