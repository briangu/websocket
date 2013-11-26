package com.ops5.moodora

import com.twitter.finagle.HttpWebSocket
import com.twitter.concurrent.Broker
import com.twitter.util.{Promise, Await}

object Client {
  def main(args: Array[String]) {
    val outgoing = new Broker[String]

    val wait = new Promise[Unit]()

    HttpWebSocket.open(outgoing.recv, "ws://localhost:8080/") onSuccess { resp =>
      outgoing ! "hello, world"

      resp.onClose foreach { x =>
        println("closing")
        wait.setValue(Unit)
      }

      var count = 0

      resp.messages foreach { message =>
        println(message)
        count += 1
        if (count == 5) {
          outgoing ! "I'm half way!"
        } else if (count == 10) {
          outgoing ! "I'm done!"
        } else if (message == "I'm done!".reverse) {
          resp.close()
        }
      }
    }

    Await.result(wait)
  }
}
