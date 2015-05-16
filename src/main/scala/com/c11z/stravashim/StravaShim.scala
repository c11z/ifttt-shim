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

  val shimRoute = {
    (pathPrefix("ifttt" / "v1") & respondWithMediaType(`application/json`) ) {
      path("status") {
        get {
          complete(OK)
        }
      }
    }
  }
}