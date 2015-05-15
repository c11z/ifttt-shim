package com.c11z.stravashim

import akka.actor.{Actor, ActorSystem, Props, ActorLogging}
import spray.routing._

class StravaShimActor extends Actor with StravaShimService with ActorLogging {
  def actorRefFactory = context
  def receive = runRoute(shimRoute)
}

trait StravaShimService extends HttpService {
  implicit def executionContext = actorRefFactory.dispatcher

  val shimRoute = {
    path("") {
      get {
        complete("Strava shim for IFTTT")
      }
    } ~
    path("status") {
      get {
        complete("yup")
      }
    }
  }
}