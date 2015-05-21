package com.c11z.stravashim.api

import akka.actor.{Actor, ActorLogging, Props}
import com.c11z.stravashim.core.StravaActor
import com.c11z.stravashim.domain.{GetStatus, GetUserInfo, PostTestSetup, RequestMessage}
import spray.http.MediaTypes.`application/json`
import spray.httpx.encoding.Gzip
import spray.routing._

import scala.concurrent.ExecutionContextExecutor


class ShimActor extends Actor with ShimService with ActorLogging {
  def actorRefFactory = context
  def receive = runRoute(shimRoute)
}

trait ShimService extends HttpService with PerRequestCreator {
  implicit def executionContext: ExecutionContextExecutor = actorRefFactory.dispatcher

  val shimRoute = {
    (pathPrefix("ifttt" / "v1") & respondWithMediaType(`application/json`) & encodeResponse(Gzip)) {
      headerValueByName("IFTTT-Channel-Key") { channelKey =>
        (path("status") & get) {
          handlePerRequest {
            GetStatus(channelKey)
          }
        } ~ (path("test" / "setup") & post) {
          handlePerRequest {
            PostTestSetup(channelKey)
          }
        }
      } ~ headerValueByName("Authorization") { token =>
        (path("user" / "info") & get) {
          handlePerRequest {
            GetUserInfo(token)
          }
        }
      }
    }
  }

  def handlePerRequest(message: RequestMessage): Route =
    ctx => perRequest(actorRefFactory, ctx, Props[StravaActor], message)
}
