package com.c11z.ifttt.shim.api

import akka.actor.{Actor, ActorLogging, Props}
import com.c11z.ifttt.shim.channel.strava.StravaActor
import com.c11z.ifttt.shim.domain._
import spray.http.MediaTypes.{`application/json`, `text/html`}
import spray.httpx.encoding.Gzip
import spray.routing._

import scala.concurrent.ExecutionContextExecutor

/**
 * Main Actor for routes. To extend:
 *        1. Create new channelRoute in ShimService
 *        2. Add it to the runRoute
 *        3. Create new {channel}PerHandler in ShimService and reference {Channel}Actor
 *        4. Profit
 */
class ShimActor extends Actor with ActorLogging with ShimService {
  def actorRefFactory = context
  def receive = runRoute(shimRoute ~ stravaRoute)
}

trait ShimService extends HttpService with PerRequestCreator {
  implicit def executionContext: ExecutionContextExecutor = actorRefFactory.dispatcher

  val shimRoute = {
    path("") {
      respondWithMediaType(`text/html`) {
        get {
          complete(
            """<http>
            | <h1>Welcome to IFTTT-Shim</h1>
            |</http>
          """.stripMargin)
        }
      }
    }
  }

  /**
   * Route for Strava Shim
   */
  val stravaRoute = {
    (pathPrefix("strava" / "ifttt" / "v1") & respondWithMediaType(`application/json`) & encodeResponse(Gzip)) {
      headerValueByName("IFTTT-Channel-Key") { channelKey =>
        (path("status") & get) {
          stravaPerRequest {
            GetStatus(channelKey)
          }
        } ~ (path("test" / "setup") & post) {
          stravaPerRequest {
            PostTestSetup(channelKey)
          }
        }
      } ~ headerValueByName("Authorization") { token =>
        (path("user" / "info") & get) {
          stravaPerRequest {
            GetUserInfo(token)
          }
        } ~ entity(as[String]) { trigger =>
          pathPrefix("triggers") {
            (path("new_personal_record") & post) {
              stravaPerRequest {
                NewPersonalRecord(token, trigger)
              }
            } ~ (path("new_activity") & post) {
              stravaPerRequest {
                NewActivity(token, trigger)
              }
            }
          }
        }
      }
    }
  }

  def stravaPerRequest(message: RequestMessage): Route =
    ctx => perRequest(actorRefFactory, ctx, Props[StravaActor], message)

  /*** Put new Channel HttpService trait HERE! ***/
  // {channel}Route = ???
  // def {channel}PerRequest(message: RequestMessage): Route = ???
}
