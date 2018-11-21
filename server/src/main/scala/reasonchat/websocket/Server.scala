package reasonchat.websocket

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import akka.stream.Materializer

import scala.concurrent.Future

object Server {
  def start(port: Int, handler: ConnectionHandler)(implicit mat: Materializer, system: ActorSystem): Future[Http.ServerBinding] = {
    val routes = path("connect" / Segment) { nickname =>
      cors() {
        handleWebSocketMessages(handler.create(nickname))
      }
    }

    Http().bindAndHandle(routes, "localhost", port)
  }
}
