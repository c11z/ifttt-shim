package com.c11z.ifttt.shim.api

import akka.actor.SupervisorStrategy.Stop
import akka.actor._
import com.c11z.ifttt.shim.api.PerRequest.WithProps
import com.c11z.ifttt.shim.domain._
import org.json4s.{DefaultFormats, _}
import spray.http.StatusCodes._
import spray.http.{HttpHeader, StatusCode}
import spray.httpx.Json4sSupport
import spray.routing.RequestContext

import scala.concurrent.duration._


trait PerRequest extends Actor with Json4sSupport {
  def r: RequestContext
  def target: ActorRef
  def message: RequestMessage

  import context._

  val json4sFormats = DefaultFormats

  setReceiveTimeout(2.seconds)

  target ! message

  def receive = {
    case Http200Empty() => complete(OK)
    case Http401(message) => complete(Unauthorized, convertToJson("errors" -> List("message" -> message)))
    case Http200(content) => complete(OK, convertToJson(content))
  }

  def convertToJson(a: Any) = Extraction.decompose(a)(json4sFormats)

  def complete[T <: AnyRef](status: StatusCode, obj: T = Nil, headers: List[HttpHeader] = List()) = {
    r.withHttpResponseHeadersMapped(oldheaders => oldheaders:::headers).complete(status, obj)
    stop(self)
  }

  override val supervisorStrategy =
    OneForOneStrategy() {
      case e => {
        complete(InternalServerError, e.getMessage)
        Stop
      }
    }
}

object PerRequest {
  case class WithProps(r: RequestContext, props: Props, message: RequestMessage) extends PerRequest {
    lazy val target = context.actorOf(props)
  }
}

trait PerRequestCreator {
  def perRequest(actorRefFactory: ActorRefFactory, r: RequestContext, props: Props, message: RequestMessage) =
    actorRefFactory.actorOf(Props(new WithProps(r, props, message)))
}
