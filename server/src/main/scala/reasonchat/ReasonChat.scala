package reasonchat

import java.io.File

import akka.actor.{ActorSystem, Props}
import akka.stream.scaladsl.{Broadcast, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import monix.eval.Task
import monix.reactive.Observable
import reasonchat.actors.{Disconnect, ReasonChatServerActor}
import reasonchat.dsl._
import reasonchat.infrastructure.InMemoryStoreDSL
import reasonchat.messages.{ChatMessage, ChatMessageInterpreterAsync}
import reasonchat.models.{Channel, Id, Message, User}
import reasonchat.websocket.{ConnectionHandler, Server}
import spray.json._
import Result.syntax._
import reasonchat.dsl.Result.Result
import DefaultJsonProtocol._
import akka.http.scaladsl.Http
import reasonchat.messages.JsonProtocol._
import reasonchat.shared.DeferEffectInstance._

import scala.util.control.NonFatal

object ReasonChat {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val exc = monix.execution.Scheduler.Implicits.global

  val reasonChatServerActor = system.actorOf(Props(classOf[ReasonChatServerActor]))

  val MaxBufferSize = 16134

  type TaskResult[A] = Result[Task, A]

  private def buildChannelStore(channels: Seq[Channel]): TaskResult[InMemoryStoreDSL[Id[Channel], Channel]] = {
    val store = new InMemoryStoreDSL[Id[Channel], Channel]()
    Observable.fromIterator(channels.iterator)
      .mapTask(channel => store.put(channel.id, channel))
      .toListL
      .map(Result.sequence)
      .handleError
      .map(_ => store)
      .value
  }

  private def buildMessageStore(channels: Seq[Channel]): TaskResult[InMemoryStoreDSL[Id[Channel], Seq[Message]]] = {
    val store = new InMemoryStoreDSL[Id[Channel], Seq[Message]]()
    val robotUser = User(
      id = Id(0),
      name = "Robot"
    )
    Observable.fromIterator(channels.iterator)
      .mapTask(channel => store.put(channel.id, Seq(Message(Id(0), "Hello there", robotUser))))
      .toListL
      .map(Result.sequence)
      .handleError
      .map(_ => store)
      .value
  }


  private val messageActorSource =
    Source.queue[ChatMessage](MaxBufferSize, OverflowStrategy.fail)

  def startServer(connectionHandler: ConnectionHandler): Result[Task, Http.ServerBinding] =
    Task.deferFuture {
      Server.start(10000, connectionHandler)
        .map(Right(_))
        .recover {
          case NonFatal(ex) =>
            Left(InternalError(ex): AppError)
        }
    }

  def main(args: Array[String]): Unit = {

    val channels = scala.io.Source.fromResource("channels.json")
      .getLines()
      .mkString
      .parseJson
      .convertTo[Seq[Channel]]

    val logDSL = new Log4jDSL[Task]

    val startupTask = for {
      channelStore <-buildChannelStore(channels).handleError
      messageStore <- buildMessageStore(channels).handleError
      userDsl = new UserDSL[Task](new InMemoryStoreDSL(), logDSL)
      channelDsl = new ChannelDSL[Task](channelStore, messageStore, userDsl, logDSL)
      chatInterpreter = new ChatMessageInterpreterAsync()
        .interpreter(
          channelDsl,
          userDsl
        )
      disconnectSink = (userId: Id[User]) => Sink.combine(
        Sink.actorRef[ChatMessage](
          reasonChatServerActor,
          0
        ),
        Sink.onComplete[ChatMessage] { _ =>
          userDsl.deleteUser(userId).map {
            case Left(error) =>
              throw error
            case other =>
              other
          }.onErrorRestart(10)
            .runAsync
          reasonChatServerActor ! Disconnect(userId)
        }
      )(Broadcast[ChatMessage](_))
      connectionHandler = ConnectionHandler(
        messageSource = messageActorSource,
        idGen = IdGen.createGen(),
        msgServer = ReasonChatServerActor.flow(reasonChatServerActor),
        msgInterpreter = chatInterpreter,
        disconnectSink = disconnectSink
      )
      binding <- startServer(connectionHandler).handleError
    } yield binding


    startupTask
      .value
      .map {
        case Left(error) =>
          error.printStackTrace()
          system.terminate()
        case Right(_) =>
          println("Listening on port 10000")
      }
      .onErrorRecover {
        case NonFatal(ex) =>
          ex.printStackTrace()
          system.terminate()
      }
      .runAsync
  }
}
