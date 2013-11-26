package com.ops5.moodora

import com.twitter.finagle.HttpWebSocket
import com.twitter.concurrent.Broker
import com.twitter.util.{Promise, Await, Future}

object Client {
  def main(args: Array[String]) {
    val out = new Broker[String]

    val wait = new Promise[Unit]()

    val client = HttpWebSocket.open(out.recv, "ws://localhost:8080/") onSuccess { resp =>
      out ! "hello, world"

      resp.onClose foreach { x =>
        println("closing")
        wait.setValue(Unit)
      }

      var count = 0

      resp.messages foreach { message =>
        println(message)
        count += 1
        if (count == 10) {
          resp.close()
        }
      }
    }

    Await.result(wait)
  }
}
