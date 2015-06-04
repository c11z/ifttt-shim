package com.c11z.ifttt.shim.channel.strava

import com.typesafe.config.ConfigFactory
import dispatch.Defaults._
import dispatch._
import org.json4s._
import org.json4s.native.JsonMethods._

/**
 * Http Client with convenience methods and setup for the Strava API
 */
object StravaClient {
  val stravaHttp = new Http
  val stravaHost = host("www.strava.com/api/v3/").secure

  val asJson: Res => JValue = as.Response { res =>
    parse(res.getResponseBody)
  }

  def getAthlete(token: String) = {
    val req: Req = (stravaHost / "athlete")
      .addHeader("Authorization", token)
      .GET
    stravaHttp(req OK asJson)
  }

  def getAthleteActivities(token: String, limit: String) = {
    val req = (stravaHost / "athlete" / "activities")
      .addHeader("Authorization", token)
      .addParameter("per_page", limit)
      .GET
    stravaHttp(req OK asJson)
  }

  def getActivity(token: String, id: BigInt) = {
    val req = (stravaHost / "activities" / id.toString())
      .addHeader("Authorization", token)
      .GET
    stravaHttp(req OK asJson)
  }
}
