package com.c11z.stravashim.core

import akka.actor.Actor
import com.c11z.stravashim.domain._
import com.typesafe.config.ConfigFactory
import dispatch.Defaults._
import dispatch._
import org.json4s.JsonAST.JValue
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by c11z on 5/20/15.
 */


trait StravaOptions {
  val conf = ConfigFactory.load()
  val secret = conf.getConfig("secret")
  val test = conf.getConfig("test")

  def validateChannelKey(channelKey: String): Boolean = {
    if(channelKey == secret.getString("ifttt-channel-key")) true else false
  }

  def reportStatus(channelKey: String) = {
    validateChannelKey(channelKey) match {
      case true => Good()
      case false => Bad("Invalid IFTTT-Channel-Key")
    }
  }

  def returnTestSetup(channelKey: String) = {
    validateChannelKey(channelKey) match {
      case true => Json(parse(test.getString("setup-response")))
      case false => Bad("Invalid IFTTT-Channel-Key")
    }
  }

  def userFromAthlete(token: String) = {
    val res = StravaClient.getAthlete(token)
    Await.result(res, 2.seconds) match {
      case Some(json) =>
        val JString(first) = json \ "firstname"
        val JString(last) = json \ "lastname"
        val JInt(id) = json \ "id"
        Json(parse(s"""{"data":{"name": "${first} ${last}", "id": "${id}"}}"""))
      case None => Bad("Unable to request Athlete from Strava")
    }
  }
}


/**
 * Actor to provide operations with the Strava API
 */
class StravaActor extends Actor with StravaOptions {
  override def receive: Receive = {
    case GetStatus(channelKey) => sender ! reportStatus(channelKey)
    case PostTestSetup(channelKey) => sender ! returnTestSetup(channelKey)
    case GetUserInfo(token) => sender ! userFromAthlete(token)
  }
}


object StravaClient {
  val stravaHttp = new Http
  val stravaHost = host("www.strava.com/api/v3/").secure

  val asJson: Res => JValue = as.Response { res =>
    parse(res.getResponseBody)
  }

  def getAthlete(token: String) = {
    val req: Req = (stravaHost / "athlete").addHeader("Authorization", token).GET
    stravaHttp(req OK asJson).option
  }
}
