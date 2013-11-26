package com.ops5.moodora

import com.twitter.concurrent.{Offer, Broker}
import com.twitter.finagle.{HttpWebSocket, Service}
import com.twitter.finagle.websocket.WebSocket
import com.twitter.util.{Await, Future}
import java.util.Date

case class ClientSocket(incoming: Offer[String], outgoing: Broker[String])

object ClientFanout {
  var clients: Set[ClientSocket] = Set()

  def register(client: ClientSocket) { clients += client }
  def unregister(client: ClientSocket) { clients -= client }

  def send(msg: String): Future[Unit] = {
    Future.collect(clients.toSeq map { c => c.outgoing ! msg }) flatMap { _ => Future.Done }
  }

  def run() {
    new Thread(new Runnable {
      def run() {
        while(true) {
          send(new Date().toString)
          Thread.sleep(1000)
        }
      }
    }).start()
  }
}

object Server {
  def main(args: Array[String]) {

    ClientFanout.run()

    val server = HttpWebSocket.serve(":8080", new Service[WebSocket, WebSocket] {
      def apply(req: WebSocket): Future[WebSocket] = {
        val outgoing = new Broker[String]
        val socket = req.copy(messages = outgoing.recv)
        req.messages foreach { itm =>
          println(itm)
          outgoing ! itm.reverse
        }
        val clientSocket = ClientSocket(socket.messages, outgoing)
        ClientFanout.register(clientSocket)
        socket.onClose foreach { x =>
          println("closing")
          ClientFanout.unregister(clientSocket)
        }
        Future.value(socket)
      }
    })

    Await.result(server)
  }
}

