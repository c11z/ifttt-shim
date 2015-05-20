package com.c11z.stravashim.core

import akka.actor.Actor
import com.c11z.stravashim.domain._
import com.typesafe.config.ConfigFactory

/**
 * Created by c11z on 5/20/15.
 */

trait StravaApiOptions {
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
      case true => Json(test.getString("setup-response"))
      case false => Bad("Invalid IFTTT-Channel-Key")
    }
  }
}


/**
 * Actor to provide operations with the Strava API
 */
class StravaActor extends Actor with StravaApiOptions {
  override def receive: Receive = {
    case GetStatus(channelKey) => sender ! reportStatus(channelKey)
    case PostTestSetup(channelKey) => sender ! returnTestSetup(channelKey)
  }
}
