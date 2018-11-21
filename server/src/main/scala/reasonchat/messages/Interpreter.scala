package reasonchat.messages

import akka.NotUsed
import akka.stream.scaladsl.Flow
import cats.Applicative
import monix.eval.Task
import monix.execution.Scheduler
import reasonchat.actors.{BroadcastIn, NewClient, SetChannel}
import reasonchat.dsl.Result.Result
import reasonchat.dsl._
import reasonchat.models.{Id, User}
import reasonchat.websocket.Reply

import scala.concurrent.Future

trait ChatMessageInterpreter[F[_]] {
  def interpreter(channelDSL: ChannelDSL[F], userDSL: UserDSL[F]): Flow[ChatMessage, Reply, NotUsed]
}

trait DSLExecution {
  def execute[F[_]: Applicative, A](userId: Id[User], result: => Result[F, A])(f: A => Reply): F[Reply] = {
    val applicativeInstance = implicitly[Applicative[F]]
    applicativeInstance.map(result) {
      case Right(a) => f(a)
      case Left(error: AppError) =>
        Reply(ErrorOccured(userId, error.code))
      case Left(error) =>
        Reply(ErrorOccured(userId, ErrorCodes.InternalError))
    }
  }
}

class ChatMessageInterpreterAsync(implicit scheduler: Scheduler) extends ChatMessageInterpreter[Task] with DSLExecution {

  override def interpreter(channelDSL: ChannelDSL[Task], userDSL: UserDSL[Task]): Flow[ChatMessage, Reply, NotUsed] =
    Flow[ChatMessage].mapAsync(1) {
      case msg: UnknownMessage =>
        Future.successful(Reply(msg))
      case UserConnected(userId, name, queue) =>
        execute(userId, userDSL.addUser(User(userId, name))) { user =>
          Reply(UserInfo(userId, user), Seq(NewClient(userId, queue)))
        }.runAsync
      case GetChannels(userId) =>
        execute(userId, channelDSL.getAllChannels) { channels =>
           Reply(AllChannels(userId, channels))
        }.runAsync
      case msg: SendMessage =>
        execute(msg.userId, channelDSL.addMessage(msg.channelId, msg.userId, msg.message)) {
          newMessage => Reply(
            NoReply, Seq(
              BroadcastIn(
                msg.channelId, NewMessage(msg.userId, msg.channelId, newMessage)
            )
          )
        )
        }.runAsync
      case msg: SelectChannel =>
        Future.successful(Reply(NoReply, Seq(SetChannel(msg.userId, msg.channelId))))
      case GetMessages(userId, channelId) =>
        execute(userId, channelDSL.getMessages(channelId)) {
          messages => Reply(AllMessages(userId, channelId, messages))
        }.runAsync
      case _ =>
        Future.successful(Reply(NoReply))
    }
}

