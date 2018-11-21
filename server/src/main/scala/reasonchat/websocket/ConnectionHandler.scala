package reasonchat.websocket

import akka.NotUsed
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Sink, Source, SourceQueueWithComplete}
import reasonchat.IdGen.IdGen
import reasonchat.actors.ChatActionMessage
import reasonchat.messages.{ChatMessage, UnknownMessage, UserConnected}
import reasonchat.models.{Id, User}
import spray.json._
import reasonchat.messages.JsonProtocol._

import scala.util.Try

case class Reply(msg: ChatMessage, actions: Seq[ChatActionMessage] = Seq())

class ConnectionHandler(messageSource: Source[ChatMessage, SourceQueueWithComplete[ChatMessage]],
                        idGen: IdGen[User],
                        msgServer: Flow[Reply, ChatMessage, NotUsed],
                        msgInterpreter: Flow[ChatMessage, Reply, NotUsed],
                        disconnectSink: Id[User] => Sink[ChatMessage, NotUsed]) {

  def create(nickname: String): Flow[Message, Message, Any] =
    Flow.fromGraph(GraphDSL.create(messageSource) {
      implicit builder =>
        messageSourceActor => {
          import GraphDSL.Implicits._
          val userId = idGen()
          val clientSourceMat = builder.materializedValue.map { sourceQueue =>
            UserConnected(userId, nickname, sourceQueue)
          }.via(msgInterpreter)

          val merge = builder.add(Merge[Reply](2))
          val inputMessage = builder.add(
            Flow[Message].collect {
              case TextMessage.Strict(text) =>
                Try(text.parseJson.convertTo[ChatMessage]).getOrElse(UnknownMessage(userId, text))
            }
          )
          val replyMessages = builder.add(
            Flow[ChatMessage].collect {
              case reply =>
                TextMessage(reply.toJson.prettyPrint)
            }
          )

          clientSourceMat ~> merge
          inputMessage ~> msgInterpreter ~> merge ~> msgServer ~> disconnectSink(userId)
          messageSourceActor ~> replyMessages
          FlowShape(inputMessage.in, replyMessages.out)
        }
    })
}


object ConnectionHandler {
  def apply(messageSource: Source[ChatMessage, SourceQueueWithComplete[ChatMessage]],
            idGen: IdGen[User],
            msgServer: Flow[Reply, ChatMessage, NotUsed],
            msgInterpreter: Flow[ChatMessage, Reply, NotUsed],
            disconnectSink: Id[User] => Sink[ChatMessage, NotUsed]): ConnectionHandler =
    new ConnectionHandler(messageSource, idGen, msgServer, msgInterpreter, disconnectSink)
}