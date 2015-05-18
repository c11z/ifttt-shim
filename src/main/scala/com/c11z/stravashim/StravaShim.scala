package com.c11z.stravashim

import akka.actor.{Actor, ActorLogging}
import spray.http.MediaTypes.`application/json`
import spray.http.StatusCodes._
import spray.routing._

import scala.concurrent.ExecutionContextExecutor

class StravaShimActor extends Actor with StravaShimService with ActorLogging {
  def actorRefFactory = context
  def receive = runRoute(shimRoute)
}

trait StravaShimService extends HttpService {
  implicit def executionContext: ExecutionContextExecutor = actorRefFactory.dispatcher
  val CHANNEL_KEY = "wq3U2-THKBYt08VBJOmT_B304rhQD6MND5friCCH_KYJIvhmKGvC-MH72weSPCQ3"
  val TEST_TOKEN = "e2f0782b68800e7d7a97e59e22493c55fb518152"

  val shimRoute = {
    (pathPrefix("ifttt" / "v1") &
      respondWithMediaType(`application/json`) &
      headerValueByName("IFTTT-Channel-Key")) { channelKey =>
      (path("status")  & get) {
        if(channelKey == CHANNEL_KEY) complete(OK)
        else complete(Unauthorized)
      } ~ (path("test" / "setup") & put) {
        if (channelKey == CHANNEL_KEY) {
          complete(s"""{"data": {"accessToken": "$TEST_TOKEN", "samples": {}}}""")
        } else {
          complete(Unauthorized)
        }
      }

    }
  }
}
