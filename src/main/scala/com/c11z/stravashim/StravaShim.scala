package com.c11z.stravashim

import akka.actor.{Actor, ActorLogging}
import spray.http.MediaTypes.`application/json`
import spray.http.StatusCodes._
import spray.routing._
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContextExecutor

class StravaShimActor extends Actor with StravaShimService with ActorLogging {
  def actorRefFactory = context
  def receive = runRoute(shimRoute)
}


trait StravaShimService extends HttpService {
  implicit def executionContext: ExecutionContextExecutor = actorRefFactory.dispatcher
  val conf = ConfigFactory.load()
  val secret = conf.getConfig("secret")
  val test = conf.getConfig("test")

  val shimRoute = {
    (pathPrefix("ifttt" / "v1") & respondWithMediaType(`application/json`)) {
      headerValueByName("IFTTT-Channel-Key") { channelKey =>
        (path("status") & get) {
          if (channelKey == secret.getString("ifttt-channel-key")) complete(OK)
          else complete(Unauthorized)
        } ~ (path("test" / "setup") & post) {
          if (channelKey == secret.getString("ifttt-channel-key")) {
            complete(test.getString("setup-response"))
          } else {
            complete(Unauthorized)
          }
        }
      }
    }
  }
}
