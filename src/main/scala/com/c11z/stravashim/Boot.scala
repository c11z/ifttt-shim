package com.c11z.stravashim

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import com.c11z.stravashim.api.StravaShimActor
import spray.can.Http

import scala.concurrent.duration._
import scala.util.Properties

object Boot extends App {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("strava-shim-system")

  // create and start our service actor
  val service = system.actorOf(Props[StravaShimActor], "strava-shim-service")

  // !!IMPORTANT Allowing port to be sourced from Properties is what makes Heroku work!!
  val port = Properties.envOrElse("PORT", "8080").toInt
  implicit val timeout = Timeout(5.seconds)

  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ? Http.Bind(service, interface = "0.0.0.0", port = port)
}